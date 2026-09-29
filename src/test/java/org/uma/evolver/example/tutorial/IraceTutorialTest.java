package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class IraceTutorial")
class IraceTutorialTest {

  @Nested
  @DisplayName("When reading the best configuration from irace's output: ")
  class BestConfigurationTestCases {

    @Test
    @DisplayName(
        "given an irace output with its final list of best configurations, when read, then the"
            + " first one is returned without its identifier")
    void givenIraceOutput_whenRead_thenFirstBestConfigurationIsReturned(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path output = tempDir.resolve("irace.stdout.txt");
      Files.writeString(
          output,
          """
          # Best configurations as commandlines (first number is the configuration ID; listed \
          from best to worst according to the sum of ranks):
          12  --crossover SBX --crossoverProbability 0.5
          # Elite configurations
          # Best configurations as commandlines (first number is the configuration ID; listed \
          from best to worst according to the sum of ranks):
          106 --crossover PCX --crossoverProbability 0.9
          87  --crossover SDX --crossoverProbability 0.1

          """);

      // Act
      String configuration = IraceTutorial.bestConfiguration(output);

      // Assert
      assertEquals("--crossover PCX --crossoverProbability 0.9", configuration);
    }

    @Test
    @DisplayName("given an irace output without best configurations, when read, then it fails")
    void givenIraceOutputWithoutBestConfigurations_whenRead_thenItFails(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path output = tempDir.resolve("irace.stdout.txt");
      Files.writeString(output, "# irace is still running\n");

      // Act & Assert
      assertThrows(JMetalException.class, () -> IraceTutorial.bestConfiguration(output));
    }
  }
}
