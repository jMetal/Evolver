package org.uma.evolver.meta.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.meta.problem.ConstrainedIndicatorValue;
import org.uma.jmetal.solution.Solution;

/**
 * The diagnosis of feasibility of the configurations of a training on problems with constraints,
 * written by {@link ConsolidatedOutputResults} and {@link TreeOutputResults} next to their
 * indicator files: {@code FEASIBILITY.csv} for the non-dominated configurations of each
 * checkpoint, with the same rows as {@code INDICATORS.csv}, and {@code POPULATION_FEASIBILITY.csv}
 * for the whole population when it is written. Its columns are {@code
 * Evaluation,SolutionId,FailedRuns,Runs,FeasibleRunRate,MeanScaledViolation}: the runs without
 * feasible solutions, all the runs, the fraction of the runs with feasible solutions, and the mean
 * scaled violation of the runs without them (see {@link ConstrainedIndicatorValue}). The
 * diagnosis is in a file of its own because the readers of {@code INDICATORS.csv} take every column
 * after {@code SolutionId} as an indicator.
 */
final class FeasibilityOutput {

  static final String FEASIBILITY_FILE = "FEASIBILITY.csv";
  static final String POPULATION_FEASIBILITY_FILE = "POPULATION_FEASIBILITY.csv";

  private static final String HEADER =
      "Evaluation,SolutionId,FailedRuns,Runs,FeasibleRunRate,MeanScaledViolation";

  private FeasibilityOutput() {}

  /**
   * Appends the diagnosis of some configurations to a file, writing its header first if the file
   * is empty.
   *
   * @param outputDirectoryName the output directory
   * @param fileName the file
   * @param evaluations the meta-evaluations of the checkpoint
   * @param solutions the configurations, in the order of the indicator file
   * @throws IOException if the file cannot be written
   */
  static void write(
      String outputDirectoryName,
      String fileName,
      int evaluations,
      List<? extends Solution<?>> solutions)
      throws IOException {
    File file = new File(outputDirectoryName, fileName);
    boolean empty = file.length() == 0;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
      if (empty) {
        writer.write(HEADER);
        writer.newLine();
      }
      for (int i = 0; i < solutions.size(); i++) {
        writer.write(line(evaluations, i, solutions.get(i)));
        writer.newLine();
      }
    }
  }

  /** The line of a configuration: empty values if it carries no diagnosis. */
  static String line(int evaluations, int solutionId, Solution<?> solution) {
    Object failedRuns = solution.attributes().get(ConstrainedIndicatorValue.FAILED_RUNS);
    Object runs = solution.attributes().get(ConstrainedIndicatorValue.RUNS);
    Object violation = solution.attributes().get(ConstrainedIndicatorValue.MEAN_SCALED_VIOLATION);
    if (!(failedRuns instanceof Integer failed)
        || !(runs instanceof Integer total)
        || !(violation instanceof Double meanViolation)) {
      return evaluations + "," + solutionId + ",,,,";
    }
    return String.format(
        Locale.ROOT,
        "%d,%d,%d,%d,%s,%s",
        evaluations,
        solutionId,
        failed,
        total,
        total == 0 ? 0.0 : (double) (total - failed) / total,
        meanViolation);
  }
}
