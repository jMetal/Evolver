package org.uma.evolver.trainingset;

import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract base class for {@link TrainingSet} implementations that eliminates boilerplate code.
 *
 * <p>Subclasses only need to provide the configuration data through constructor parameters,
 * while this class handles the common method implementations.
 *
 * @param <S> the solution type
 */
public abstract class AbstractTrainingSet<S extends Solution<?>> implements TrainingSet<S> {

  private final List<Problem<S>> problemList;
  private final List<String> referenceFronts;
  private List<Integer> evaluationsToOptimize;
  private final String name;

  /**
   * Constructs a training set with the specified configuration.
   *
   * @param problemList the list of problems in this training set
   * @param referenceFronts the list of reference front file paths
   * @param evaluationsToOptimize the list of evaluation budgets per problem
   * @param name the name identifier for this training set
   * @throws IllegalArgumentException if lists have different sizes or are empty
   */
  protected AbstractTrainingSet(
      List<Problem<S>> problemList,
      List<String> referenceFronts,
      List<Integer> evaluationsToOptimize,
      String name) {
    
    validateInputs(problemList, referenceFronts, evaluationsToOptimize, name);
    
    this.problemList = List.copyOf(problemList);
    this.referenceFronts = List.copyOf(referenceFronts);
    this.evaluationsToOptimize = List.copyOf(evaluationsToOptimize);
    this.name = name;
  }

  private void validateInputs(
      List<Problem<S>> problemList,
      List<String> referenceFronts,
      List<Integer> evaluationsToOptimize,
      String name) {
    
    if (problemList == null || problemList.isEmpty()) {
      throw new IllegalArgumentException("Problem list cannot be null or empty");
    }
    if (referenceFronts == null || referenceFronts.size() != problemList.size()) {
      throw new IllegalArgumentException(
          "Reference fronts list must have same size as problem list");
    }
    if (evaluationsToOptimize == null || evaluationsToOptimize.size() != problemList.size()) {
      throw new IllegalArgumentException(
          "Evaluations list must have same size as problem list");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name cannot be null or blank");
    }
  }

  @Override
  public List<Problem<S>> problemList() {
    return problemList;
  }

  @Override
  public List<String> referenceFronts() {
    return referenceFronts;
  }

  @Override
  public List<Integer> evaluationsToOptimize() {
    return evaluationsToOptimize;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public TrainingSet<S> setEvaluationsToOptimize(int evaluations) {
    if (evaluations <= 0) {
      throw new IllegalArgumentException("Evaluations must be positive: " + evaluations);
    }
    this.evaluationsToOptimize = Collections.nCopies(problemList.size(), evaluations);
    return this;
  }

  @Override
  public TrainingSet<S> setEvaluationsToOptimize(List<Integer> evaluations) {
    if (evaluations == null || evaluations.size() != problemList.size()) {
      throw new IllegalArgumentException(
          "Evaluations list must have same size as problem list (" + problemList.size() + ")");
    }
    for (int i = 0; i < evaluations.size(); i++) {
      if (evaluations.get(i) <= 0) {
        throw new IllegalArgumentException(
            "Evaluation at index " + i + " must be positive: " + evaluations.get(i));
      }
    }
    this.evaluationsToOptimize = List.copyOf(evaluations);
    return this;
  }
}
