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
 * Keeps the validation study of tutorial E8 ("Validating a configuration") from rotting: runs
 * {@link ValidationTutorial} with minimal budgets, using the bundled tuned configuration.
 */
@Tag("integration")
@DisplayName("Integration tests for class ValidationTutorial")
class ValidationTutorialIT {

  @Test
  @DisplayName(
      "given the bundled configuration and minimal budgets, when the study is run, then every"
          + " algorithm is run on every problem and the quality indicators are computed")
  void givenMinimalBudgets_whenRun_thenEveryAlgorithmAndProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader(ValidationTutorial.TUNED_CONFIGURATION_FILE)
            .getConfiguration(1);

    // Act
    ValidationTutorial.run(configuration, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("validation/data");
    for (String algorithm : new String[] {"NSGAII", "SMPSO", "NSGAIIWFG"}) {
      for (String problem : new String[] {"WFG1", "WFG9", "DTLZ1", "DTLZ7"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
    assertTrue(Files.exists(data.resolve("NSGAIIWFG/DTLZ7/HV")));
    assertTrue(Files.exists(data.resolve("NSGAIIWFG/DTLZ7/SP")));
  }
}
