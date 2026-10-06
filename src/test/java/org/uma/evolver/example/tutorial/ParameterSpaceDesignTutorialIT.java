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
 * Keeps the validation study of tutorial E5 ("Designing your own parameter space") from rotting:
 * runs {@link ParameterSpaceDesignTutorial} with minimal budgets, using the bundled configurations.
 */
@Tag("integration")
@DisplayName("Integration tests for class ParameterSpaceDesignTutorial")
class ParameterSpaceDesignTutorialIT {

  @Test
  @DisplayName(
      "given the bundled configuration and minimal budgets, when the study is run, then every"
          + " algorithm is run on every problem and the quality indicators are computed")
  void givenMinimalBudgets_whenRun_thenEveryAlgorithmAndProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    String fullSpace =
        new ConfigurationFileReader(ParameterSpaceDesignTutorial.FULL_SPACE_CONFIGURATION_FILE)
            .getConfiguration(1);
    String smallSpace =
        new ConfigurationFileReader(ParameterSpaceDesignTutorial.SMALL_SPACE_CONFIGURATION_FILE)
            .getConfiguration(1);

    // Act
    ParameterSpaceDesignTutorial.run(fullSpace, smallSpace, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("validation/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIWFG", "NSGAIIWFGSmall"}) {
      for (String problem : new String[] {"WFG1", "WFG9", "DTLZ1", "DTLZ7"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
    assertTrue(Files.exists(data.resolve("NSGAIIWFGSmall/DTLZ7/HV")));
    assertTrue(Files.exists(data.resolve("NSGAIIWFGSmall/DTLZ7/SP")));
  }
}
