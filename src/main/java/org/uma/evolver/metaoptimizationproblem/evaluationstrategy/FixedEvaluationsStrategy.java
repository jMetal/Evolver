package org.uma.evolver.metaoptimizationproblem.evaluationstrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.uma.jmetal.util.errorchecking.Check;

/**
 * Implementation of EvaluationStrategy that uses fixed evaluation counts for each problem.
 * The number of evaluations is specified explicitly for each problem in the constructor.
 *
 * @author Antonio J. Nebro
 */
public class FixedEvaluationsStrategy implements EvaluationStrategy {
  private final List<Integer> evaluations;

  /**
   * Creates a new FixedEvaluationsStrategy with the specified evaluation counts.
   *
   * @param evaluations a list containing the number of evaluations for each problem
   * @throws NullPointerException if evaluations is null
   * @throws IllegalArgumentException if evaluations is empty
   */
  public FixedEvaluationsStrategy(List<Integer> evaluations) {
    Check.notNull(evaluations);
    Check.that(!evaluations.isEmpty(), "The evaluations list cannot be empty");
    
    // Validate all evaluation counts are positive
    for (int i = 0; i < evaluations.size(); i++) {
      Check.that(evaluations.get(i) > 0, 
          "Evaluation count must be positive, but got " + evaluations.get(i) + 
          " at index " + i);
    }
    
    this.evaluations = new ArrayList<>(evaluations);
  }

  @Override
  public int getEvaluations(int problemIndex) {
    Check.that(problemIndex >= 0 && problemIndex < evaluations.size(), 
        "Problem index " + problemIndex + " is out of bounds [0," + (evaluations.size() - 1) + "]");
    return evaluations.get(problemIndex);
  }

  @Override
  public void validate(int numberOfProblems) {
    // We only need to check the number of problems here
    // The constructor already ensures all evaluation counts are positive
    Check.that(evaluations.size() == numberOfProblems, 
        "Number of evaluation counts (" + evaluations.size() + 
        ") must match number of problems (" + numberOfProblems + ")");
  }

  /**
   * Returns an unmodifiable view of the evaluation counts.
   *
   * @return the list of evaluation counts
   */
  public List<Integer> getEvaluations() {
    return Collections.unmodifiableList(evaluations);
  }
}
