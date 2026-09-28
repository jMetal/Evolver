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
 * Keeps the validation study of tutorial E7 ("Training sets, indicators and budgets") from
 * rotting: runs {@link TrainingSetsValidationTutorial} with minimal budgets, using the default
 * NSGA-II configuration in place of the tuned one.
 */
@Tag("integration")
@DisplayName("Integration tests for class TrainingSetsValidationTutorial")
class TrainingSetsValidationTutorialIT {

  @Test
  @DisplayName(
      "given a configuration and minimal budgets, when the study is run, then every algorithm is"
          + " run on every problem and the quality indicators are computed")
  void givenMinimalBudgets_whenRun_thenEveryAlgorithmAndProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    // Act
    TrainingSetsValidationTutorial.run(configuration, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("validation/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIDTLZ", "NSGAIII", "SMSEMOA", "AGEMOEA"}) {
      for (String problem : new String[] {"DTLZ1", "DTLZ7", "WFG1", "WFG9"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
    assertTrue(Files.exists(data.resolve("NSGAIIDTLZ/WFG9/HV")));
  }
}
