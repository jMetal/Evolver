package org.uma.evolver.meta.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * Output writer for tree-encoded meta-optimization experiments.
 *
 * <p>Generates the same file structure as {@link org.uma.evolver.meta.output.ConsolidatedOutputResults}:
 * <ul>
 *   <li>METADATA.txt: Experiment metadata</li>
 *   <li>INDICATORS.csv: Quality indicator values per solution over time</li>
 *   <li>CONFIGURATIONS.csv: Decoded parameter configurations per solution over time</li>
 *   <li>VAR_CONF.txt: Human-readable configurations with indicator values</li>
 *   <li>POPULATION_INDICATORS.csv and POPULATION_CONFIGURATIONS.csv, only with
 *       {@link #writePopulation(boolean)}: the same as INDICATORS.csv and CONFIGURATIONS.csv for
 *       the whole population, not only its non-dominated solutions</li>
 * </ul>
 *
 * <p>Also implements {@link Observer} so it can be registered directly on the meta-optimizer's
 * observable, writing results at a configurable frequency.
 *
 * @author Antonio J. Nebro
 */
public class TreeOutputResults implements Observer<Map<String, Object>> {

  private int evaluations;
  /** The evaluations of the last checkpoint written, or -1 if none has been written. */
  private int lastCheckpointEvaluations = -1;
  private final long creationMillis = System.currentTimeMillis();
  private Long computingTimeMillis;
  private final TreeMetaOptimizationProblem<?> problem;
  private final String problemName;
  private final List<QualityIndicator> indicators;
  private final String outputDirectoryName;
  private final MetaOptimizerConfig config;
  private final int writeFrequency;

  private boolean headersWritten = false;
  private boolean writePopulation = false;

  /**
   * Constructs the output writer.
   *
   * @param problem the tree meta-optimization problem
   * @param problemName the name of the training set
   * @param indicators the quality indicators being optimized
   * @param outputDirectoryName the output directory path
   * @param config the meta-optimizer configuration metadata
   * @param writeFrequency how often (in evaluations) to write results
   */
  public TreeOutputResults(
      TreeMetaOptimizationProblem<?> problem,
      String problemName,
      List<QualityIndicator> indicators,
      String outputDirectoryName,
      MetaOptimizerConfig config,
      int writeFrequency) {
    this.problem = problem;
    this.problemName = problemName;
    this.indicators = indicators;
    this.outputDirectoryName = outputDirectoryName;
    this.config = config;
    this.writeFrequency = writeFrequency;

    createOutputDirectory();
    writeMetadata();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    List<DerivationTreeSolution> population =
        (List<DerivationTreeSolution>) data.get("POPULATION");
    int evals = (int) data.get("EVALUATIONS");
    if ((evals % writeFrequency) == 0) {
      this.evaluations = evals;
      if (data.get("COMPUTING_TIME") instanceof Long computingTime) {
        this.computingTimeMillis = computingTime;
      }
      try {
        writeResultsToFiles(population);
      } catch (IOException e) {
        throw new JMetalException(e);
      }
    }
  }

  /**
   * Sets whether the whole population of each checkpoint is written as well, to
   * POPULATION_INDICATORS.csv and POPULATION_CONFIGURATIONS.csv (false by default); the other files
   * keep only the non-dominated solutions.
   *
   * @return this object, for chaining
   */
  public TreeOutputResults writePopulation(boolean writePopulation) {
    this.writePopulation = writePopulation;
    return this;
  }

  /**
   * Writes final results to files.
   *
   * @param solutions the solutions to write
   * @param finalEvaluations the final evaluation count
   * @throws IOException if file writing fails
   */
  public void writeFinalResults(List<DerivationTreeSolution> solutions, int finalEvaluations)
      throws IOException {
    this.evaluations = finalEvaluations;
    writeResultsToFiles(solutions);
  }

  /**
   * Writes final results to files, with the computing time of the meta-optimizer.
   *
   * @param solutions the solutions to write
   * @param finalEvaluations the final evaluation count
   * @param computingTimeMillis the computing time of the run, in milliseconds
   * @throws IOException if file writing fails
   */
  public void writeFinalResults(
      List<DerivationTreeSolution> solutions, int finalEvaluations, long computingTimeMillis)
      throws IOException {
    this.computingTimeMillis = computingTimeMillis;
    writeFinalResults(solutions, finalEvaluations);
  }

  /**
   * The computing time to write at a checkpoint, in minutes: the meta-optimizer's own clock when it
   * publishes it, or else the time elapsed since this object was created. It is written whatever the
   * stopping condition is.
   */
  private double checkpointMinutes() {
    long millis = computingTimeMillis != null
        ? computingTimeMillis
        : System.currentTimeMillis() - creationMillis;
    return millis / 60_000.0;
  }

  /**
   * Appends a wall-clock time record to METADATA.txt, in the same format as {@link
   * org.uma.evolver.meta.output.ConsolidatedOutputResults#writeWallClockTime}, so downstream analysis
   * tooling (e.g. the training-convergence scripts) can read it regardless of encoding.
   */
  public void writeWallClockTime(long elapsedTimeMillis) {
    File metadataFile = new File(outputDirectoryName, "METADATA.txt");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile, true))) {
      writer.newLine();
      writer.write("--- Execution ---");
      writer.newLine();
      writer.write("Wall-clock time: " + formatDuration(elapsedTimeMillis)
          + " (" + elapsedTimeMillis + " ms)");
      writer.newLine();
      writer.write("Meta-evaluations performed: " + evaluations);
      writer.newLine();
    } catch (IOException e) {
      throw new JMetalException(e);
    }
  }

  private static String formatDuration(long elapsedTimeMillis) {
    long totalSeconds = elapsedTimeMillis / 1000;
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    return hours + "h " + minutes + "m " + seconds + "s";
  }

  private void writeResultsToFiles(List<DerivationTreeSolution> solutions) throws IOException {
    if (evaluations == lastCheckpointEvaluations) {
      // The final front is written once more at the end of the run: when its last
      // meta-evaluation falls on a periodic checkpoint, that checkpoint already holds it
      computingTimeMillis = null;
      return;
    }
    lastCheckpointEvaluations = evaluations;
    if (!headersWritten) {
      writeHeaders("INDICATORS.csv", "CONFIGURATIONS.csv");
      if (writePopulation) {
        writeHeaders("POPULATION_INDICATORS.csv", "POPULATION_CONFIGURATIONS.csv");
      }
      headersWritten = true;
    }

    Archive<DerivationTreeSolution> archive = new NonDominatedSolutionListArchive<>();
    solutions.forEach(archive::add);
    List<DerivationTreeSolution> nonDominatedSolutions = archive.solutions();

    writeIndicators(nonDominatedSolutions, "INDICATORS.csv");
    writeConfigurations(nonDominatedSolutions, "CONFIGURATIONS.csv");
    writeVarConf(nonDominatedSolutions);
    if (writePopulation) {
      writeIndicators(solutions, "POPULATION_INDICATORS.csv");
      writeConfigurations(solutions, "POPULATION_CONFIGURATIONS.csv");
    }
    computingTimeMillis = null;
  }

  private void createOutputDirectory() {
    File outputDirectory = new File(outputDirectoryName);
    if (!outputDirectory.exists()) {
      boolean result = outputDirectory.mkdirs();
      if (!result) {
        throw new JMetalException("Error creating directory " + outputDirectoryName);
      }
    }
  }

  private void writeMetadata() {
    File metadataFile = new File(outputDirectoryName, "METADATA.txt");
    if (metadataFile.exists()) {
      return;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile))) {
      writer.write("=== Meta-Optimization Experiment (Tree Encoding) ===");
      writer.newLine();
      writer.write("Date: " + LocalDateTime.now());
      writer.newLine();
      writer.newLine();

      writer.write("--- Meta-Optimizer ---");
      writer.newLine();
      writer.write("Algorithm: " + config.metaOptimizerName());
      writer.newLine();
      writer.write("Encoding: Derivation Tree (GGGP)");
      writer.newLine();
      for (String line : config.stoppingConditionLines()) {
        writer.write(line);
        writer.newLine();
      }
      writer.write("Population Size: " + config.metaPopulationSize());
      writer.newLine();
      writer.write("Cores: " + config.numberOfCores());
      writer.newLine();
      writer.newLine();

      writer.write("--- Base-Level Algorithm ---");
      writer.newLine();
      writer.write("Algorithm: " + config.baseLevelAlgorithmName());
      writer.newLine();
      writer.write("Population/Swarm Size: " + config.baseLevelPopulationSize());
      writer.newLine();
      writer.write("Max Evaluations: " + config.baseLevelMaxEvaluations());
      writer.newLine();
      writer.write("Evaluation Strategy: " + config.evaluationBudgetStrategy());
      writer.newLine();
      writer.write("Parameter Space: " + config.yamlParameterSpaceFile());
      writer.newLine();
      writer.newLine();

      writer.write("--- Training Set ---");
      writer.newLine();
      writer.write("Problem Family: " + problemName);
      writer.newLine();
      writer.write("Problems: "
          + problem.problems().stream()
              .map(Problem::name)
              .collect(Collectors.joining(", ")));
      writer.newLine();
      writer.newLine();

      writer.write("--- Quality Indicators ---");
      writer.newLine();
      writer.write("Indicators: "
          + indicators.stream().map(QualityIndicator::name).collect(Collectors.joining(", ")));
      writer.newLine();
    } catch (IOException e) {
      throw new JMetalException("Error writing metadata", e);
    }
  }

  private void writeHeaders(String indicatorsFileName, String configurationsFileName)
      throws IOException {
    File indicatorsFile = new File(outputDirectoryName, indicatorsFileName);
    if (indicatorsFile.length() == 0 || !indicatorsFile.exists()) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(indicatorsFile, true))) {
        writer.write("Evaluation,SolutionId,"
            + indicators.stream().map(QualityIndicator::name).collect(Collectors.joining(",")));
        writer.newLine();
      }
    }

    File configurationsFile = new File(outputDirectoryName, configurationsFileName);
    if (configurationsFile.length() == 0 || !configurationsFile.exists()) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(configurationsFile, true))) {
        String parameterNames = problem.parameters().stream()
            .map(Parameter::name)
            .collect(Collectors.joining(","));
        writer.write("Evaluation,SolutionId," + parameterNames);
        writer.newLine();
      }
    }
  }

  private void writeIndicators(List<DerivationTreeSolution> solutions, String fileName)
      throws IOException {
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(new File(outputDirectoryName, fileName), true))) {
      for (int i = 0; i < solutions.size(); i++) {
        DerivationTreeSolution solution = solutions.get(i);
        StringBuilder line = new StringBuilder();
        line.append(evaluations).append(",").append(i);
        for (double objective : solution.objectives()) {
          line.append(",").append(objective);
        }
        writer.write(line.toString());
        writer.newLine();
      }
    }
  }

  private void writeConfigurations(List<DerivationTreeSolution> solutions, String fileName)
      throws IOException {
    List<Parameter<?>> parameters = problem.parameters();
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(new File(outputDirectoryName, fileName), true))) {
      for (int i = 0; i < solutions.size(); i++) {
        DerivationTreeSolution solution = solutions.get(i);
        List<Double> numericValues = ParameterManagement.decodeConfigArrayToDoubleValues(
            solution.toParameterArray(), parameters);
        StringBuilder line = new StringBuilder();
        line.append(evaluations).append(",").append(i);
        for (Double val : numericValues) {
          line.append(",").append(Double.isNaN(val) ? "NaN" : val);
        }
        writer.write(line.toString());
        writer.newLine();
      }
    }
  }

  private void writeVarConf(List<DerivationTreeSolution> solutions) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(new File(outputDirectoryName, "VAR_CONF.txt"), true))) {
      writer.write("# Evaluation: " + evaluations);
      writer.newLine();
      writer.write(ConsolidatedOutputResults.timeLine(checkpointMinutes()));
      writer.newLine();
      for (int i = 0; i < solutions.size(); i++) {
        DerivationTreeSolution solution = solutions.get(i);

        StringBuilder indicatorValues = new StringBuilder();
        for (int j = 0; j < indicators.size(); j++) {
          if (j > 0) {
            indicatorValues.append(" ");
          }
          indicatorValues.append(indicators.get(j).name())
              .append("=")
              .append(solution.objectives()[j]);
        }

        String parameterString = String.join(" ", solution.toParameterArray());
        writer.write(indicatorValues + " | " + parameterString);
        writer.newLine();
      }
      writer.newLine();
    }
  }

  /**
   * Returns the list of problems from the meta-optimization problem.
   *
   * @return the problems list
   */
  public List<? extends Problem<?>> problems() {
    return problem.problems();
  }
}
