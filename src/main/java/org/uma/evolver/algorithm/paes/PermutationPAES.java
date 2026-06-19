package org.uma.evolver.algorithm.paes;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Configurable PAES implementation for permutation-encoded problems.
 *
 * <p>Extends {@link BasePAES} with the wiring needed for {@link PermutationSolution} encodings.
 * Permutation mutation operators (swap, insert, scramble, inversion, simpleInversion, displacement)
 * use {@code mutationProbability} directly — no non-configurable parameter injection is needed.
 *
 * @see BasePAES
 */
public class PermutationPAES extends BasePAES<PermutationSolution<Integer>> {

  public PermutationPAES(int numberOfSolutionsToFind, ParameterSpace parameterSpace) {
    super(numberOfSolutionsToFind, parameterSpace);
  }

  public PermutationPAES(
      Problem<PermutationSolution<Integer>> problem,
      int numberOfSolutionsToFind,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, numberOfSolutionsToFind, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public BaseLevelAlgorithm<PermutationSolution<Integer>> createInstance(
      Problem<PermutationSolution<Integer>> problem, int maximumNumberOfEvaluations) {
    return new PermutationPAES(
        problem, numberOfSolutionsToFind, maximumNumberOfEvaluations,
        parameterSpace.createInstance());
  }

  @Override
  protected void setNonConfigurableParameters() {
    // Permutation mutation operators use mutationProbability directly; no injection needed.
  }
}
