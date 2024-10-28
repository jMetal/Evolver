package org.uma.evolver.problemfamilyinfo;

import java.util.List;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;

public class ZCATReducedProblemFamilyInfo implements ProblemFamilyInfo {
  private static final List<DoubleProblem> problemList =
      List.of(new ZCAT2_2D(), new ZCAT6_2D(), new ZCAT12_2D(), new ZCAT14_2D(),
              new ZCAT19_2D());
  private static final List<String> referenceFrontFileName =
      List.of(
              "resources/referenceFronts/ZCAT2.2D.csv",
              "resources/referenceFronts/ZCAT6.2D.csv",
              "resources/referenceFronts/ZCAT12.2D.csv",
              "resources/referenceFronts/ZCAT14.2D.csv",
              "resources/referenceFronts/ZCAT19.2D.csv");

  private static final List<Integer> evaluationsToOptimize =
      List.of(25000, 25000, 25000, 25000, 25000);

  @Override
  public List<DoubleProblem> problemList() {
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
    return "ZCATReduced";
  }
}
