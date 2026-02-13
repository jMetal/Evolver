package org.uma.evolver.util.trainingset;

import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the WFG family configured for two-objective (2D) problems.
 *
 * <p>The {@code 2D} suffix is included here to make explicit that this set targets bi-objective
 * variants of the WFG family. If other dimensionalities are added in the future, follow the same
 * naming convention (e.g. {@code WFG3DTrainingSet}).
 */
public class WFG2DTrainingSet extends AbstractTrainingSet<DoubleSolution> {

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 25000;
  private static final String NAME = "WFG2D";

  private static final List<Problem<DoubleSolution>> PROBLEMS =
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

  private static final List<String> REFERENCE_FRONTS =
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

  private static final List<Integer> EVALUATIONS =
      Collections.nCopies(PROBLEMS.size(), DEFAULT_NUMBER_OF_EVALUATIONS);

  public WFG2DTrainingSet() {
    super(PROBLEMS, REFERENCE_FRONTS, EVALUATIONS, NAME);
  }
}
