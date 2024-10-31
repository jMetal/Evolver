package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.*;

public class ZCATFullProblemFamilyInfo implements ProblemFamilyInfo {
  private static int objectives = 2 ;
  private static int variables = 30 ;
  private static boolean complicatedParetoSet = false ;
  private static int level = 1 ;
  private static boolean bias = false ;
  private static boolean imbalance = false ;

  private static int defaultNumberOfEvaluations = 25000 ;

  private static final List<DoubleProblem> problemList =
      List.of(
          new ZCAT1(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT2(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT3(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT4(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT5(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT6(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT7(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT8(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT9(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT10(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT11(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT12(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT13(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT14(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT15(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT16(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT17(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT18(objectives, variables, complicatedParetoSet, level, bias, imbalance),
          new ZCAT19(objectives, variables, complicatedParetoSet, level, bias, imbalance));

  private static final List<String> referenceFrontFileName =
      IntStream.range(0, problemList.size())
          .mapToObj(id -> "resources/referenceFronts/ZCAT" + id + ".2D.csv")
          .toList();

  private static final List<Integer> evaluationsToOptimize =
      new ArrayList<>(Collections.nCopies(problemList.size(), defaultNumberOfEvaluations));

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
    return "ZCATFull";
  }
}
