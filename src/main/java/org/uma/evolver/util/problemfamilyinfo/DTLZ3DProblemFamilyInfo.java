package org.uma.evolver.util.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class DTLZ3DProblemFamilyInfo implements ProblemFamilyInfo {
  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 16000;

  private static final List<Problem<DoubleSolution>> problemList =
      List.of(
          new DTLZ1(),
          new DTLZ2(),
          new DTLZ3(),
          new DTLZ4(),
          new DTLZ5(),
          new DTLZ6(),
          new DTLZ7());

  private static final List<String> referenceFrontFileName =
      IntStream.range(1, problemList.size() + 1)
          .mapToObj(id -> "resources/referenceFronts/DTLZ" + id + ".3D.csv")
          .toList();

  private static final List<Integer> evaluationsToOptimize =
      new ArrayList<>(Collections.nCopies(problemList.size(), DEFAULT_NUMBER_OF_EVALUATIONS));

  @Override
  public List<Problem<DoubleSolution>> problemList() {
    return problemList;
  }

  @Override
  public List<String> referenceFronts() {
    return referenceFrontFileName;
  }

  @Override
  public List<Integer> evaluationsToOptimize() {
    return evaluationsToOptimize;
  }

  @Override
  public String name() {
    return "DTLZ3D";
  }
}
