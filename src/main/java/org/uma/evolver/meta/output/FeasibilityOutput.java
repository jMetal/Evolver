package org.uma.evolver.meta.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.meta.problem.ConstrainedIndicatorValue;
import org.uma.evolver.meta.problem.ProblemFeasibility;
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
 *
 * <p>Next to each of them, {@code FEASIBILITY_BY_PROBLEM.csv} and {@code
 * POPULATION_FEASIBILITY_BY_PROBLEM.csv} give the same diagnosis by training problem, with a row per
 * configuration and problem: {@code
 * Evaluation,SolutionId,ProblemId,Problem,FailedRuns,Runs,MeanScaledViolation}, where {@code
 * ProblemId} is the position of the problem in the training set, from 0 (two problems may have the
 * same name).
 */
final class FeasibilityOutput {

  static final String FEASIBILITY_FILE = "FEASIBILITY.csv";
  static final String POPULATION_FEASIBILITY_FILE = "POPULATION_FEASIBILITY.csv";
  static final String FEASIBILITY_BY_PROBLEM_FILE = "FEASIBILITY_BY_PROBLEM.csv";
  static final String POPULATION_FEASIBILITY_BY_PROBLEM_FILE =
      "POPULATION_FEASIBILITY_BY_PROBLEM.csv";

  private static final String HEADER =
      "Evaluation,SolutionId,FailedRuns,Runs,FeasibleRunRate,MeanScaledViolation";

  private static final String BY_PROBLEM_HEADER =
      "Evaluation,SolutionId,ProblemId,Problem,FailedRuns,Runs,MeanScaledViolation";

  private FeasibilityOutput() {}

  /**
   * Appends the diagnosis of the non-dominated configurations of a checkpoint to their files.
   *
   * @param outputDirectoryName the output directory
   * @param evaluations the meta-evaluations of the checkpoint
   * @param solutions the configurations, in the order of the indicator file
   * @throws IOException if a file cannot be written
   */
  static void writeFront(
      String outputDirectoryName, int evaluations, List<? extends Solution<?>> solutions)
      throws IOException {
    write(outputDirectoryName, FEASIBILITY_FILE, HEADER, lines(evaluations, solutions));
    write(
        outputDirectoryName,
        FEASIBILITY_BY_PROBLEM_FILE,
        BY_PROBLEM_HEADER,
        linesByProblem(evaluations, solutions));
  }

  /**
   * Appends the diagnosis of the whole population of a checkpoint to its files.
   *
   * @param outputDirectoryName the output directory
   * @param evaluations the meta-evaluations of the checkpoint
   * @param solutions the configurations, in the order of the indicator file
   * @throws IOException if a file cannot be written
   */
  static void writePopulation(
      String outputDirectoryName, int evaluations, List<? extends Solution<?>> solutions)
      throws IOException {
    write(outputDirectoryName, POPULATION_FEASIBILITY_FILE, HEADER, lines(evaluations, solutions));
    write(
        outputDirectoryName,
        POPULATION_FEASIBILITY_BY_PROBLEM_FILE,
        BY_PROBLEM_HEADER,
        linesByProblem(evaluations, solutions));
  }

  /** Appends some lines to a file, writing its header first if the file is empty. */
  private static void write(
      String outputDirectoryName, String fileName, String header, List<String> lines)
      throws IOException {
    File file = new File(outputDirectoryName, fileName);
    boolean empty = file.length() == 0;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
      if (empty) {
        writer.write(header);
        writer.newLine();
      }
      for (String line : lines) {
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private static List<String> lines(int evaluations, List<? extends Solution<?>> solutions) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < solutions.size(); i++) {
      lines.add(line(evaluations, i, solutions.get(i)));
    }
    return lines;
  }

  /** A line per configuration and problem; none for a configuration that carries no diagnosis. */
  private static List<String> linesByProblem(
      int evaluations, List<? extends Solution<?>> solutions) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < solutions.size(); i++) {
      Object attribute =
          solutions.get(i).attributes().get(ConstrainedIndicatorValue.FEASIBILITY_BY_PROBLEM);
      if (!(attribute instanceof List<?> problems)) {
        continue;
      }
      for (int id = 0; id < problems.size(); id++) {
        if (problems.get(id) instanceof ProblemFeasibility problem) {
          lines.add(
              String.format(
                  Locale.ROOT,
                  "%d,%d,%d,%s,%d,%d,%s",
                  evaluations,
                  i,
                  id,
                  problem.problem(),
                  problem.failedRuns(),
                  problem.runs(),
                  problem.meanScaledViolation()));
        }
      }
    }
    return lines;
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
