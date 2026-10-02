package org.uma.evolver.example.training.dtlz;

import java.io.IOException;
import java.nio.file.Path;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;

/**
 * Runs the asynchronous NSGA-II with the tree (derivation tree) encoding as meta-optimizer to
 * configure RVEA using DTLZ1Minus, DTLZ2Minus and DTLZ3Minus as training set, through {@link
 * TrainingRunner}.
 *
 * <p>The three problems are DTLZ1, DTLZ2 and DTLZ3 with their objectives negated and rescaled
 * (Ishibuchi et al., IEEE TEVC 2017): their Pareto fronts are inverted, so they do not cover the
 * simplex RVEA spreads its reference vectors over, which makes the choice between RVEA, RVEA* and
 * iRVEA (the {@code replacement} parameter) part of what the meta-optimizer has to find (see the
 * RVEA guide, {@code docs/algorithms/rvea.rst}). DTLZ1Minus and DTLZ3Minus are also multimodal. As
 * with MOEA/D, RVEA reads the reference vectors of each problem from {@code
 * weightVectorFilesDirectory} ({@code W3D_100.dat} here, for three objectives and a population of
 * 100).
 *
 * <p>The problems are not among the short names of {@code ProblemRegistry}, so they are given by
 * their fully-qualified class names, which the registry resolves by reflection.
 *
 * <p>The meta-optimizer works directly on derivation trees of {@code RVEADouble.yaml}, with typed
 * subtree crossover and point/subtree mutation. Being asynchronous, it does not wait for a whole
 * generation: as soon as a core finishes evaluating a configuration it creates the next one, which
 * keeps the cores busy when the configurations take very different times to evaluate. Its selection
 * (binary tournament) and replacement (ranking and crowding distance) are fixed by the algorithm, so
 * the configuration has no selection fields. The run stops after 20 minutes on 18 cores; the initial
 * population is always evaluated in full, and the evaluations in progress when the limit is reached
 * are discarded.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingRVEAForBenchmarkDTLZMinus {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: RVEA
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: RVEADouble.yaml
      extraConfig: {weightVectorFilesDirectory: resources/weightVectors}
      trainingProblemNames:
        - org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1Minus
        - org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus
        - org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3Minus
      trainingReferenceFrontFileNames:
        - resources/referenceFronts/DTLZ1Minus.3D.csv
        - resources/referenceFronts/DTLZ2Minus.3D.csv
        - resources/referenceFronts/DTLZ3Minus.3D.csv
      trainingEvaluations: [25000, 25000, 25000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: AsyncNSGA-II
      encoding: tree
      metaMaxComputingTimeMinutes: 20
      metaPopulationSize: 50
      numberOfCores: 18
      crossoverProbability: 0.9
      mutationProbability: 1.0
      mutationDistributionIndex: 5.0
      """;

  private static final String OUTPUT_DIRECTORY = "results/tree-asyncnsgaii/rvea/DTLZMinus";
  private static final int WRITE_FREQUENCY = 50;
  private static final int STATUS_FREQUENCY = 50;
  // Live plot of the meta-optimizer's front, updated every that many meta-evaluations.
  private static final int FRONT_PLOT_FREQUENCY = 50;

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.loadFromYaml(BASE_LEVEL_YAML);
    MetaSearchConfig metaSearch = MetaOptimizerConfigurationReader.loadFromYaml(META_SEARCH_YAML);

    TrainingRequest request =
        new TrainingRequest(
            baseLevel,
            metaSearch,
            OUTPUT_DIRECTORY,
            WRITE_FREQUENCY,
            STATUS_FREQUENCY,
            FRONT_PLOT_FREQUENCY);

    new TrainingRunner().run(request, Path.of(OUTPUT_DIRECTORY, "status.yaml"));

    System.exit(0);
  }
}
