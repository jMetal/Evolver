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
 * Runs NSGA-II with the tree (derivation tree) encoding as meta-optimizer to configure RVEA using
 * problem DTLZ3Minus as training set, through {@link TrainingRunner}.
 *
 * <p>DTLZ3Minus is DTLZ3 with its objectives negated and rescaled (Ishibuchi et al., IEEE TEVC
 * 2017): a multimodal problem (many local fronts, as DTLZ3) whose Pareto front is inverted, so it
 * does not cover the simplex RVEA spreads its reference vectors over. That makes the choice between RVEA, RVEA* and iRVEA (the {@code
 * replacement} parameter) part of what the meta-optimizer has to find (see the RVEA guide, {@code
 * docs/algorithms/rvea.rst}). As with MOEA/D, RVEA reads the reference vectors of each problem from
 * {@code weightVectorFilesDirectory} ({@code W3D_100.dat} here, for three objectives and a
 * population of 100).
 *
 * <p>DTLZ3Minus is not among the short names of {@code ProblemRegistry}, so it is given by its
 * fully-qualified class name, which the registry resolves by reflection.
 *
 * <p>The meta-optimizer works directly on derivation trees of {@code RVEADouble.yaml}, with typed
 * subtree crossover and point/subtree mutation, so it never sees the inactive parameters of the
 * flat encoding (for example, the iRVEA parameters when the replacement is RVEA).
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingRVEAForProblemDTLZ3Minus {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: RVEA
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: RVEADouble.yaml
      extraConfig: {weightVectorFilesDirectory: resources/weightVectors}
      trainingProblemNames: [org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3Minus]
      trainingReferenceFrontFileNames: [resources/referenceFronts/DTLZ3Minus.3D.csv]
      trainingEvaluations: [25000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: NSGA-II
      encoding: tree
      metaMaxComputingTimeMinutes: 5
      metaPopulationSize: 50
      numberOfCores: 12
      crossoverProbability: 0.9
      mutationProbability: 1.0
      mutationDistributionIndex: 5.0
      selection: tournament
      selectionTournamentSize: 2
      """;

  private static final String OUTPUT_DIRECTORY = "results/tree-nsgaii/rvea/DTLZ3Minus";
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
