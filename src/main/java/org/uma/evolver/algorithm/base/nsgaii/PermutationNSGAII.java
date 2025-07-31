package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * A configurable implementation of the Non-dominated Sorting Genetic Algorithm II (NSGA-II) 
 * specifically designed for permutation-based optimization problems.
 *
 * <p>This class extends the base {@link BaseNSGAII} implementation to handle permutation-encoded
 * solutions, providing specialized support for combinatorial optimization problems where solutions
 * are represented as permutations of integers.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Support for permutation-based solution spaces</li>
 *   <li>Configurable genetic operators through the parameter space</li>
 *   <li>Specialized support for permutation-specific operators</li>
 *   <li>Integration with JMetal's permutation solution interface</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Create a permutation problem instance (e.g., TSP)
 * PermutationProblem<Integer> problem = new TSP(...);
 * 
 * // Configure the algorithm
 * int populationSize = 100;
 * int maxEvaluations = 25000;
 * ParameterSpace parameterSpace = new ParameterSpace();
 * // Configure parameter space with desired operators and parameters
 * 
 * // Create and run the algorithm
 * PermutationNSGAII algorithm = new PermutationNSGAII(problem, populationSize, maxEvaluations, parameterSpace);
 * algorithm.run();
 * 
 * // Get results
 * List<PermutationSolution<Integer>> population = algorithm.result();
 * }</pre>
 *
 * <p>The algorithm is particularly well-suited for combinatorial optimization problems
 * such as the Traveling Salesman Problem (TSP), Quadratic Assignment Problem (QAP),
 * and other permutation-based optimization tasks.
 *
 * @see BaseNSGAII
 * @see PermutationSolution
 * @see org.uma.jmetal.problem.permutationproblem.PermutationProblem
 * @since version
 */
public class PermutationNSGAII extends BaseNSGAII<PermutationSolution<Integer>> {
  
  /**
   * Constructs a new instance of PermutationNSGAII with the specified population size and parameter space.
   * 
   * <p>Note: This creates a partially configured instance. The {@link #createInstance(Problem, int)} 
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population to be used in the algorithm. Must be positive.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive or parameterSpace is null
   */
  public PermutationNSGAII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a fully configured PermutationNSGAII instance ready for execution.
   *
   * @param problem the permutation-based optimization problem to be solved. Must implement the
   *               PermutationProblem interface.
   * @param populationSize the size of the population. Must be a positive integer.
   * @param maximumNumberOfEvaluations the evaluation budget for the algorithm. The algorithm will
   *                                 terminate once this number of evaluations is reached.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws ClassCastException if the provided problem does not implement PermutationProblem
   */
  public PermutationNSGAII(
      Problem<PermutationSolution<Integer>> problem,
      int populationSize,
      int maximumNumberOfEvaluations, 
      ParameterSpace parameterSpace) {
    super(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  /**
   * Creates and returns a new instance of PermutationNSGAII configured for the specified problem.
   * 
   * <p>This method implements the factory method pattern, allowing the creation of algorithm
   * instances with the same configuration but potentially different problems or evaluation limits.
   *
   * @param problem the permutation-based optimization problem to solve. Must not be null.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the new instance.
   *                                 Must be positive.
   * @return a new, fully configured instance of PermutationNSGAII
   * @throws IllegalArgumentException if problem is null or maximumNumberOfEvaluations is not positive
   */
  @Override
  public BaseLevelAlgorithm<PermutationSolution<Integer>> createInstance(
      Problem<PermutationSolution<Integer>> problem, int maximumNumberOfEvaluations) {
    return new PermutationNSGAII(problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Configures non-configurable parameters based on the problem's characteristics.
   * 
   * <p>This method is automatically called during algorithm initialization. For the permutation-based
   * NSGA-II implementation, no additional non-configurable parameters need to be set as all required
   * parameters are handled by the base class or through the parameter space configuration.
   * 
   * @implNote This method is intentionally empty as no additional parameter configuration is needed
   * for the permutation variant. It's maintained for consistency with the template method pattern
   * and potential future extensions.
   */
  @Override
  protected void setNonConfigurableParameters() {
    // No non-configurable parameters need to be set for the permutation variant
  }
}
