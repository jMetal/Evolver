package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEAPermutationParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Configurable implementation of the NSGA-II algorithm for permutation-based problems.
 *
 * <p>This class provides a highly customizable version of NSGA-II, supporting:
 *
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)
 *   <li>Multiple crossover operators (e.g., PMX, CX, OX, etc.)
 *   <li>Different mutation approaches (e.g., swap, scramble, insertion, etc.)
 *   <li>Optional external archive integration
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * NSGAIIPermutation algorithm = new NSGAIIPermutation(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<PermutationSolution<Integer>> nsgaii = algorithm.build();
 * nsgaii.run();
 * }</pre>
 *
 * <p>Non-configurable parameters, such as the number of problem variables and, depending on the
 * mutation operator, other derived values, are set automatically based on the problem and algorithm
 * configuration.
 *
 * @see
 *     RDEMOEAPermutationParameterSpace
 * @see org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter
 */
public class NSGAIIPermutation extends AbstractNSGAII<PermutationSolution<Integer>> {
  /**
   * Constructs an NSGAIIPermutation instance with the given population size and a default parameter
   * space.
   *
   * @param populationSize the population size to use
   */
  public NSGAIIPermutation(int populationSize) {
    this(populationSize, new NSGAIIPermutationParameterSpace());
  }

  /**
   * Constructs an NSGAIIPermutation instance with the given population size and parameter space.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public NSGAIIPermutation(int populationSize, NSGAIIPermutationParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs an NSGAIIPermutation instance with the given problem, population size, and maximum
   * number of evaluations. Uses a default parameter space.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   */
  public NSGAIIPermutation(
      Problem<PermutationSolution<Integer>> problem,
      int populationSize,
      int maximumNumberOfEvaluations) {
    super(
        problem, populationSize, maximumNumberOfEvaluations, new NSGAIIPermutationParameterSpace());
  }

  /**
   * Creates a new instance of NSGAIIPermutation for the given problem and maximum number of
   * evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of NSGAIIPermutation
   */
  @Override
  public BaseLevelAlgorithm<PermutationSolution<Integer>> createInstance(
      Problem<PermutationSolution<Integer>> problem, int maximumNumberOfEvaluations) {
    return new NSGAIIPermutation(problem, populationSize, maximumNumberOfEvaluations);
  }

  /** Sets non-configurable parameters that depend on the problem or algorithm configuration. */
  @Override
  protected void setNonConfigurableParameters() {
    // This method is intentionally left empty because the NSGAIIPermutation algorithm does not have
    // non-configurable parameters.
  }
}
