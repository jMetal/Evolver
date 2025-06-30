package org.uma.evolver.algorithm.base.rdsmoea;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEAPermutationParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Implementation of RDS-MOEA for permutation problems.
 *
 * @author Your Name
 */
public class RDEMOEAPermutation extends AbstractRDEMOEA<PermutationSolution<Integer>> {
  /**
   * Constructs an NSGAIIPermutation instance with the given population size and a default parameter
   * space.
   *
   * @param populationSize the population size to use
   */
  public RDEMOEAPermutation(int populationSize) {
    this(populationSize, new RDEMOEAPermutationParameterSpace());
  }

  /**
   * Constructs an NSGAIIPermutation instance with the given population size and parameter space.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public RDEMOEAPermutation(int populationSize, RDEMOEAPermutationParameterSpace parameterSpace) {
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
  public RDEMOEAPermutation(
          Problem<PermutationSolution<Integer>> problem,
          int populationSize,
          int maximumNumberOfEvaluations) {
    super(
            problem, populationSize, maximumNumberOfEvaluations, new RDEMOEAPermutationParameterSpace());
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
    return new RDEMOEAPermutation(problem, populationSize, maximumNumberOfEvaluations);
  }

  /** Sets non-configurable parameters that depend on the problem or algorithm configuration. */
  @Override
  protected void setNonConfigurableParameters() {
    // This method is intentionally left empty because the NSGAIIPermutation algorithm does not have
    // non-configurable parameters.
  }
}
