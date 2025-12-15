package org.uma.evolver.trainingset;

import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Interface defining methods for returning information about problem families, including a list with their
 * implementations, a list with the names of the reference fronts, and a list containing the typical number
 * of evaluations used to optimize them by metaheuristics.
 *
 * <p>Naming convention used by classes in this package:
 * <ul>
 *   <li>Classes are named after the problem family (e.g. {@code ZDT}, {@code DTLZ}, {@code WFG}, {@code RE}).</li>
 *   <li>If a problem family supports multiple objective dimensionalities, a suffix with the dimensionality
 *   may be appended (for example {@code DTLZ3DTrainingSet}). For families that are fixed to a single
 *   dimensionality (e.g. ZDT are bi-objective), the dimensional suffix is omitted (for example {@code ZDTTrainingSet}).</li>
 * </ul>
 */
public interface TrainingSet<S extends Solution<?>> {
  List<Problem<S>> problemList();
  List<String> referenceFronts();
  List<Integer> evaluationsToOptimize();

  String name() ;
}
