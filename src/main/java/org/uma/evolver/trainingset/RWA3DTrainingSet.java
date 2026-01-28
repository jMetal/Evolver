package org.uma.evolver.trainingset;

import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.rwa.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the RE problems having three objective functions.
 *
 * <p>The {@code 3D} suffix indicates this training set targets three-objective variants of the RE
 * family.
 */
public class RWA3DTrainingSet extends AbstractTrainingSet<DoubleSolution> {

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 7000;
  private static final String NAME = "RWA3D";

  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(new RWA2(), new RWA3(), new RWA4(), new RWA5(), new RWA6(), new RWA7());

  private static final List<String> REFERENCE_FRONTS =
      List.of("resources/estimatedReferenceFronts/RWA2.csv",
              "resources/estimatedReferenceFronts/RWA3.csv",
              "resources/estimatedReferenceFronts/RWA4.csv",
              "resources/estimatedReferenceFronts/RWA5.csv",
              "resources/estimatedReferenceFronts/RWA6.csv",
              "resources/estimatedReferenceFronts/RWA7.csv");
  private static final List<Integer> EVALUATIONS =
      Collections.nCopies(PROBLEMS.size(), DEFAULT_NUMBER_OF_EVALUATIONS);

  public RWA3DTrainingSet() {
    super(PROBLEMS, REFERENCE_FRONTS, EVALUATIONS, NAME);
  }
}
