package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.cli.solving.SolveRequest;
import org.uma.evolver.cli.solving.SolveRequestYamlLoader;
import org.uma.evolver.cli.solving.SolveRunner;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;

/**
 * Code of tutorial E17, "Extending Evolver" (see {@code docs/tutorials/extending_evolver.rst}): a
 * problem of the user, {@link BiSphere}, that Evolver does not know and that its requests refer to
 * by class name.
 *
 * <p>With no arguments it writes the reference front of the problem and runs NSGA-II with its
 * default configuration on it (request {@code tutorial-extending-request.yaml}); with {@code train}
 * it also tunes NSGA-II for it for two minutes (the request of {@code
 * cli/training} runs the same training). The comments {@code //
 * [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the tutorial page includes;
 * keep them when editing this class. Run it from the root of the repository.
 */
public class ExtendingTutorial {

  static final int NUMBER_OF_VARIABLES = 20;
  static final int POINTS_OF_THE_FRONT = 100;
  static final String REFERENCE_FRONT = "results/tutorial-extending/BiSphere.csv";
  static final String SOLVE_REQUEST = "src/main/resources/cli/solving/tutorial-extending-request.yaml";
  static final String TRAINING_OUTPUT_DIRECTORY = "results/tutorial-extending/training";

  private ExtendingTutorial() {}

  public static void main(String[] args) throws IOException {
    writeReferenceFront(Path.of(REFERENCE_FRONT));
    solve(Path.of(SOLVE_REQUEST));
    if (args.length > 0 && args[0].equals("train")) {
      train(TRAINING_OUTPUT_DIRECTORY);
    }
  }

  /** Writes the Pareto front of {@link BiSphere}, which is known, as a CSV file without header. */
  static void writeReferenceFront(Path file) throws IOException {
    // [step-1-start]
    Files.createDirectories(file.getParent());
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      for (double[] point : BiSphere.referenceFront(NUMBER_OF_VARIABLES, POINTS_OF_THE_FRONT)) {
        writer.println(point[0] + "," + point[1]);
      }
    }
    // [step-1-end]
  }

  /** Runs the solve request and prints the median of each indicator over its runs. */
  static void solve(Path requestFile) throws IOException {
    // [step-2-start]
    SolveRequest request = SolveRequestYamlLoader.load(requestFile);
    Path output = new SolveRunner().run(request, Path.of(request.outputDirectory(), "status.yaml"));
    // [step-2-end]
    List<String> lines = Files.readAllLines(output.resolve("INDICATORS.csv"));
    String[] header = lines.get(0).split(",");
    for (int column = 3; column < header.length; column++) {
      double[] values = new double[lines.size() - 1];
      for (int row = 1; row < lines.size(); row++) {
        values[row - 1] = Double.parseDouble(lines.get(row).split(",")[column]);
      }
      System.out.printf(
          Locale.ROOT,
          "%s: median %.5f over %d runs%n",
          header[column],
          median(values),
          values.length);
    }
  }

  /** Tunes NSGA-II for {@link BiSphere} for two minutes (the request of {@code cli/training}). */
  static void train(String outputDirectory) throws IOException {
    // [step-3-start]
    var baseLevel = BaseLevelConfigurationReader.load("TutorialBiSphereBaseLevel.yaml");
    var metaSearch = MetaOptimizerConfigurationReader.load("TutorialTimeNSGAIIMetaSearch.yaml");
    var request = new TrainingRequest(baseLevel, metaSearch, outputDirectory, 50, 50, null);
    new TrainingRunner().run(request, Path.of(outputDirectory, "status.yaml"));
    // [step-3-end]
  }

  static double median(double[] values) {
    double[] sorted = values.clone();
    java.util.Arrays.sort(sorted);
    int middle = sorted.length / 2;
    return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
  }
}
