package org.uma.evolver.cli.training;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Minimal CLI entry point for {@link TrainingRunner}: a single argument (path to a
 * {@link TrainingRequest} YAML file) replaces the ad hoc, hardcoded {@code main(String[] args)}
 * pattern used by the other {@code org.uma.evolver.example.training} classes.
 *
 * <p>Usage: {@code TrainingRunnerMain <request.yaml> [status.yaml]}
 *
 * <p>Study prototype — see {@link TrainingRunner} for scope notes.
 */
public class TrainingRunnerMain {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: TrainingRunnerMain <request.yaml> [status.yaml]");
      System.exit(1);
    }

    Path requestFile = Path.of(args[0]);
    Path statusFile = args.length > 1 ? Path.of(args[1]) : requestFile.resolveSibling("status.yaml");

    TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);
    Path outputDirectory = new TrainingRunner().run(request, statusFile);

    writeResultsPointer(requestFile.resolveSibling("results.yaml"), outputDirectory);

    // AsynchronousMultiThreadedNSGAII (metaSearch.algorithm: AsyncNSGA-II) leaves its
    // master/worker thread pool running after run() returns, so the JVM never exits on its own
    // — same reason org.uma.evolver.example.training's async examples end with System.exit(0).
    // Harmless for the other meta-optimizer algorithms, which already terminate naturally.
    System.exit(0);
  }

  private static void writeResultsPointer(Path resultsFile, Path outputDirectory) throws IOException {
    Map<String, Object> results = new LinkedHashMap<>();
    results.put("outputDirectory", outputDirectory.toString());
    results.put("metadataFile", outputDirectory.resolve("METADATA.txt").toString());
    results.put("indicatorsFile", outputDirectory.resolve("INDICATORS.csv").toString());
    results.put("configurationsFile", outputDirectory.resolve("CONFIGURATIONS.csv").toString());

    try (FileWriter writer = new FileWriter(resultsFile.toFile())) {
      new Yaml().dump(results, writer);
    }
  }
}
