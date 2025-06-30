package org.uma.evolver.util.problemfamilyinfo;

import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Interface defining methods for returning information about problem families, including a list with their
 * implementations, a list with the names of the reference fronts, and a list containing the typical number
 * of evaluations used to optimize them by metaheuristics.
 */
public interface ProblemFamilyInfo<S extends Solution<?>> {
  List<Problem<S>> problemList();
  List<String> referenceFronts();
  List<Integer> evaluationsToOptimize();

  String name() ;
}
