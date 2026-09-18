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
 * Runs SPEA2 as meta-optimizer to configure NSGA-II using problem DTLZ3 (three-objective) as
 * training set, through {@link TrainingRunner}.
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
 * DTLZ3NSGAIIBaseLevel.yaml} and {@code src/main/resources/metaOptimizerConfigurations/
 * MetaSPEA2FlatConfiguration.yaml} — this class keeps its own inline copy so the whole example
 * reads top-to-bottom from a single file, and so the recipe can be tweaked here without touching
 * the packaged resources. To run this exact experiment from a terminal instead, without building
 * or touching Java at all, use the ready-made {@code request.yaml} that references those two
 * files ({@code mvn clean package} produces {@code
 * target/Evolver-<version>-jar-with-dependencies.jar}):
 *
 * <pre>{@code
 * java -cp target/Evolver-<version>-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.training.TrainingRunnerMain \
 *     src/main/resources/cli/training/spea2-dtlz3-request.yaml
 * }</pre>
 *
 * <p>That same {@code request.yaml} pattern works for any other combination: {@code baseLevel}/
 * {@code metaSearch} are names resolved against the reusable recipes bundled under
 * {@code src/main/resources/{baseLevelConfigurations, metaOptimizerConfigurations}/} (see
 * {@link BaseLevelConfigurationReader}/{@link MetaOptimizerConfigurationReader} for the exact
 * lookup order), or absolute paths to standalone files of your own.
 *
 * <p>SPEA2 hardcodes its own operators (SBX crossover, polynomial mutation, strength ranking,
 * KNN density estimator, tournament selection) — unlike the NSGA-II-based examples, {@code
 * META_SEARCH_YAML} below has no crossover/mutation flags to set, only population size,
 * evaluations and cores.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SPEA2OptimizingNSGAIIForProblemDTLZ3 {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: NSGA-II
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: NSGAIIDouble.yaml
      trainingProblemNames: [DTLZ3]
      trainingReferenceFrontFileNames: [resources/referenceFronts/DTLZ3.3D.csv]
      trainingEvaluations: [15000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: SPEA2
      encoding: flat
      metaMaxEvaluations: 2000
      metaPopulationSize: 100
      numberOfCores: 8
      """;

  private static final String OUTPUT_DIRECTORY = "results/spea2/nsgaii/DTLZ3";
  private static final int WRITE_FREQUENCY = 1;
  private static final int STATUS_FREQUENCY = 100;
  // Live Pareto front plot, as the original example had.
  private static final int FRONT_PLOT_FREQUENCY = 1;

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
