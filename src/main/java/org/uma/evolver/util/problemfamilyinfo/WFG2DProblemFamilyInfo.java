package org.uma.evolver.util.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class WFG2DProblemFamilyInfo implements ProblemFamilyInfo {
  private static final List<Problem<DoubleSolution>> problemList =
      List.of(
          new WFG1(),
          new WFG2(),
          new WFG3(),
          new WFG4(),
          new WFG5(),
          new WFG6(),
          new WFG7(),
          new WFG8(),
          new WFG9());

  private static final List<String> referenceFrontFileName =
      List.of(
          "resources/referenceFronts/WFG1.2D.csv",
          "resources/referenceFronts/WFG2.2D.csv",
          "resources/referenceFronts/WFG3.2D.csv",
          "resources/referenceFronts/WFG4.2D.csv",
          "resources/referenceFronts/WFG5.2D.csv",
          "resources/referenceFronts/WFG6.2D.csv",
          "resources/referenceFronts/WFG7.2D.csv",
          "resources/referenceFronts/WFG8.2D.csv",
          "resources/referenceFronts/WFG9.2D.csv");

  private static final List<Integer> evaluationsToOptimize =
      new ArrayList<>(Collections.nCopies(problemList.size(), 25000));

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
    return "WFG2D";
  }
}
