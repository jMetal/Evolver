package org.uma.evolver.problemfamilyinfo;

import java.util.List;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;

/**
 * Interface defining methods for returning information about problem families, including a list with their
 * implementations, a list with the names of the reference fronts, and a list containing the typical number
 * of evaluations used to optimize them by metaheuristics.
 */
public interface ProblemFamilyInfo {
  List<DoubleProblem> problemList();
  List<String> referenceFronts();
  List<Integer> evaluationsToOptimize();
}
