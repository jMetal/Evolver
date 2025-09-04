package org.uma.evolver.util.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.re.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class RE3DProblemFamilyInfo implements ProblemFamilyInfo<DoubleSolution> {
  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 8000;

  private static final List<Problem<DoubleSolution>> problemList =
      List.of(new RE31(), new RE32(), new RE33(), new RE34(), new RE35(), new RE36(), new RE37());

  private static final List<String> referenceFrontFileName =
      IntStream.range(1, problemList.size() + 1)
          .mapToObj(id -> "resources/referenceFronts/RE3" + id + ".csv")
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
    return "RE3D";
  }
}
