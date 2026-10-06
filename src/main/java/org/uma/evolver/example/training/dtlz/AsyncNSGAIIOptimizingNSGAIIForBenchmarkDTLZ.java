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
 * Runs an asynchronous multi-threaded NSGA-II as meta-optimizer to configure NSGA-II using the
 * DTLZ1-DTLZ7 (three-objective) problems as training set, through {@link TrainingRunner}. It is
 * the training run of tutorial E6 ({@code docs/tutorials/training_sets_indicators_budgets.rst}):
 * NHV and EP as meta-objectives, and 10000 evaluations per problem, a fifth of the validation
 * budget (50000). The live plot of the meta-optimizer front
 * ({@code FRONT_PLOT_FREQUENCY}) is on.
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
 * <p>{@code BASE_LEVEL_YAML}/{@code META_SEARCH_YAML} are the same recipe already bundled as
 * standalone files under {@code src/main/resources/baseLevelConfigurations/
 * DTLZ3DNSGAIIBaseLevel.yaml} and {@code src/main/resources/metaOptimizerConfigurations/
 * MetaAsyncNSGAIIFlatConfiguration.yaml}, except for {@code numberOfCores} (16 here, 8 in the
 * bundled file, which suits smaller machines) — this class keeps its own inline copy so the whole
 * example reads top-to-bottom from a single file, and so the recipe can be tweaked here without
 * touching the packaged resources. To run this exact experiment from a terminal instead, without
 * building or touching Java at all, use the ready-made {@code request.yaml} that references those
 * two files ({@code mvn clean package} produces {@code
 * target/Evolver-<version>-jar-with-dependencies.jar}):
 *
 * <pre>{@code
 * java -cp target/Evolver-<version>-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.training.TrainingRunnerMain \
 *     src/main/resources/cli/training/async-nsgaii-dtlz3d-request.yaml
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
public class AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ {

  // [step-1-start]
  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: NSGA-II
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: NSGAIIDouble.yaml
      trainingProblemNames: [DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ5, DTLZ6, DTLZ7]
      trainingReferenceFrontFileNames:
        - resources/referenceFronts/DTLZ1.3D.csv
        - resources/referenceFronts/DTLZ2.3D.csv
        - resources/referenceFronts/DTLZ3.3D.csv
        - resources/referenceFronts/DTLZ4.3D.csv
        - resources/referenceFronts/DTLZ5.3D.csv
        - resources/referenceFronts/DTLZ6.3D.csv
        - resources/referenceFronts/DTLZ7.3D.csv
      trainingEvaluations: [10000, 10000, 10000, 10000, 10000, 10000, 10000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: AsyncNSGA-II
      encoding: flat
      metaMaxEvaluations: 2000
      metaPopulationSize: 50
      numberOfCores: 16
      crossover: SBX
      mutation: polynomial
      crossoverProbability: 0.9
      crossoverRepairStrategy: bounds
      sbxDistributionIndex: 20.0
      mutationProbabilityFactor: 1.0
      mutationRepairStrategy: bounds
      polynomialMutationDistributionIndex: 20.0
      """;
  // [step-1-end]

  private static final String OUTPUT_DIRECTORY = "results/tutorial-training-sets/training";
  private static final int WRITE_FREQUENCY = 100;
  private static final int STATUS_FREQUENCY = 500;
  private static final int FRONT_PLOT_FREQUENCY = 100;
  // Also writes the whole population of the meta-optimizer at each checkpoint, not only its
  // non-dominated configurations (POPULATION_INDICATORS.csv, POPULATION_CONFIGURATIONS.csv)
  private static final boolean WRITE_POPULATION = true;

  public static void main(String[] args) throws IOException {
    // [step-2-start]
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.loadFromYaml(BASE_LEVEL_YAML);
    MetaSearchConfig metaSearch = MetaOptimizerConfigurationReader.loadFromYaml(META_SEARCH_YAML);

    TrainingRequest request =
        new TrainingRequest(
            baseLevel,
            metaSearch,
            OUTPUT_DIRECTORY,
            WRITE_FREQUENCY,
            STATUS_FREQUENCY,
            FRONT_PLOT_FREQUENCY,
            WRITE_POPULATION);

    new TrainingRunner().run(request, Path.of(OUTPUT_DIRECTORY, "status.yaml"));
    // [step-2-end]

    // AsyncNSGA-II leaves its worker threads running (see TrainingRunnerMain), so the JVM does not
    // end by itself. That keeps the live plot open to show the final population: closing its
    // window ends the JVM (the plot's Swing window exits on close).
    System.out.println("Training finished: close the plot window to exit.");
  }
}
