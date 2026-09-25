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
 * Keeps tutorial E3 ("Meta-optimization workflow") from rotting: runs {@link
 * MetaOptimizationWorkflowTutorial} on the tutorial's own configuration files, with minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class MetaOptimizationWorkflowTutorial")
class MetaOptimizationWorkflowTutorialIT {

  @Test
  @DisplayName(
      "given the tutorial's configuration files with minimal budgets, when the tutorial is run, then"
          + " it chooses a configuration and writes the fronts of both configurations")
  void givenTutorialFilesWithMinimalBudgets_whenRun_thenBothFrontsAreWritten(@TempDir Path tempDir)
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
        (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load("TutorialNSGAIIMetaSearch.yaml");
    var metaSearch = new FlatMetaSearchConfig(meta.algorithm(), 12, 4, 2, meta.operatorFlags());

    PrintStream standardOutput = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    System.setOut(new PrintStream(output));

    // Act
    try {
      MetaOptimizationWorkflowTutorial.run(baseLevel, metaSearch, tempDir.toString());
    } finally {
      System.setOut(standardOutput);
    }

    // Assert
    String printed = output.toString();
    assertTrue(printed.contains("Training finished in"), printed);
    assertTrue(printed.contains("Chosen (lowest NHV): --algorithmResult"), printed);
    assertTrue(printed.contains("Chosen configuration on ZDT4:  EP = "), printed);
    assertTrue(Files.exists(tempDir.resolve("VAR_CONF.txt")));
    assertTrue(Files.exists(tempDir.resolve("validation/default/FUN.csv")));
    assertTrue(Files.exists(tempDir.resolve("validation/tuned/FUN.csv")));
  }
}
