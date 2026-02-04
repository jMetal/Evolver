package org.uma.evolver.analysis.ablation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.trainingset.WFG2DTrainingSet;
import org.uma.evolver.trainingset.ZDTTrainingSet;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Factory for creating problem suites and training sets with reference fronts.
 */
public final class ProblemSuiteFactory {

  private ProblemSuiteFactory() {
    // Utility class
  }

  /**
   * Creates a training set for the given suite.
   *
   * @param suite the problem suite
   * @return a training set
   */
  public static TrainingSet<DoubleSolution> createTrainingSet(ProblemSuite suite) {
    TrainingSet<DoubleSolution> result;
    if (suite == ProblemSuite.ZDT) {
      result = new ZDTTrainingSet();
    } else if (suite == ProblemSuite.DTLZ) {
      result = new DTLZ3DTrainingSet();
    } else if (suite == ProblemSuite.WFG) {
      result = new WFG2DTrainingSet();
    } else {
      throw new IllegalArgumentException("Unsupported problem suite: " + suite);
    }
    return result;
  }

  /**
   * Creates a list of problems with loaded reference fronts.
   *
   * @param suiteName the suite name
   * @return list of problems with their reference fronts
   */
  public static List<ProblemWithReferenceFront<DoubleSolution>> createProblemSuite(String suiteName) {
    List<ProblemWithReferenceFront<DoubleSolution>> result;
    var suite = ProblemSuite.fromString(suiteName);
    var trainingSet = createTrainingSet(suite);
    result = loadProblemSuite(trainingSet);
    return result;
  }

  /**
   * Returns the problem names for a suite.
   *
   * @param suiteName the suite name
   * @return list of problem names
   */
  public static List<String> getProblemNames(String suiteName) {
    List<String> result;
    var suite = ProblemSuite.fromString(suiteName);
    var trainingSet = createTrainingSet(suite);
    result = trainingSet.problemList().stream()
        .map(problem -> problem.getClass().getSimpleName())
        .toList();
    return result;
  }

  private static List<ProblemWithReferenceFront<DoubleSolution>> loadProblemSuite(
      TrainingSet<DoubleSolution> trainingSet) {
    List<ProblemWithReferenceFront<DoubleSolution>> result;
    var problems = trainingSet.problemList();
    var referenceFronts = trainingSet.referenceFronts();
    var bundle = new ArrayList<ProblemWithReferenceFront<DoubleSolution>>();

    for (int i = 0; i < problems.size(); i++) {
      var problem = problems.get(i);
      var referenceFrontPath = referenceFronts.get(i);
      double[][] referenceFront;
      try {
        referenceFront = VectorUtils.readVectors(referenceFrontPath, ",");
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to load reference front: " + referenceFrontPath, e);
      }
      bundle.add(new ProblemWithReferenceFront<>(problem, referenceFront,
          problem.getClass().getSimpleName()));
    }

    result = List.copyOf(bundle);
    return result;
  }
}
