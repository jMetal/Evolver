package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.example.tutorial.BudgetsTutorial.Checkpoint;

@DisplayName("Unit tests for class BudgetsTutorial")
class BudgetsTutorialTest {

  @Nested
  @DisplayName("When reading the checkpoints of a training: ")
  class CheckpointsTestCases {

    @Test
    @DisplayName(
        "given a VAR_CONF.txt with two checkpoints, when read, then the evaluations and the minutes"
            + " of each one are returned")
    void givenTwoCheckpoints_whenRead_thenTheirEvaluationsAndMinutesAreReturned(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path varConf = tempDir.resolve("VAR_CONF.txt");
      Files.writeString(
          varConf,
          """
          # Evaluation: 50
          # Time (min): 0.125
          EP=0.9 NHV=1.0 | --crossover SBX

          # Evaluation: 100
          # Time (min): 0.250
          EP=0.2 NHV=0.3 | --crossover blxAlpha

          """);

      // Act
      List<Checkpoint> checkpoints = BudgetsTutorial.checkpoints(varConf);

      // Assert
      assertEquals(List.of(new Checkpoint(50, 0.125), new Checkpoint(100, 0.25)), checkpoints);
    }

    @Test
    @DisplayName("given a VAR_CONF.txt of an older version without times, when read, then it is empty")
    void givenCheckpointsWithoutTimes_whenRead_thenNothingIsReturned(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path varConf = tempDir.resolve("VAR_CONF.txt");
      Files.writeString(varConf, "# Evaluation: 50\nEP=0.9 NHV=1.0 | --crossover SBX\n");

      // Act & Assert
      assertTrue(BudgetsTutorial.checkpoints(varConf).isEmpty());
    }
  }

  @Nested
  @DisplayName("When reading the status of a training: ")
  class StatusTestCases {

    @Test
    @DisplayName("given a status file of a run bounded by time, when read, then its fields are returned")
    void givenStatusFile_whenRead_thenItsFieldsAreReturned(@TempDir Path tempDir) throws IOException {
      // Arrange
      Path statusFile = tempDir.resolve("status.yaml");
      Files.writeString(
          statusFile,
          "{status: FINISHED, evaluationsDone: 800, maxEvaluations: 0,"
              + " maxComputingTimeMinutes: 2.0, elapsedMinutes: 2.07}\n");

      // Act
      Map<String, Object> status = BudgetsTutorial.status(statusFile);

      // Assert
      assertEquals("FINISHED", status.get("status"));
      assertEquals(800, status.get("evaluationsDone"));
      assertEquals(2.07, ((Number) status.get("elapsedMinutes")).doubleValue());
    }
  }

  @Nested
  @DisplayName("When loading the tutorial's meta-optimizer: ")
  class MetaSearchTestCases {

    @Test
    @DisplayName(
        "given TutorialTimeNSGAIIMetaSearch.yaml, when loaded, then it is bounded by 2 minutes and"
            + " not by evaluations")
    void givenTimeConfiguration_whenLoaded_thenItIsBoundedByTime() {
      // Act
      var metaSearch = MetaOptimizerConfigurationReader.load("TutorialTimeNSGAIIMetaSearch.yaml");

      // Assert
      assertTrue(metaSearch.boundedByComputingTime());
      assertEquals(2.0, metaSearch.metaMaxComputingTimeMinutes());
      assertEquals(0, metaSearch.metaMaxEvaluations());
    }
  }
}
