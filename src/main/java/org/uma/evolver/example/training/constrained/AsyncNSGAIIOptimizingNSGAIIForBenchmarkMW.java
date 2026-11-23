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
 * Runs the asynchronous NSGA-II as meta-optimizer to configure NSGA-II using five two-objective
 * problems with constraints of the MW suite as training set, through {@link TrainingRunner}.
 *
 * <p>The training set has at least one problem of each type of the suite (the types differ in how
 * the constrained Pareto front relates to the unconstrained one): MW2 (type I), MW6 (II), MW10 and
 * MW13 (III), and MW11 (IV). The other MW problems are left to validate the configurations.
 *
 * <p>The base level uses {@code NSGAIIDoubleConstrainedFixedSize.yaml}: the parameter space of
 * NSGA-II with its {@code constraintHandling} parameter, so the constraint-handling technique is
 * tuned with the rest of the configuration, and with the population and the offspring fixed at 100,
 * which avoids the long runs of the configurations with few offspring per generation. Each
 * configuration is evaluated with one run of 10000 evaluations per problem.
 *
 * <p>The objectives of the meta-optimizer are ICMOP ({@code ConstrainedMultiobjectiveIndicator})
 * and EP. Since the training problems have constraints, each objective is {@code 6k + 2v + Q}, as
 * explained in {@link AsyncNSGAIIOptimizingNSGAIIForProblemDASCMOP5}.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForBenchmarkMW {

  private static final String BASE_LEVEL_YAML =
      """
      algorithmName: NSGA-II
      populationSize: 100
      numberOfIndependentRuns: 1
      yamlParameterSpaceFile: NSGAIIDoubleConstrainedFixedSize.yaml
      trainingProblemNames: [MW2, MW6, MW10, MW11, MW13]
      trainingReferenceFrontFileNames:
        - resources/referenceFronts/MW2.csv
        - resources/referenceFronts/MW6.csv
        - resources/referenceFronts/MW10.csv
        - resources/referenceFronts/MW11.csv
        - resources/referenceFronts/MW13.csv
      trainingEvaluations: [10000, 10000, 10000, 10000, 10000]
      indicatorNames: [ConstrainedMultiobjectiveIndicator, Epsilon]
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

  private static final String OUTPUT_DIRECTORY = "results/asyncnsgaii/MW";
  private static final int WRITE_FREQUENCY = 100;
  private static final int STATUS_FREQUENCY = 50;
  // Live plot of the meta-optimizer's front, to watch the search
  private static final int FRONT_PLOT_FREQUENCY = 100;
  // Also writes the whole population at each checkpoint, not only its non-dominated configurations
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
