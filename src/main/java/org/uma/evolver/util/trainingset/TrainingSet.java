package org.uma.evolver.util.trainingset;

import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Interface defining methods for returning information about problem families, including a list
 * with their implementations, a list with the names of the reference fronts, and a list containing
 * the typical number of evaluations used to optimize them by metaheuristics.
 *
 * <p>Naming convention used by classes in this package:
 *
 * <ul>
 *   <li>Classes are named after the problem family (e.g. {@code ZDT}, {@code DTLZ}, {@code WFG},
 *       {@code RE}).
 *   <li>If a problem family supports multiple objective dimensionalities, a suffix with the
 *       dimensionality may be appended (for example {@code DTLZ3DTrainingSet}). For families that
 *       are fixed to a single dimensionality (e.g. ZDT are bi-objective), the dimensional suffix is
 *       omitted (for example {@code ZDTTrainingSet}).
 * </ul>
 *
 * @param <S> the solution type
 */
public interface TrainingSet<S extends Solution<?>> {

  List<Problem<S>> problemList();

  List<String> referenceFronts();

  List<Integer> evaluationsToOptimize();

  String name();

  /**
   * Sets the same number of evaluations for all problems in the training set.
   *
   * @param evaluations the number of evaluations to use for all problems
   * @return this training set instance for method chaining
   */
  TrainingSet<S> setEvaluationsToOptimize(int evaluations);

  /**
   * Sets different numbers of evaluations for each problem in the training set.
   *
   * @param evaluations the list of evaluation counts, one per problem
   * @return this training set instance for method chaining
   */
  TrainingSet<S> setEvaluationsToOptimize(List<Integer> evaluations);
}
