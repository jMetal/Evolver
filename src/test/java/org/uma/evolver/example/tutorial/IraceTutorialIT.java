package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.util.ConfigurationFileReader;

/**
 * Keeps tutorial E15 ("Tuning with irace") from rotting: applies a configuration with {@link
 * IraceTutorial#apply} with minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class IraceTutorial")
class IraceTutorialIT {

  @Test
  @DisplayName(
      "given a configuration and minimal budgets, when applied, then it and the standard NSGA-II"
          + " are run on every ZDT problem")
  void givenConfiguration_whenApplied_thenEveryProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    // Act
    IraceTutorial.apply(configuration, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("validation/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIIrace"}) {
      for (String problem : new String[] {"ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
  }
}
