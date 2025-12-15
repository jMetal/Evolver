package org.uma.evolver.trainingset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the ZDT family (bi-objective problems).
 *
 * <p>ZDT problems are bi-objective by definition, so a dimensional suffix (e.g. "2D") is redundant
 * and intentionally omitted from the class name. The class groups ZDT1, ZDT2, ZDT3, ZDT4 and ZDT6.
 */
public class ZDTTrainingSet implements TrainingSet<DoubleSolution> {
  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 10000 ;

  private static final List<Problem<DoubleSolution>> problemList =
      List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());

  private static final List<String> referenceFrontFileName =
          List.of("resources/referenceFronts/ZDT1.csv",
                  "resources/referenceFronts/ZDT2.csv",
                  "resources/referenceFronts/ZDT3.csv",
                  "resources/referenceFronts/ZDT4.csv",
                  "resources/referenceFronts/ZDT6.csv") ;


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
    return "ZDT";
  }
}
