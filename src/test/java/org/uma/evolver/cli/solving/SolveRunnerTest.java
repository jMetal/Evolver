package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.util.ConfigurationFileReader;

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
              valid.outputDirectory());
      Path statusFile = tempDir.resolve("status.yaml");

      // Act & Assert
      assertThrows(RuntimeException.class, () -> new SolveRunner().run(request, statusFile));
      assertTrue(Files.readString(statusFile).contains("FAILED"));
    }
  }
}
