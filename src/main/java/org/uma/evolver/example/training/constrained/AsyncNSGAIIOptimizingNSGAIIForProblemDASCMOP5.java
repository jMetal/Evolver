package org.uma.evolver.example.training.constrained;

import java.io.IOException;
import java.nio.file.Path;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;

/**
 * Runs the asynchronous NSGA-II as meta-optimizer to configure NSGA-II for DASCMOP5, a problem with
 * constraints, through {@link TrainingRunner}.
 *
 * <p>The first reference example of tuning on a problem with constraints. The base level uses
 * {@code NSGAIIDoubleConstrained.yaml}, the parameter space of NSGA-II plus its {@code
 * constraintHandling} parameter, so the constraint-handling technique is tuned with the rest of the
 * configuration. With its default configuration ({@code cdp}) and 20000 evaluations, NSGA-II finds
 * feasible solutions in about 6 of 10 runs on DASCMOP5, and their ICMOP ranges from -0.41 to 0.09:
 * there is room both to fail less often and to improve the quality.
 *
 * <p>The objectives of the meta-optimizer are ICMOP ({@code ConstrainedMultiobjectiveIndicator},
 * the indicator of Vodopija, Tušar and Filipič) and EP, as recommended in {@code
 * docs/concepts/evaluation.rst}. Since the training problem has constraints, each objective is
 * {@code 6k + 2v + Q} ({@code ConstrainedIndicatorValue}): {@code k} runs without feasible
 * solutions, their mean scaled violation {@code v}, and the indicator on the feasible solutions,
 * capped at its threshold (1 for both). With a single run per configuration, a configuration that
 * finds feasible solutions has both objectives at most 1, and one that does not has them above 6.
 *
 * <p>The live plot of the meta-optimizer's front is on, to watch the search: the first
 * configurations that fail appear above 6, and the front then moves below 1, where the
 * configurations are compared by their quality.
 *
 * <p>The whole population of the meta-optimizer is written too ({@code writePopulation}), since
 * the non-dominated configurations hide those without feasible solutions as soon as one has them.
 * In the asynchronous NSGA-II every evaluated configuration enters the population while it has
 * fewer than 50, so the checkpoint at evaluation 50 shows how many configurations of the random
 * initial population fail; later, a configuration that fails competes to enter and is discarded at
 * once, so the checkpoints show when the whole population becomes feasible, not every failure.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForProblemDASCMOP5 {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: NSGA-II
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: NSGAIIDoubleConstrained.yaml
      trainingProblemNames: [DASCMOP5]
      trainingReferenceFrontFileNames: [resources/referenceFronts/DASCMOP5.csv]
      trainingEvaluations: [20000]
      indicatorNames: [ConstrainedMultiobjectiveIndicator, Epsilon]
      """;

  private static final String META_SEARCH_YAML =
      """
      algorithm: AsyncNSGA-II
      encoding: flat
      metaMaxEvaluations: 2000
      metaPopulationSize: 50
      numberOfCores: 14
      crossover: SBX
      mutation: polynomial
      crossoverProbability: 0.9
      crossoverRepairStrategy: bounds
      sbxDistributionIndex: 20.0
      mutationProbabilityFactor: 1.0
      mutationRepairStrategy: bounds
      polynomialMutationDistributionIndex: 20.0
      """;

  private static final String OUTPUT_DIRECTORY = "results/asyncnsgaii/DASCMOP5";
  // A checkpoint every 10 evaluations: the one at 50 has the whole random initial population
  private static final int WRITE_FREQUENCY = 10;
  private static final int STATUS_FREQUENCY = 50;
  // Live plot of the meta-optimizer's front, to watch the search
  private static final int FRONT_PLOT_FREQUENCY = 10;
  // The whole population is also written at each checkpoint (POPULATION_INDICATORS.csv and
  // POPULATION_CONFIGURATIONS.csv), not only its non-dominated configurations, so that the
  // configurations without feasible solutions can be followed
  private static final boolean WRITE_POPULATION = true;

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
            FRONT_PLOT_FREQUENCY,
            WRITE_POPULATION);

    new TrainingRunner().run(request, Path.of(OUTPUT_DIRECTORY, "status.yaml"));

    System.exit(0);
  }
}
