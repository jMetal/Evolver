package org.uma.evolver.cli.training;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Minimal CLI entry point for {@link TrainingRunner}: a single argument (path to a
 * {@link TrainingRequest} YAML file) replaces the ad hoc, hardcoded {@code main(String[] args)}
 * pattern used by the other {@code org.uma.evolver.example.training} classes.
 *
 * <p>Usage: {@code TrainingRunnerMain <request.yaml> [status.yaml] [--output-dir dir]}
 *
 * <p>By default the results go to the request's {@code outputDirectory}, and {@code status.yaml}
 * and {@code results.yaml} next to the request file. With {@code --output-dir}, the results go to
 * that directory instead, and so do {@code status.yaml} (unless given) and {@code results.yaml}:
 * the independent replications of a training then share one request file, each with its own
 * directory ({@code results/<study>/run01}, {@code run02}, ...).
 *
 * <p>Study prototype — see {@link TrainingRunner} for scope notes.
 */
public class TrainingRunnerMain {

  static final String USAGE = "Usage: TrainingRunnerMain <request.yaml> [status.yaml] [--output-dir dir]";

  public static void main(String[] args) throws IOException {
    Path outputDirectory;
    try {
      outputDirectory = execute(args);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }

    // AsynchronousMultiThreadedNSGAII (metaSearch.algorithm: AsyncNSGA-II) leaves its
    // master/worker thread pool running after run() returns, so the JVM never exits on its own
    // — same reason org.uma.evolver.example.training's async examples end with System.exit(0).
    // Harmless for the other meta-optimizer algorithms, which already terminate naturally.
    System.exit(outputDirectory == null ? 1 : 0);
  }

  /**
   * Runs the training of the arguments and returns its output directory.
   *
   * @throws IllegalArgumentException if the arguments do not follow {@link #USAGE}
   */
  static Path execute(String[] args) throws IOException {
    String requestArgument = null;
    String statusArgument = null;
    String outputDirectoryArgument = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--output-dir")) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException("--output-dir needs a directory\n" + USAGE);
        }
        outputDirectoryArgument = args[++i];
      } else if (args[i].startsWith("--")) {
        throw new IllegalArgumentException("Unknown option: " + args[i] + "\n" + USAGE);
      } else if (requestArgument == null) {
        requestArgument = args[i];
      } else if (statusArgument == null) {
        statusArgument = args[i];
      } else {
        throw new IllegalArgumentException("Too many arguments\n" + USAGE);
      }
    }
    if (requestArgument == null) {
      throw new IllegalArgumentException(USAGE);
    }

    Path requestFile = Path.of(requestArgument);
    TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);
    Path pointerDirectory = requestFile.toAbsolutePath().getParent();
    if (outputDirectoryArgument != null) {
      request = request.withOutputDirectory(outputDirectoryArgument);
      pointerDirectory = Path.of(outputDirectoryArgument);
      Files.createDirectories(pointerDirectory);
    }
    Path statusFile =
        statusArgument != null ? Path.of(statusArgument) : pointerDirectory.resolve("status.yaml");

    Path outputDirectory = new TrainingRunner().run(request, statusFile);

    writeResultsPointer(
        pointerDirectory.resolve("results.yaml"), outputDirectory, request.writePopulation());
    return outputDirectory;
  }

  private static void writeResultsPointer(
      Path resultsFile, Path outputDirectory, boolean writePopulation) throws IOException {
    Map<String, Object> results = new LinkedHashMap<>();
    results.put("outputDirectory", outputDirectory.toString());
    results.put("metadataFile", outputDirectory.resolve("METADATA.txt").toString());
    results.put("indicatorsFile", outputDirectory.resolve("INDICATORS.csv").toString());
    results.put("configurationsFile", outputDirectory.resolve("CONFIGURATIONS.csv").toString());
    if (writePopulation) {
      results.put(
          "populationIndicatorsFile",
          outputDirectory.resolve("POPULATION_INDICATORS.csv").toString());
      results.put(
          "populationConfigurationsFile",
          outputDirectory.resolve("POPULATION_CONFIGURATIONS.csv").toString());
    }

    try (FileWriter writer = new FileWriter(resultsFile.toFile())) {
      new Yaml().dump(results, writer);
    }
  }
}
