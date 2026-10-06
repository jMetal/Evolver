package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;
import org.yaml.snakeyaml.Yaml;

/**
 * Code of tutorial E11, "Budgets: evaluations or time" (see {@code docs/tutorials/budgets.rst}).
 *
 * <p>It runs the training of tutorial E3 (NSGA-II tuned for ZDT4 by NSGA-II) with the meta-optimizer
 * bounded by computing time instead of by meta-evaluations, and reads how it stopped: the status
 * file, the stopping condition recorded in {@code METADATA.txt}, and the computing time of each
 * checkpoint in {@code VAR_CONF.txt}.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class BudgetsTutorial {

  /** A checkpoint of {@code VAR_CONF.txt}: the meta-evaluations done and the minutes elapsed. */
  public record Checkpoint(int evaluations, double minutes) {}

  private BudgetsTutorial() {}

  public static void main(String[] args) throws IOException {
    // [step-1-start]
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.load("TutorialZdt4BaseLevel.yaml");
    MetaSearchConfig byEvaluations =
        MetaOptimizerConfigurationReader.load("TutorialNSGAIIMetaSearch.yaml");
    MetaSearchConfig byTime =
        MetaOptimizerConfigurationReader.load("TutorialTimeNSGAIIMetaSearch.yaml");

    System.out.println("Budget of the base level: " + baseLevel.trainingEvaluations() + " evaluations");
    System.out.println(
        "Budget of the meta-optimizer, by evaluations: "
            + byEvaluations.metaMaxEvaluations()
            + " configurations");
    System.out.println(
        "Budget of the meta-optimizer, by time: "
            + byTime.metaMaxComputingTimeMinutes()
            + " minutes");
    // [step-1-end]

    run(baseLevel, byTime, "results/tutorial/budgets");
  }

  /**
   * Runs the training and reads how it stopped.
   *
   * @param baseLevel the base level: the algorithm to tune, its training problem and indicators
   * @param metaSearch the meta-optimizer, bounded by computing time
   * @param outputDirectory where the training writes its results
   */
  public static void run(BaseLevelConfig baseLevel, MetaSearchConfig metaSearch, String outputDirectory)
      throws IOException {
    // [step-2-start]
    var request = new TrainingRequest(baseLevel, metaSearch, outputDirectory, 50, 50, null);
    Path results = new TrainingRunner().run(request, Path.of(outputDirectory, "status.yaml"));
    // [step-2-end]

    // [step-3-start]
    Map<String, Object> status = status(Path.of(outputDirectory, "status.yaml"));
    System.out.printf(
        Locale.ROOT,
        "Stopped after %.2f minutes (limit: %s), with %s meta-evaluations done%n",
        ((Number) status.get("elapsedMinutes")).doubleValue(),
        status.get("maxComputingTimeMinutes"),
        status.get("evaluationsDone"));

    for (String line : Files.readAllLines(results.resolve("METADATA.txt"))) {
      if (line.startsWith("Max Computing Time")
          || line.startsWith("Stopping condition")
          || line.startsWith("Meta-evaluations performed")) {
        System.out.println(line);
      }
    }
    // [step-3-end]

    // [step-4-start]
    for (Checkpoint checkpoint : checkpoints(results.resolve("VAR_CONF.txt"))) {
      System.out.printf(
          Locale.ROOT,
          "Meta-evaluation %4d at minute %.2f%n",
          checkpoint.evaluations(),
          checkpoint.minutes());
    }
    // [step-4-end]
  }

  /** Reads the status file written by the training. */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> status(Path statusFile) throws IOException {
    return (Map<String, Object>) new Yaml().load(Files.readString(statusFile));
  }

  /**
   * Reads the checkpoints of a training's {@code VAR_CONF.txt} file: each one starts with {@code #
   * Evaluation: <n>} and is followed by {@code # Time (min): <minutes>}.
   *
   * @param varConfFile the {@code VAR_CONF.txt} file written by the training
   * @return the checkpoints, in order
   */
  public static List<Checkpoint> checkpoints(Path varConfFile) throws IOException {
    List<Checkpoint> checkpoints = new ArrayList<>();
    int evaluations = -1;
    for (String line : Files.readAllLines(varConfFile)) {
      if (line.startsWith("# Evaluation: ")) {
        evaluations = Integer.parseInt(line.substring("# Evaluation: ".length()).trim());
      } else if (line.startsWith("# Time (min): ") && evaluations >= 0) {
        checkpoints.add(
            new Checkpoint(
                evaluations,
                Double.parseDouble(line.substring("# Time (min): ".length()).trim())));
        evaluations = -1;
      }
    }
    return checkpoints;
  }
}
