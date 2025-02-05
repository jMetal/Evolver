package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.*;

public class ZCATReducedProblemFamilyInfo implements ProblemFamilyInfo {
  private static final int OBJECTIVES = 3;
  private static final int VARIABLES = 30;
  private static final boolean COMPLICATED_PARETO_SET = false;
  private static final int LEVEL = 1;
  private static final boolean BIAS = false;
  private static final boolean IMBALANCE = false;

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 50000;

  private static final List<DoubleProblem> problemList =
      List.of(
          new ZCAT1(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT2(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT3(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT8(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT11(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT14(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE));
  private static final List<String> referenceFrontFileName =
      List.of(
          "resources/referenceFronts/ZCAT1.3D.csv",
          "resources/referenceFronts/ZCAT2.3D.csv",
          "resources/referenceFronts/ZCAT3.3D.csv",
          "resources/referenceFronts/ZCAT8.3D.csv",
          "resources/referenceFronts/ZCAT11.3D.csv",
          "resources/referenceFronts/ZCAT14.3D.csv");

  private static final List<Integer> evaluationsToOptimize =
      new ArrayList<>(Collections.nCopies(problemList.size(), DEFAULT_NUMBER_OF_EVALUATIONS));

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
