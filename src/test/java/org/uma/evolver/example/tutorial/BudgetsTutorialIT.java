package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.FlatMetaSearchConfig;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;

/**
 * Keeps tutorial E17 ("Budgets: evaluations or time") from rotting: runs {@link BudgetsTutorial} on
 * the tutorial's own configuration files, with minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class BudgetsTutorial")
class BudgetsTutorialIT {

  @Test
  @DisplayName(
      "given the tutorial's configuration files with minimal budgets, when the tutorial is run, then"
          + " the training stops by time and the tutorial reports it")
  void givenTutorialFilesWithMinimalBudgets_whenRun_thenTheTrainingStopsByTime(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    BaseLevelConfig base = BaseLevelConfigurationReader.load("TutorialZdt4BaseLevel.yaml");
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            base.algorithmName(),
            base.encoding(),
            10,
            base.numberOfIndependentRuns(),
            base.yamlParameterSpaceFile(),
            base.extraConfig(),
            base.trainingProblemNames(),
            base.trainingReferenceFrontFileNames(),
            List.of(300),
            base.indicatorNames());
    var meta =
        (FlatMetaSearchConfig)
            MetaOptimizerConfigurationReader.load("TutorialTimeNSGAIIMetaSearch.yaml");
    var metaSearch = new FlatMetaSearchConfig(meta.algorithm(), 0, 0.03, 4, 2, meta.operatorFlags());

    PrintStream standardOutput = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    System.setOut(new PrintStream(output));

    // Act
    try {
      BudgetsTutorial.run(baseLevel, metaSearch, tempDir.toString());
    } finally {
      System.setOut(standardOutput);
    }

    // Assert
    String printed = output.toString();
    assertTrue(printed.contains("Stopped after"), printed);
    assertTrue(printed.contains("Stopping condition: computing time"), printed);
    assertTrue(printed.contains("Meta-evaluations performed: "), printed);
    assertTrue(printed.contains("Meta-evaluation "), printed);
    assertTrue(Files.exists(tempDir.resolve("VAR_CONF.txt")));
  }
}
