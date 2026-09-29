package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.example.tutorial.MetaOptimizationWorkflowTutorial.TunedConfiguration;

@DisplayName("Unit tests for class TrainingAnalysisTutorial")
class TrainingAnalysisTutorialTest {

  @Nested
  @DisplayName("When reading the candidates of a training run: ")
  class CandidatesTestCases {

    @Test
    @DisplayName(
        "given a final front with three configurations, when read, then they are ordered by NHV"
            + " and then by EP")
    void givenFinalFront_whenRead_thenOrderedByNhvAndEp(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path varConf = tempDir.resolve("VAR_CONF.txt");
      Files.writeString(
          varConf,
          """
          # Evaluation: 100
          EP=0.9 NHV=0.9 | --crossover SBX

          # Evaluation: 200
          EP=0.1 NHV=0.4 | --crossover PCX
          EP=0.3 NHV=0.2 | --crossover blxAlpha
          EP=0.2 NHV=0.2 | --crossover SDX

          """);

      // Act
      List<TunedConfiguration> candidates = TrainingAnalysisTutorial.candidates(varConf);

      // Assert
      assertEquals(
          List.of("--crossover SDX", "--crossover blxAlpha", "--crossover PCX"),
          candidates.stream().map(TunedConfiguration::configuration).toList());
    }
  }
}
