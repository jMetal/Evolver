package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEAPermutationParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
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
 */
public class NSGAIIPermutationV2 extends AbstractNSGAIIV2<PermutationSolution<Integer>> {
  
  /**
   * Constructs an NSGAIIPermutation instance with the given population size and parameter space.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public NSGAIIPermutationV2(int populationSize, ParameterSpace parameterSpace) {
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
  public NSGAIIPermutationV2(
      Problem<PermutationSolution<Integer>> problem,
      int populationSize,
      int maximumNumberOfEvaluations, 
      ParameterSpace parameterSpace) {
    super(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
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
    return new NSGAIIPermutationV2(problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /** Sets non-configurable parameters that depend on the problem or algorithm configuration. */
  @Override
  protected void setNonConfigurableParameters() {
    // This method is intentionally left empty because the NSGAIIPermutation algorithm does not have
    // non-configurable parameters.
  }
}
