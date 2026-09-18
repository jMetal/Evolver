package org.uma.evolver.example.training;

import java.io.IOException;
import java.nio.file.Path;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;

/**
 * Runs NSGA-II with tree (derivation tree) encoding as meta-optimizer to configure NSGA-II using
 * the RE31-RE37 (RE3D) problems as training set, through {@link TrainingRunner}.
 *
 * <p>This example uses the derivation tree encoding instead of the flat [0,1]^n encoding. The
 * meta-optimizer operates directly on tree-structured solutions using typed subtree crossover and
 * point/subtree mutation.
 *
 * <p>Both halves of the configuration ({@code BASE_LEVEL_YAML}, {@code META_SEARCH_YAML}) are
 * kept as Java text blocks right here instead of separate files under {@code
 * src/main/resources/{baseLevelConfigurations,metaOptimizerConfigurations}/} — the same format
 * {@link BaseLevelConfigurationReader}/{@link MetaOptimizerConfigurationReader} already parse
 * from a named file, just given directly as text via {@code loadFromYaml(String)}. That keeps
 * this example self-contained and readable top-to-bottom (the point of {@code
 * org.uma.evolver.example.training}), while still reusing the exact same parsing/validation and
 * the full {@link TrainingRunner} pipeline (observers, status file, output files) instead of
 * hand-assembling them, as the older examples in this package do.
 *
 * <p>{@code BASE_LEVEL_YAML}/{@code META_SEARCH_YAML} are exactly the same recipe already bundled
 * as standalone files under {@code src/main/resources/baseLevelConfigurations/
 * Re3dNSGAIITreeBaseLevel.yaml} and {@code src/main/resources/metaOptimizerConfigurations/
 * MetaParallelNSGAIITreeConfiguration.yaml} — this class keeps its own inline copy so the whole
 * example reads top-to-bottom from a single file, and so the recipe can be tweaked here without
 * touching the packaged resources. To run this exact experiment from a terminal instead, without
 * building or touching Java at all, use the ready-made {@code request.yaml} that references those
 * two files ({@code mvn clean package} produces {@code
 * target/Evolver-<version>-jar-with-dependencies.jar}):
 *
 * <pre>{@code
 * java -cp target/Evolver-<version>-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.training.TrainingRunnerMain \
 *     src/main/resources/cli/training/tree-nsgaii-re3d-request.yaml
 * }</pre>
 *
 * <p>That same {@code request.yaml} pattern works for any other combination: {@code baseLevel}/
 * {@code metaSearch} are names resolved against the reusable recipes bundled under
 * {@code src/main/resources/{baseLevelConfigurations, metaOptimizerConfigurations}/} (see
 * {@link BaseLevelConfigurationReader}/{@link MetaOptimizerConfigurationReader} for the exact
 * lookup order), or absolute paths to standalone files of your own.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: NSGA-II
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: NSGAIIDouble.yaml
      trainingProblemNames: [RE31, RE32, RE33, RE34, RE35, RE36, RE37]
      trainingReferenceFrontFileNames:
        - resources/referenceFronts/RE31.csv
        - resources/referenceFronts/RE32.csv
        - resources/referenceFronts/RE33.csv
        - resources/referenceFronts/RE34.csv
        - resources/referenceFronts/RE35.csv
        - resources/referenceFronts/RE36.csv
        - resources/referenceFronts/RE37.csv
      trainingEvaluations: [10000, 10000, 10000, 10000, 10000, 10000, 10000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: NSGA-II
      encoding: tree
      metaMaxEvaluations: 2000
      metaPopulationSize: 50
      metaOffspringSize: 50
      numberOfCores: 8
      crossoverProbability: 0.9
      mutationProbability: 1.0
      mutationDistributionIndex: 20.0
      """;

  private static final String OUTPUT_DIRECTORY = "results/tree-nsgaii/RE3D";
  private static final int WRITE_FREQUENCY = 50;
  private static final int STATUS_FREQUENCY = 50;
  // Live Pareto front plot, as the original example had.
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
