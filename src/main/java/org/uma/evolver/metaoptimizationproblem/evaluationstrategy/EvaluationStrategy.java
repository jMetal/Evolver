package org.uma.evolver.metaoptimizationproblem.evaluationstrategy;

/**
 * Interface defining the contract for different evaluation strategies in meta-optimization
 * problems. Implementations of this interface determine how the number of evaluations is determined
 * for each problem during the optimization process.
 *
 * @author Antonio J. Nebro
 */
public interface EvaluationStrategy {
  /**
   * Retrieves the number of evaluations to be performed for a specific problem. The implementation
   * should handle the logic for determining the evaluation count, which could be fixed, random, or
   * based on some other criterion.
   *
   * @param problemIndex the zero-based index of the problem in the problem list
   * @return the number of evaluations to perform for the specified problem
   * @throws IndexOutOfBoundsException if the problemIndex is invalid
   */
  int getEvaluations(int problemIndex);

  /**
   * Validates the configuration of the evaluation strategy against the number of problems.
   * Implementations should throw an {@link IllegalArgumentException} if the configuration is
   * invalid for the given number of problems.
   *
   * @param numberOfProblems the total number of problems to be evaluated
   * @throws IllegalArgumentException if the strategy configuration is invalid for the given number
   *     of problems (e.g., insufficient evaluation counts provided)
   */
  void validate(int numberOfProblems);
}
