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
 * Keeps the validation studies of tutorial E10 ("Binary and permutation encodings") from rotting:
 * runs {@link EncodingsTutorial} with minimal budgets, using the default configurations in place
 * of the tuned ones.
 */
@Tag("integration")
@DisplayName("Integration tests for class EncodingsTutorial")
class EncodingsTutorialIT {

  @Test
  @DisplayName(
      "given minimal budgets, when the binary study is run, then both algorithms are run on ZDT5"
          + " and the quality indicators are computed")
  void givenMinimalBudgets_whenTheBinaryStudyIsRun_thenZdt5IsCovered(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIBinaryDefault.txt")
            .getConfiguration(1);

    // Act
    EncodingsTutorial.runBinary(configuration, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("binary/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIZDT5"}) {
      assertTrue(Files.exists(data.resolve(algorithm).resolve("ZDT5").resolve("FUN1.csv")));
    }
    assertTrue(Files.exists(data.resolve("NSGAIIZDT5/ZDT5/HV")));
  }

  @Test
  @DisplayName(
      "given minimal budgets, when the permutation study is run, then both algorithms are run on"
          + " the four TSP instances, a reference front is built and the indicators are computed")
  void givenMinimalBudgets_whenThePermutationStudyIsRun_thenEveryInstanceIsCovered(
      @TempDir Path tempDir) throws IOException {
    // Arrange
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIPermutationDefault.txt")
            .getConfiguration(1);

    // Act
    EncodingsTutorial.runPermutation(configuration, tempDir.toString(), 2, 500, 4);

    // Assert
    Path data = tempDir.resolve("permutation/data");
    for (String algorithm : new String[] {"NSGAII", "NSGAIIKroTSP"}) {
      for (String problem : new String[] {"KroAB100TSP", "KroAE100TSP"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
    assertTrue(Files.exists(data.resolve("NSGAIIKroTSP/KroAE100TSP/HV")));
    assertTrue(Files.exists(tempDir.resolve("permutation/referenceFronts/KroAE100TSP.csv")));
  }
}
