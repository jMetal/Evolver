package org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy;

import java.util.Random;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Implementation of EvaluationBudgetStrategy that generates random evaluation counts within a specified range.
 * Each call to getEvaluations() returns a random value between minEvaluations and maxEvaluations (inclusive).
 *
 * @author Antonio J. Nebro
 */
public class RandomRangeEvaluationsStrategy implements EvaluationBudgetStrategy {
  private final int minEvaluations;
  private final int maxEvaluations;
  private final Random random;

  /**
   * Creates a new RandomRangeEvaluationsStrategy with the specified range.
   *
   * @param minEvaluations the minimum number of evaluations (inclusive)
   * @param maxEvaluations the maximum number of evaluations (inclusive)
   * @throws IllegalArgumentException if minEvaluations is not positive or is greater than maxEvaluations
   */
  public RandomRangeEvaluationsStrategy(int minEvaluations, int maxEvaluations) {
    this(minEvaluations, maxEvaluations, new Random());
  }

  /**
   * Creates a new RandomRangeEvaluationsStrategy with the specified range and random seed.
   * This constructor is primarily for testing purposes.
   *
   * @param minEvaluations the minimum number of evaluations (inclusive)
   * @param maxEvaluations the maximum number of evaluations (inclusive)
   * @param random the random number generator to use
   * @throws NullPointerException if random is null
   * @throws IllegalArgumentException if minEvaluations is not positive or is greater than maxEvaluations
   */
  public RandomRangeEvaluationsStrategy(int minEvaluations, int maxEvaluations, Random random) {
    Check.notNull(random);
    Check.that(minEvaluations > 0, "Minimum evaluations must be positive: " + minEvaluations);
    Check.that(maxEvaluations >= minEvaluations, 
          "Maximum evaluations (" + maxEvaluations + ") must be greater than or equal to " 
          + "minimum evaluations (" + minEvaluations + ")");
    this.minEvaluations = minEvaluations;
    this.maxEvaluations = maxEvaluations;
    this.random = random;
  }

  @Override
  public int getEvaluations(int problemIndex) {
    // We don't use problemIndex for random strategy, but we still validate it
    if (problemIndex < 0) {
      throw new IndexOutOfBoundsException("Problem index cannot be negative: " + problemIndex);
    }
    return minEvaluations + random.nextInt(maxEvaluations - minEvaluations + 1);
  }

  @Override
  public void validate(int numberOfProblems) {
    // For random strategy, we just need to ensure numberOfProblems is valid
    Check.that(numberOfProblems > 0, "Number of problems must be positive: " + numberOfProblems);
  }

  /**
   * Returns the minimum number of evaluations.
   *
   * @return the minimum evaluations
   */
  public int getMinEvaluations() {
    return minEvaluations;
  }

  /**
   * Returns the maximum number of evaluations.
   *
   * @return the maximum evaluations
   */
  public int getMaxEvaluations() {
    return maxEvaluations;
  }
}
