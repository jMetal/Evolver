package org.uma.evolver.example.training.zdt;

import java.io.IOException;
import java.nio.file.Path;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;

/**
 * Runs NSGA-II as meta-optimizer to configure MOEA/D using problem ZDT4 as training set, through
 * {@link TrainingRunner}.
 *
 * <p>One of the minimal set of reference examples in {@code example.training}: this one exists to
 * show a base-level algorithm other than NSGA-II (MOEA/D, with its own {@code extraConfig} key,
 * {@code weightVectorFilesDirectory}) being tuned — every other reference example in this package
 * tunes NSGA-II itself.
 *
 * <p>{@code BASE_LEVEL_YAML}/{@code META_SEARCH_YAML} are exactly the same recipe already bundled
 * as standalone files under {@code src/main/resources/baseLevelConfigurations/
 * MoeadZdt4BaseLevel.yaml} and {@code src/main/resources/metaOptimizerConfigurations/
 * MetaNSGAIIFlatConfiguration.yaml} — this class keeps its own inline copy so the whole example
 * reads top-to-bottom from a single file, and so the recipe can be tweaked here without touching
 * the packaged resources. To run this exact experiment from a terminal instead, without building
 * or touching Java at all, use the ready-made {@code request.yaml} that references those two
 * files ({@code mvn clean package} produces {@code
 * target/Evolver-<version>-jar-with-dependencies.jar}):
 *
 * <pre>{@code
 * java -cp target/Evolver-<version>-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.training.TrainingRunnerMain \
 *     src/main/resources/cli/training/moead-zdt4-request.yaml
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
public class NSGAIIOptimizingMOEADForProblemZDT4 {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: MOEAD
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: MOEADDouble.yaml
      extraConfig: {weightVectorFilesDirectory: resources/weightVectors}
      trainingProblemNames: [ZDT4]
      trainingReferenceFrontFileNames: [resources/referenceFronts/ZDT4.csv]
      trainingEvaluations: [10000]
      indicatorNames: [Epsilon, NormalizedHypervolume]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: NSGA-II
      encoding: flat
      metaMaxEvaluations: 2000
      metaPopulationSize: 50
      offspringPopulationSize: 50
      numberOfCores: 8
      crossover: SBX
      mutation: polynomial
      crossoverProbability: 0.9
      crossoverRepairStrategy: bounds
      sbxDistributionIndex: 20.0
      mutationProbabilityFactor: 1.0
      mutationRepairStrategy: bounds
      polynomialMutationDistributionIndex: 20.0
      selection: tournament
      selectionTournamentSize: 2
      """;

  private static final String OUTPUT_DIRECTORY = "results/moead/ZDT4";
  private static final int WRITE_FREQUENCY = 1;
  private static final int STATUS_FREQUENCY = 50;
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
