package org.uma.evolver.meta.builder;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.MuPlusLambdaReplacement;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedGeneticAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Builder for creating asynchronous multi-threaded Genetic Algorithm instances for optimization
 * tasks with double solutions.
 *
 * <p>This builder provides a convenient way to configure and create instances of
 * {@link AsynchronousMultiThreadedGeneticAlgorithm} with sensible defaults for asynchronous
 * parallel execution. It follows the builder pattern for fluent configuration of algorithm
 * parameters.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * MetaAsyncGeneticAlgorithmBuilder builder = new MetaAsyncGeneticAlgorithmBuilder(problem)
 *     .setPopulationSize(100)
 *     .setMaxEvaluations(10000)
 *     .setNumberOfCores(4)
 *     .setCrossover(new SBXCrossover(1.0, 20.0))
 *     .setMutation(new PolynomialMutation(1.0, 20.0));
 * AsynchronousMultiThreadedGeneticAlgorithm<DoubleSolution> ga = builder.build();
 * }</pre>
 * </p>
 *
 * @see AsynchronousMultiThreadedGeneticAlgorithm
 */
public class MetaAsyncGeneticAlgorithmBuilder {
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

  /** The selection operator (default: Binary tournament) */
  private SelectionOperator<List<DoubleSolution>, DoubleSolution> selection;

  /** The replacement strategy (default: Mu+Lambda) */
  private Replacement<DoubleSolution> replacement;

  /** The mutation probability factor (default: 1.0) */
  private double mutationProbabilityFactor = 1.0;

  /** The objective index to optimize (default: 0) */
  private int objectiveIndex = 0;

  /**
   * Creates a new builder with the specified problem.
   *
   * <p>Initializes with default operators:
   * <ul>
   *   <li>Crossover: SBX with probability 1.0 and distribution index 20.0</li>
   *   <li>Mutation: Polynomial with probability 1.0/numberOfVariables and distribution index
   *       20.0</li>
   *   <li>Selection: Binary tournament selection</li>
   *   <li>Replacement: Mu+Lambda replacement</li>
   * </ul>
   *
   * @param problem the optimization problem to be solved (must not be null)
   * @throws NullPointerException if the problem is null
   */
  public MetaAsyncGeneticAlgorithmBuilder(Problem<DoubleSolution> problem) {
    Check.notNull(problem);
    this.problem = problem;
    this.crossover = new SBXCrossover(1.0, 20.0);
    this.mutation =
        new PolynomialMutation(mutationProbabilityFactor * 1.0 / problem.numberOfVariables(), 20.0);
    this.selection = new BinaryTournamentSelection<>(new ObjectiveComparator<>(objectiveIndex));
    this.replacement = new MuPlusLambdaReplacement<>(new ObjectiveComparator<>(objectiveIndex));
  }

  /**
   * Sets the population size for the asynchronous Genetic Algorithm.
   *
   * @param populationSize the population size (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if populationSize is negative
   */
  public MetaAsyncGeneticAlgorithmBuilder setPopulationSize(int populationSize) {
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
  public MetaAsyncGeneticAlgorithmBuilder setMaxEvaluations(int maxEvaluations) {
    Check.valueIsNotNegative(maxEvaluations);
    this.maxEvaluations = maxEvaluations;
    return this;
  }

  /**
   * Sets the mutation probability factor for the mutation operator.
   *
   * @param mutationProbabilityFactor the mutation probability factor (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if mutationProbabilityFactor is negative
   */
  public MetaAsyncGeneticAlgorithmBuilder setMutationProbabilityFactor(
      double mutationProbabilityFactor) {
    Check.valueIsNotNegative(mutationProbabilityFactor);
    this.mutationProbabilityFactor = mutationProbabilityFactor;
    this.mutation =
        new PolynomialMutation(
            mutationProbabilityFactor * 1.0 / problem.numberOfVariables(), 20.0);
    return this;
  }

  /**
   * Sets the number of CPU cores to use for parallel evaluation.
   *
   * @param numberOfCores the number of CPU cores (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if numberOfCores is negative
   */
  public MetaAsyncGeneticAlgorithmBuilder setNumberOfCores(int numberOfCores) {
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
  public MetaAsyncGeneticAlgorithmBuilder setCrossover(
      CrossoverOperator<DoubleSolution> crossover) {
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
  public MetaAsyncGeneticAlgorithmBuilder setMutation(MutationOperator<DoubleSolution> mutation) {
    Check.notNull(mutation);
    this.mutation = mutation;
    return this;
  }

  /**
   * Sets the selection operator to be used by the algorithm.
   *
   * @param selection the selection operator (must not be null)
   * @return this builder instance for method chaining
   * @throws NullPointerException if selection is null
   */
  public MetaAsyncGeneticAlgorithmBuilder setSelection(
      SelectionOperator<List<DoubleSolution>, DoubleSolution> selection) {
    Check.notNull(selection);
    this.selection = selection;
    return this;
  }

  /**
   * Sets the replacement strategy to be used by the algorithm.
   *
   * @param replacement the replacement strategy (must not be null)
   * @return this builder instance for method chaining
   * @throws NullPointerException if replacement is null
   */
  public MetaAsyncGeneticAlgorithmBuilder setReplacement(Replacement<DoubleSolution> replacement) {
    Check.notNull(replacement);
    this.replacement = replacement;
    return this;
  }

  /**
   * Sets the objective index to optimize (used for selection and replacement comparators).
   *
   * @param objectiveIndex the objective index (must be non-negative)
   * @return this builder instance for method chaining
   * @throws IllegalArgumentException if objectiveIndex is negative
   */
  public MetaAsyncGeneticAlgorithmBuilder setObjectiveIndex(int objectiveIndex) {
    Check.valueIsNotNegative(objectiveIndex);
    this.objectiveIndex = objectiveIndex;
    this.selection = new BinaryTournamentSelection<>(new ObjectiveComparator<>(objectiveIndex));
    this.replacement = new MuPlusLambdaReplacement<>(new ObjectiveComparator<>(objectiveIndex));
    return this;
  }

  /**
   * Builds and configures an asynchronous multi-threaded Genetic Algorithm instance.
   *
   * <p>The returned instance is ready for execution with the configured parameters. The algorithm
   * will use the specified number of cores for parallel evaluation and will terminate after
   * reaching the maximum number of evaluations.</p>
   *
   * @return a fully configured asynchronous Genetic Algorithm instance
   * @throws IllegalStateException if required parameters are not set properly
   */
  public AsynchronousMultiThreadedGeneticAlgorithm<DoubleSolution> build() {
    return new AsynchronousMultiThreadedGeneticAlgorithm<>(
        numberOfCores,
        problem,
        populationSize,
        crossover,
        mutation,
        selection,
        replacement,
        new TerminationByEvaluations(maxEvaluations));
  }
}
