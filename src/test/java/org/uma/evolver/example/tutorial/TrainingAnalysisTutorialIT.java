package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.example.tutorial.MetaOptimizationWorkflowTutorial.TunedConfiguration;
import org.uma.evolver.util.ConfigurationFileReader;

/**
 * Keeps the validation of tutorial E8 ("Analyzing training results") from rotting: runs {@link
 * TrainingAnalysisTutorial#validate} with two candidates and minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class TrainingAnalysisTutorial")
class TrainingAnalysisTutorialIT {

  @Test
  @DisplayName(
      "given two candidates and minimal budgets, when validated, then the standard NSGA-II and"
          + " both candidates are run on every ZDT problem")
  void givenTwoCandidates_whenValidated_thenEveryAlgorithmAndProblemIsCovered(
      @TempDir Path tempDir) throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);
    List<TunedConfiguration> candidates =
        List.of(
            new TunedConfiguration(0.1, 0.2, configuration),
            new TunedConfiguration(0.2, 0.1, configuration));

    // Act
    TrainingAnalysisTutorial.validate(candidates, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("validation/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIZDT1", "NSGAIIZDT2"}) {
      for (String problem : new String[] {"ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
  }
}
