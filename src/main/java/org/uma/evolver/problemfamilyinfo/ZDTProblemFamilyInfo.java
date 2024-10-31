package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.*;

public class ZDTProblemFamilyInfo implements ProblemFamilyInfo {
  private static final int defaultNumberOfEvaluations = 25000 ;

  private static final List<DoubleProblem> problemList =
      List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());

  private static final List<String> referenceFrontFileName =
          IntStream.range(0, problemList.size())
                  .mapToObj(id -> "resources/referenceFronts/ZDT" + id + ".csv")
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
    return "ZDT";
  }
}
