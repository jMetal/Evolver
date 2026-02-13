package org.uma.evolver.trainingset;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Training set for the DTLZ family configured for three-objective (3D) problems.
 *
 * <p>DTLZ problems can be defined for multiple objective dimensionalities; therefore the class name
 * includes the dimensional suffix {@code 3D} to indicate the specific configuration provided here.
 */
public class DTLZ3DTrainingSet extends AbstractTrainingSet<DoubleSolution> {

  private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 16000;
  private static final String NAME = "DTLZ3D";

  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(
          new DTLZ1(), new DTLZ2(), new DTLZ3(), new DTLZ4(), new DTLZ5(), new DTLZ6(), new DTLZ7());

  private static final List<String> REFERENCE_FRONTS =
      IntStream.range(1, PROBLEMS.size() + 1)
          .mapToObj(id -> "resources/referenceFronts/DTLZ" + id + ".3D.csv")
          .toList();

  private static final List<Integer> EVALUATIONS =
      Collections.nCopies(PROBLEMS.size(), DEFAULT_NUMBER_OF_EVALUATIONS);

  public DTLZ3DTrainingSet() {
    super(PROBLEMS, REFERENCE_FRONTS, EVALUATIONS, NAME);
  }
}
