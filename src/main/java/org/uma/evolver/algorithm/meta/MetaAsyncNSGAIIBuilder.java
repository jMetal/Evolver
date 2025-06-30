package org.uma.evolver.algorithm.meta;

import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Builder for creating asynchronous multi-threaded NSGA-II instances for optimization tasks.
 *
 * <p>This builder provides a convenient way to configure and create instances of
 * {@link AsynchronousMultiThreadedNSGAII} with sensible defaults for asynchronous parallel
 * execution. It follows the builder pattern for fluent configuration of algorithm parameters.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * MetaAsyncNSGAIIBuilder builder = new MetaAsyncNSGAIIBuilder(problem)
 *     .setPopulationSize(100)
 *     .setMaxEvaluations(10000)
 *     .setNumberOfCores(4)
 *     .setCrossover(new SBXCrossover(1.0, 20.0))
 *     .setMutation(new PolynomialMutation(1.0, 20.0));
 * AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii = builder.build();
 * }</pre>
 * </p>
 *
 * @see AsynchronousMultiThreadedNSGAII
 * @see EvolutionaryAlgorithm
 */
public class MetaAsyncNSGAIIBuilder {
  /** The optimization problem to be solved */
  private final Problem<DoubleSolution> problem;
  
  /** The population size (default: 50) */
  private int populationSize = 50;
  
  /** The maximum number of evaluations (default: 2000) */
  private int maxEvaluations = 2000;
  
  /** The number of CPU cores to use (default: available processors) */
  private int numberOfCores = Runtime.getRuntime().availableProcessors();
  
  /** The crossover operator (default: SBX with distribution index 20.0) */
  private CrossoverOperator<DoubleSolution> crossover;
  
  /** The mutation operator (default: Polynomial with distribution index 20.0) */
  private MutationOperator<DoubleSolution> mutation;

  /**
   * Creates a new builder with the specified problem.
   *
   * <p>Initializes with default operators:
   * <ul>
   *   <li>Crossover: SBX with probability 1.0 and distribution index 20.0</li>
   *   <li>Mutation: Polynomial with probability 1.0 and distribution index 20.0</li>
   * </ul>
   *
   * @param problem the optimization problem to be solved (must not be null)
   * @throws NullPointerException if the problem is null
   */
  public MetaAsyncNSGAIIBuilder(Problem<DoubleSolution> problem) {
    Check.notNull(problem);
    this.problem = problem;
    this.crossover = new SBXCrossover(1.0, 20.0);
    this.mutation = new PolynomialMutation(1.0, 20.0);
  }

  /**
   * Sets the population size for the asynchronous NSGA-II algorithm.
   *
   * @param populationSize the population size (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if populationSize is negative
   */
  public MetaAsyncNSGAIIBuilder setPopulationSize(int populationSize) {
    Check.valueIsNotNegative(populationSize);
    this.populationSize = populationSize;
    return this;
  }

  /**
   * Sets the maximum number of evaluations for the algorithm.
   *
   * @param maxEvaluations the maximum number of evaluations (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if maxEvaluations is negative
   */
  public MetaAsyncNSGAIIBuilder setMaxEvaluations(int maxEvaluations) {
    Check.valueIsNotNegative(maxEvaluations);
    this.maxEvaluations = maxEvaluations;
    return this;
  }

  /**
   * Sets the number of CPU cores to use for parallel evaluation.
   *
   * @param numberOfCores the number of CPU cores (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if numberOfCores is negative
   */
  public MetaAsyncNSGAIIBuilder setNumberOfCores(int numberOfCores) {
    Check.valueIsNotNegative(numberOfCores);
    this.numberOfCores = numberOfCores;
    return this;
  }

  /**
   * Sets the crossover operator to be used by the algorithm.
   *
   * @param crossover the crossover operator (must not be null)
   * @return this builder instance for method chaining
   * @throws NullPointerException if crossover is null
   */
  public MetaAsyncNSGAIIBuilder setCrossover(CrossoverOperator<DoubleSolution> crossover) {
    Check.notNull(crossover);
    this.crossover = crossover;
    return this;
  }

  /**
   * Sets the mutation operator to be used by the algorithm.
   *
   * @param mutation the mutation operator (must not be null)
   * @return this builder instance for method chaining
   * @throws NullPointerException if mutation is null
   */
  public MetaAsyncNSGAIIBuilder setMutation(MutationOperator<DoubleSolution> mutation) {
    Check.notNull(mutation);
    this.mutation = mutation;
    return this;
  }

  /**
   * Builds and configures an asynchronous multi-threaded NSGA-II instance.
   *
   * <p>The returned instance is ready for execution with the configured parameters.
   * The algorithm will use the specified number of cores for parallel evaluation
   * and will terminate after reaching the maximum number of evaluations.</p>
   *
   * @return a fully configured asynchronous NSGA-II instance
   * @throws IllegalStateException if required parameters are not set properly
   */
  public AsynchronousMultiThreadedNSGAII<DoubleSolution> build() {
    return new AsynchronousMultiThreadedNSGAII<>(
        numberOfCores, 
        problem, 
        populationSize, 
        crossover, 
        mutation,
        new TerminationByEvaluations(maxEvaluations)
    );
  }
    
}
