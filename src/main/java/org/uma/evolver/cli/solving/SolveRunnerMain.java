package org.uma.evolver.cli.solving;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * CLI entry point for {@link SolveRunner}: runs a configurable algorithm on a problem, as
 * described by a {@link SolveRequest} YAML file.
 *
 * <p>Usage: {@code SolveRunnerMain <request.yaml> [status.yaml]}
 *
 * <p>As {@code TrainingRunnerMain} does, it writes the progress to {@code status.yaml} (next to the
 * request unless given) and, on success, a {@code results.yaml} next to the request pointing at the
 * output files.
 */
public class SolveRunnerMain {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: SolveRunnerMain <request.yaml> [status.yaml]");
      System.exit(1);
    }

    Path requestFile = Path.of(args[0]);
    Path statusFile =
        args.length > 1 ? Path.of(args[1]) : requestFile.resolveSibling("status.yaml");

    SolveRequest request = SolveRequestYamlLoader.load(requestFile);
    Path outputDirectory = new SolveRunner().run(request, statusFile);

    writeResultsPointer(requestFile.resolveSibling("results.yaml"), outputDirectory, request);
  }

  private static void writeResultsPointer(
      Path resultsFile, Path outputDirectory, SolveRequest request) throws IOException {
    Map<String, Object> results = new LinkedHashMap<>();
    results.put("outputDirectory", outputDirectory.toString());
    results.put("metadataFile", outputDirectory.resolve("METADATA.txt").toString());
    results.put("indicatorsFile", outputDirectory.resolve("INDICATORS.csv").toString());
    results.put("numberOfIndependentRuns", request.numberOfIndependentRuns());
    results.put("runDirectoryPattern", outputDirectory.resolve("run-<i>").toString());

    try (FileWriter writer = new FileWriter(resultsFile.toFile())) {
      new Yaml().dump(results, writer);
    }
  }
}
