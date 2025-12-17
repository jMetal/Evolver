package org.uma.evolver.trainingset;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.re.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the RE problems having three objective functions.
 *
 * <p>The {@code 3D} suffix indicates this training set targets three-objective variants of the RE
 * family.
 */
public class RE3DTrainingSet extends AbstractTrainingSet<DoubleSolution> {

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 8000;
  private static final String NAME = "RE3D";

  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(new RE31(), new RE32(), new RE33(), new RE34(), new RE35(), new RE36(), new RE37());

  private static final List<String> REFERENCE_FRONTS =
      IntStream.range(1, PROBLEMS.size() + 1)
          .mapToObj(id -> "resources/referenceFronts/RE3" + id + ".csv")
          .toList();

  private static final List<Integer> EVALUATIONS =
      Collections.nCopies(PROBLEMS.size(), DEFAULT_NUMBER_OF_EVALUATIONS);

  public RE3DTrainingSet() {
    super(PROBLEMS, REFERENCE_FRONTS, EVALUATIONS, NAME);
  }
}
