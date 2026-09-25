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

@DisplayName("Unit tests for class MetaOptimizationWorkflowTutorial")
class MetaOptimizationWorkflowTutorialTest {

  @Nested
  @DisplayName("When reading the final front of a training: ")
  class FinalConfigurationsTestCases {

    @Test
    @DisplayName(
        "given a VAR_CONF.txt with two checkpoints, when read, then only the configurations of the"
            + " last one are returned, with their indicator values")
    void givenTwoCheckpoints_whenRead_thenOnlyTheLastOneIsReturned(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path varConf = tempDir.resolve("VAR_CONF.txt");
      Files.writeString(
          varConf,
          """
          # Evaluation: 50
          EP=0.9 NHV=1.0 | --crossover SBX

          # Evaluation: 100
          EP=0.2 NHV=0.3 | --crossover blxAlpha
          EP=0.1 NHV=0.4 | --crossover PCX

          """);

      // Act
      List<TunedConfiguration> configurations =
          MetaOptimizationWorkflowTutorial.finalConfigurations(varConf);

      // Assert
      assertEquals(
          List.of(
              new TunedConfiguration(0.2, 0.3, "--crossover blxAlpha"),
              new TunedConfiguration(0.1, 0.4, "--crossover PCX")),
          configurations);
    }
  }
}
