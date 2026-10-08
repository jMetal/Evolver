package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The command line of {@link TrainingRunnerMain}, with a tiny training. */
@Tag("integration")
@DisplayName("Integration tests for class TrainingRunnerMain")
class TrainingRunnerMainIT {

  @TempDir Path tempDir;

  /** A request of a tiny training (NSGA-II on ZDT4, 20 configurations of 500 evaluations). */
  private Path writeTinyRequest() throws IOException {
    Path baseLevel = tempDir.resolve("base-level.yaml");
    Files.writeString(
        baseLevel,
        """
        algorithmName: NSGA-II
        populationSize: 20
        numberOfIndependentRuns: 1
        yamlParameterSpaceFile: NSGAIIDouble.yaml
        trainingProblemNames: [ZDT4]
        trainingReferenceFrontFileNames: [resources/referenceFronts/ZDT4.csv]
        trainingEvaluations: [500]
        indicatorNames: [Epsilon, NormalizedHypervolume]
        """);
    Path metaSearch = tempDir.resolve("meta-search.yaml");
    Files.writeString(
        metaSearch,
        """
        algorithm: NSGA-II
        encoding: flat
        metaMaxEvaluations: 20
        metaPopulationSize: 10
        numberOfCores: 2
        crossover: SBX
        crossoverProbability: 0.9
        crossoverRepairStrategy: bounds
        sbxDistributionIndex: 20.0
        mutation: polynomial
        mutationProbabilityFactor: 1.0
        mutationRepairStrategy: bounds
        polynomialMutationDistributionIndex: 20.0
        selection: tournament
        selectionTournamentSize: 2
        """);
    Path request = tempDir.resolve("request.yaml");
    Files.writeString(
        request,
        "baseLevel: "
            + baseLevel
            + "\nmetaSearch: "
            + metaSearch
            + "\noutputDirectory: "
            + tempDir.resolve("default-output")
            + "\nwriteFrequency: 10\nstatusFrequency: 10\n");
    return request;
  }

  @Nested
  @DisplayName("Given --output-dir")
  class OutputDirectory {

    @Test
    @DisplayName(
        "when two replications share one request with different output directories, then each"
            + " writes its results, status and results pointer to its own directory")
    void whenTwoReplicationsShareOneRequest_thenEachWritesToItsOwnDirectory() throws IOException {
      // Arrange
      Path request = writeTinyRequest();
      Path run01 = tempDir.resolve("study/run01");
      Path run02 = tempDir.resolve("study/run02");

      // Act
      Path output01 =
          TrainingRunnerMain.execute(
              new String[] {request.toString(), "--output-dir", run01.toString()});
      Path output02 =
          TrainingRunnerMain.execute(
              new String[] {request.toString(), "--output-dir", run02.toString()});

      // Assert
      assertEquals(run01, output01);
      assertEquals(run02, output02);
      for (Path run : new Path[] {run01, run02}) {
        assertTrue(Files.exists(run.resolve("INDICATORS.csv")), run + "/INDICATORS.csv");
        assertTrue(Files.exists(run.resolve("CONFIGURATIONS.csv")), run + "/CONFIGURATIONS.csv");
        assertTrue(Files.exists(run.resolve("status.yaml")), run + "/status.yaml");
        assertTrue(Files.readString(run.resolve("results.yaml")).contains(run.toString()));
      }
      assertFalse(Files.exists(tempDir.resolve("default-output")));
      assertFalse(Files.exists(tempDir.resolve("results.yaml")));
    }
  }

  @Nested
  @DisplayName("Given wrong arguments")
  class WrongArguments {

    @Test
    @DisplayName("when there is no request file, then it fails with the usage")
    void whenNoRequestFile_thenItFailsWithTheUsage() {
      // Arrange & Act & Assert
      var exception =
          assertThrows(
              IllegalArgumentException.class, () -> TrainingRunnerMain.execute(new String[] {}));
      assertTrue(exception.getMessage().contains("Usage"));
    }

    @Test
    @DisplayName("when --output-dir has no value, then it fails")
    void whenOutputDirHasNoValue_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> TrainingRunnerMain.execute(new String[] {"request.yaml", "--output-dir"}));
    }

    @Test
    @DisplayName("when an option is unknown, then it fails naming it")
    void whenOptionIsUnknown_thenItFailsNamingIt() {
      // Arrange & Act & Assert
      var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TrainingRunnerMain.execute(new String[] {"request.yaml", "--runs", "3"}));
      assertTrue(exception.getMessage().contains("--runs"));
    }
  }
}
