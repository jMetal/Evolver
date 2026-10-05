package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.util.ConfigurationFileReader;
import org.yaml.snakeyaml.Yaml;

@DisplayName("Unit tests for class SolveRunner")
class SolveRunnerTest {

  @TempDir private Path tempDir;

  private SolveRequest nsgaiiOnZdt1(int runs, Long seed, String outputDirectory)
      throws IOException {
    return new SolveRequest(
        "NSGA-II",
        "Double",
        20,
        "NSGAIIDouble.yaml",
        Map.of(),
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1),
        null,
        new ProblemSpec("ZDT1"),
        "resources/referenceFronts/ZDT1.csv",
        1000,
        runs,
        seed,
        List.of("Epsilon", "NormalizedHypervolume"),
        null,
        null,
        false,
        outputDirectory);
  }

  @Nested
  @DisplayName("When running a valid request: ")
  class ValidRequestTestCases {

    @Test
    @DisplayName(
        "given NSGA-II on ZDT1 with two runs, when run, then each run has its fronts and a row"
            + " of indicators, and the status is FINISHED")
    void givenTwoRuns_whenRun_thenEachRunIsWritten() throws IOException {
      // Arrange
      Path outputDirectory = tempDir.resolve("output");
      Path statusFile = tempDir.resolve("status.yaml");
      SolveRequest request = nsgaiiOnZdt1(2, 1L, outputDirectory.toString());

      // Act
      new SolveRunner().run(request, statusFile);

      // Assert
      assertTrue(Files.exists(outputDirectory.resolve("run-1/VAR.csv")));
      assertTrue(Files.exists(outputDirectory.resolve("run-1/FUN.csv")));
      assertTrue(Files.exists(outputDirectory.resolve("run-2/FUN.csv")));
      assertTrue(Files.exists(outputDirectory.resolve("METADATA.txt")));
      List<String> indicators = Files.readAllLines(outputDirectory.resolve("INDICATORS.csv"));
      assertEquals("Run,Seed,TimeMs,EP,NHV", indicators.get(0));
      assertEquals(3, indicators.size());
      assertTrue(indicators.get(1).startsWith("1,1,"));
      assertTrue(indicators.get(2).startsWith("2,2,"));
      String status = Files.readString(statusFile);
      assertTrue(status.contains("FINISHED"));
      assertTrue(status.contains("evaluationsDone: 2000"));
    }

    @Test
    @DisplayName("given the same seed, when run twice, then the fronts are identical")
    void givenSameSeed_whenRunTwice_thenFrontsAreIdentical() throws IOException {
      // Arrange
      Path first = tempDir.resolve("first");
      Path second = tempDir.resolve("second");

      // Act
      new SolveRunner().run(nsgaiiOnZdt1(1, 7L, first.toString()), tempDir.resolve("s1.yaml"));
      new SolveRunner().run(nsgaiiOnZdt1(1, 7L, second.toString()), tempDir.resolve("s2.yaml"));

      // Assert
      assertEquals(
          Files.readString(first.resolve("run-1/FUN.csv")),
          Files.readString(second.resolve("run-1/FUN.csv")));
    }
  }

  @Nested
  @DisplayName("When running with a statusFrequency: ")
  class ProgressTestCases {

    @Test
    @DisplayName("given a statusFrequency, when run, then the status shows progress meanwhile")
    void givenStatusFrequency_whenRun_thenTheStatusShowsProgressMeanwhile() throws Exception {
      // Arrange: a run long enough to be seen in progress, polled from another thread
      SolveRequest valid = nsgaiiOnZdt1(1, 1L, tempDir.resolve("output").toString());
      int totalEvaluations = 40000;
      SolveRequest request =
          new SolveRequest(
              valid.algorithmName(),
              valid.encoding(),
              valid.populationSize(),
              valid.yamlParameterSpaceFile(),
              valid.extraConfig(),
              valid.configuration(),
              valid.configurationFile(),
              valid.problem(),
              valid.referenceFrontFileName(),
              totalEvaluations,
              valid.numberOfIndependentRuns(),
              valid.seed(),
              valid.indicatorNames(),
              500,
              valid.frontFrequency(),
              valid.writePopulation(),
              valid.outputDirectory());
      Path statusFile = tempDir.resolve("status.yaml");
      List<Integer> observed = Collections.synchronizedList(new ArrayList<>());
      AtomicBoolean finished = new AtomicBoolean(false);
      Thread poller =
          new Thread(
              () -> {
                while (!finished.get()) {
                  try {
                    Object status = new Yaml().load(Files.readString(statusFile));
                    if (status instanceof Map<?, ?> map && "RUNNING".equals(map.get("status"))) {
                      observed.add((Integer) map.get("evaluationsDone"));
                    }
                  } catch (Exception e) {
                    // the file does not exist yet, or is being rewritten: poll again
                  }
                }
              });

      // Act
      poller.start();
      new SolveRunner().run(request, statusFile);
      finished.set(true);
      poller.join();

      // Assert: intermediate values, between the start and the end, never going back
      List<Integer> inProgress =
          observed.stream().filter(done -> done > 0 && done < totalEvaluations).toList();
      assertFalse(inProgress.isEmpty(), "observed: " + observed);
      for (int i = 1; i < observed.size(); i++) {
        assertTrue(observed.get(i) >= observed.get(i - 1), "observed: " + observed);
      }
      assertTrue(Files.readString(statusFile).contains("FINISHED"));
    }
  }

  @Nested
  @DisplayName("When running with a frontFrequency: ")
  class FrontTestCases {

    @Test
    @DisplayName("given a frontFrequency, when run, then the current front is written meanwhile")
    void givenFrontFrequency_whenRun_thenTheCurrentFrontIsWrittenMeanwhile() throws Exception {
      // Arrange: a run long enough to be seen in progress, polled from another thread
      SolveRequest valid = nsgaiiOnZdt1(1, 1L, tempDir.resolve("output").toString());
      SolveRequest request =
          new SolveRequest(
              valid.algorithmName(),
              valid.encoding(),
              valid.populationSize(),
              valid.yamlParameterSpaceFile(),
              valid.extraConfig(),
              valid.configuration(),
              valid.configurationFile(),
              valid.problem(),
              valid.referenceFrontFileName(),
              40000,
              valid.numberOfIndependentRuns(),
              valid.seed(),
              valid.indicatorNames(),
              valid.statusFrequency(),
              500,
              valid.writePopulation(),
              valid.outputDirectory());
      Path frontFile = tempDir.resolve("output").resolve("CURRENT_FRONT.csv");
      List<String> headers = Collections.synchronizedList(new ArrayList<>());
      AtomicBoolean finished = new AtomicBoolean(false);
      Thread poller =
          new Thread(
              () -> {
                while (!finished.get()) {
                  try {
                    headers.add(Files.readAllLines(frontFile).get(0));
                  } catch (Exception e) {
                    // the file does not exist yet: poll again
                  }
                }
              });

      // Act
      poller.start();
      new SolveRunner().run(request, tempDir.resolve("status.yaml"));
      finished.set(true);
      poller.join();

      // Assert: seen while running, never half-written, and removed when the run is over
      assertFalse(headers.isEmpty());
      assertTrue(headers.stream().allMatch("Run,Evaluations,NonDominated,F1,F2"::equals), "read: " + headers);
      assertFalse(Files.exists(frontFile));
      assertTrue(Files.exists(tempDir.resolve("output").resolve("run-1").resolve("FUN.csv")));
    }
  }

  @Nested
  @DisplayName("When running an invalid request: ")
  class InvalidRequestTestCases {

    @Test
    @DisplayName("given an unknown algorithm, when run, then it fails and the status is FAILED")
    void givenUnknownAlgorithm_whenRun_thenStatusIsFailed() throws IOException {
      // Arrange
      SolveRequest valid = nsgaiiOnZdt1(1, 1L, tempDir.resolve("output").toString());
      SolveRequest request =
          new SolveRequest(
              "NotAnAlgorithm",
              valid.encoding(),
              valid.populationSize(),
              valid.yamlParameterSpaceFile(),
              valid.extraConfig(),
              valid.configuration(),
              valid.configurationFile(),
              valid.problem(),
              valid.referenceFrontFileName(),
              valid.maxEvaluations(),
              valid.numberOfIndependentRuns(),
              valid.seed(),
              valid.indicatorNames(),
              valid.statusFrequency(),
              valid.frontFrequency(),
              valid.writePopulation(),
              valid.outputDirectory());
      Path statusFile = tempDir.resolve("status.yaml");

      // Act & Assert
      assertThrows(RuntimeException.class, () -> new SolveRunner().run(request, statusFile));
      assertTrue(Files.readString(statusFile).contains("FAILED"));
    }

    @Test
    @DisplayName(
        "given a Double problem and the Permutation encoding, when run, then it fails before"
            + " running and says which encodings do not match")
    void givenAnEncodingMismatch_whenRun_thenStatusIsFailedAndSaysWhy() throws IOException {
      // Arrange
      SolveRequest valid = nsgaiiOnZdt1(1, 1L, tempDir.resolve("output").toString());
      SolveRequest request =
          new SolveRequest(
              valid.algorithmName(),
              "Permutation",
              valid.populationSize(),
              "NSGAIIPermutation.yaml",
              valid.extraConfig(),
              valid.configuration(),
              valid.configurationFile(),
              valid.problem(),
              valid.referenceFrontFileName(),
              valid.maxEvaluations(),
              valid.numberOfIndependentRuns(),
              valid.seed(),
              valid.indicatorNames(),
              valid.statusFrequency(),
              valid.frontFrequency(),
              valid.writePopulation(),
              valid.outputDirectory());
      Path statusFile = tempDir.resolve("status.yaml");

      // Act & Assert
      assertThrows(RuntimeException.class, () -> new SolveRunner().run(request, statusFile));
      String status = Files.readString(statusFile);
      assertTrue(status.contains("FAILED"), status);
      assertTrue(status.contains("Double-encoded"), status);
      assertTrue(status.contains("Permutation"), status);
    }

    @Test
    @DisplayName(
        "given Spread on a problem with three objectives, when run, then it fails before running")
    void givenSpreadOnThreeObjectives_whenRun_thenStatusIsFailed() throws IOException {
      // Arrange
      SolveRequest valid = nsgaiiOnZdt1(1, 1L, tempDir.resolve("output").toString());
      SolveRequest request =
          new SolveRequest(
              valid.algorithmName(),
              valid.encoding(),
              valid.populationSize(),
              valid.yamlParameterSpaceFile(),
              valid.extraConfig(),
              valid.configuration(),
              valid.configurationFile(),
              new ProblemSpec("DTLZ2"),
              "resources/referenceFronts/DTLZ2.3D.csv",
              valid.maxEvaluations(),
              valid.numberOfIndependentRuns(),
              valid.seed(),
              List.of("Spread"),
              valid.statusFrequency(),
              valid.frontFrequency(),
              valid.writePopulation(),
              valid.outputDirectory());
      Path statusFile = tempDir.resolve("status.yaml");

      // Act
      RuntimeException exception =
          assertThrows(RuntimeException.class, () -> new SolveRunner().run(request, statusFile));

      // Assert
      assertTrue(exception.getMessage().contains("bi-objective"));
      assertTrue(Files.readString(statusFile).contains("FAILED"));
    }
  }
}
