package org.uma.evolver.algorithm.meta;

import org.uma.evolver.algorithm.base.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builder for creating SPEA2 instances configured for meta-optimization tasks with double
 * solutions. This builder provides sensible defaults for meta-optimization scenarios and internally
 * manages SBX crossover and polynomial mutation operators.
 *
 * <p>The builder follows the builder pattern for easy configuration of SPEA2 parameters
 * and creates a fully configured SPEA2 instance ready for execution.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * MetaSPEA2Builder builder = new MetaSPEA2Builder(problem)
 *     .setPopulationSize(100)
 *     .setMaxEvaluations(10000)
 *     .setNumberOfCores(4);
 * EvolutionaryAlgorithm<DoubleSolution> spea2 = builder.build();
 * }</pre>
 * </p>
 *
 * @see EvolutionaryAlgorithm
 * @see DoubleRDEMOEA
 * @see MetaOptimizationProblem
 */
public class MetaSPEA2Builder {
  /** The problem to be solved */
  private final Problem<DoubleSolution> problem;

  /** The population size (default: 50) */
  private int populationSize = 50;

  /** The offspring population size (default: 50) */
  private int offspringPopulationSize = 50;

  /** The maximum number of evaluations (default: 2000) */
  private int maxEvaluations = 2000;

  /** The number of cores to use for parallel evaluation (default: available processors) */
  private int numberOfCores = Runtime.getRuntime().availableProcessors();

  /** The mutation probability factor (default: 1.0) */
  private double mutationProbabilityFactor = 1.0;

  /** The parameter space */
  private ParameterSpace parameterSpace;

  /**
   * Creates a new builder with the specified meta-optimization problem.
   *
   * @param problem the meta-optimization problem to be solved (must not be null)
   * @throws JMetalException if the problem is null
   */
  public MetaSPEA2Builder(MetaOptimizationProblem<?> problem) {
    Check.notNull(problem);
    this.problem = problem;
    this.parameterSpace = new YAMLParameterSpace("RDEMOEADouble.yaml", new DoubleParameterFactory());
  }

  /**
   * Sets the population size for the SPEA2 algorithm.
   *
   * @param populationSize the population size (must be non-negative)
   * @return this builder instance for method chaining
   * @throws JMetalException if populationSize is negative
   */
  public MetaSPEA2Builder setPopulationSize(int populationSize) {
    Check.valueIsNotNegative(populationSize);
    this.populationSize = populationSize;
    return this;
  }

  /**
   * Sets the offspring population size for the SPEA2 algorithm.
   *
   * @param offspringPopulationSize the offspring population size (must be non-negative)
   * @return this builder instance for method chaining
   * @throws JMetalException if offspringPopulationSize is negative
   */
  public MetaSPEA2Builder setOffspringPopulationSize(int offspringPopulationSize) {
    Check.valueIsNotNegative(offspringPopulationSize);
    this.offspringPopulationSize = offspringPopulationSize;
    return this;
  }

  /**
   * Sets the maximum number of evaluations for the SPEA2 algorithm.
   *
   * @param maxEvaluations the maximum number of evaluations (must be non-negative)
   * @return this builder instance for method chaining
   * @throws JMetalException if maxEvaluations is negative
   */
  public MetaSPEA2Builder setMaxEvaluations(int maxEvaluations) {
    Check.valueIsNotNegative(maxEvaluations);
    this.maxEvaluations = maxEvaluations;
    return this;
  }

  /**
   * Sets the number of cores to use for parallel evaluation.
   *
   * @param numberOfCores the number of CPU cores to use (must be non-negative)
   * @return this builder instance for method chaining
   * @throws JMetalException if numberOfCores is negative
   */
  public MetaSPEA2Builder setNumberOfCores(int numberOfCores) {
    Check.valueIsNotNegative(numberOfCores);
    this.numberOfCores = numberOfCores;
    return this;
  }

  /**
   * Sets the mutation probability factor for the SPEA2 algorithm.
   *
   * @param mutationProbabilityFactor the mutation probability factor (must be non-negative)
   * @return this builder instance for method chaining
   * @throws JMetalException if mutationProbabilityFactor is negative
   */
  public MetaSPEA2Builder setMutationProbabilityFactor(double mutationProbabilityFactor) {
    Check.valueIsNotNegative(mutationProbabilityFactor);
    this.mutationProbabilityFactor = mutationProbabilityFactor;
    return this;
  }

  /**
   * Builds and configures a SPEA2 instance with the specified parameters.
   *
   * <p>The returned SPEA2 instance is configured with the following default operators:
   * <ul>
   *   <li>SBX Crossover (probability=0.9, distribution index=20.0)</li>
   *   <li>Polynomial Mutation (probability=1.0, distribution index=20.0)</li>
   *   <li>Strength Ranking for fitness assignment</li>
   *   <li>KNN density estimator (k=1)</li>
   *   <li>Tournament Selection (tournament size=2)</li>
   *   <li>Multi-threaded evaluation</li>
   * </ul>
   *
   * @return a fully configured SPEA2 instance ready for execution
   * @throws JMetalException if the configuration is invalid
   */
  public EvolutionaryAlgorithm<DoubleSolution> build() {
    // Create evaluation
    var evaluation = new MultiThreadedEvaluation<DoubleSolution>(numberOfCores, problem);

    String[] parameters =
            ("--algorithmResult population "
                    + "--createInitialSolutions default "
                    + "--variation crossoverAndMutationVariation "
                    + "--offspringPopulationSize " + offspringPopulationSize + " "
                    + "--crossover SBX "
                    + "--crossoverProbability 0.9 "
                    + "--crossoverRepairStrategy bounds "
                    + "--sbxDistributionIndex 20.0 "
                    + "--mutation polynomial "
                    + "--mutationProbabilityFactor " + mutationProbabilityFactor + " "
                    + "--mutationRepairStrategy bounds "
                    + "--polynomialMutationDistributionIndex 20.0 "
                    + "--ranking strengthRanking "
                    + "--densityEstimator knn "
                    + "--knnNeighborhoodSize 1 "
                    + "--knnNormalizeObjectives false "
                    + "--selection tournament "
                    + "--selectionTournamentSize 2 "
                    + "--replacement rankingAndDensityEstimator "
                    + "--removalPolicy sequential")
                    .split("\\s+");

    var metaSPEA2 =  new DoubleRDEMOEA(problem, populationSize, maxEvaluations, parameterSpace);
    metaSPEA2.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> spea2 = metaSPEA2.build();
    spea2.evaluation(evaluation);

    return spea2 ;
  }
}
