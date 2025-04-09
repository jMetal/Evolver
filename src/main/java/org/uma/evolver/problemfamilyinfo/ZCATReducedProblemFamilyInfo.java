package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.*;

public class ZCATReducedProblemFamilyInfo implements ProblemFamilyInfo {
  private static final int OBJECTIVES = 2;
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
          new ZCAT4(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE),
          new ZCAT5(OBJECTIVES, VARIABLES, COMPLICATED_PARETO_SET, LEVEL, BIAS, IMBALANCE));

  private static final List<String> referenceFrontFileName =
      List.of(
          "resources/referenceFrontsCSV/ZCAT1.2D.csv",
          "resources/referenceFrontsCSV/ZCAT2.2D.csv",
          "resources/referenceFrontsCSV/ZCAT3.2D.csv",
          "resources/referenceFrontsCSV/ZCAT4.2D.csv",
          "resources/referenceFrontsCSV/ZCAT5.2D.csv");

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
