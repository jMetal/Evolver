package org.uma.evolver.trainingset;

import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the ZDT family (bi-objective problems).
 *
 * <p>
 * ZDT problems are bi-objective by definition, so a dimensional suffix (e.g.
 * "2D") is redundant
 * and intentionally omitted from the class name. The class groups ZDT1, ZDT2,
 * ZDT3, ZDT4 and ZDT6.
 */
public class ZDTTrainingSet extends AbstractTrainingSet<DoubleSolution> {

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 10000;
  private static final String NAME = "ZDT";

  private static final List<Problem<DoubleSolution>> PROBLEMS = List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(),
      new ZDT6());

  private static final List<String> REFERENCE_FRONT_FILE_NAMES = List.of("ZDT1.csv", "ZDT2.csv", "ZDT3.csv", "ZDT4.csv",
      "ZDT6.csv");

  private static final List<Integer> EVALUATIONS = Collections.nCopies(PROBLEMS.size(), DEFAULT_NUMBER_OF_EVALUATIONS);

  public ZDTTrainingSet() {
    super(PROBLEMS, REFERENCE_FRONT_FILE_NAMES, EVALUATIONS, NAME);
  }
}
