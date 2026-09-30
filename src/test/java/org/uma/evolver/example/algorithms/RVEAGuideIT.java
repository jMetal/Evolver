package org.uma.evolver.example.algorithms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Keeps the algorithm guide of RVEA ({@code docs/algorithms/rvea.rst}) from rotting: runs its first
 * step, and its comparison with minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class RVEAGuide")
class RVEAGuideIT {

  @Test
  @DisplayName("given DTLZ2, when RVEA runs with its default configuration, then it returns a front")
  void givenDtlz2_whenRunWithTheDefaultConfiguration_thenAFrontIsReturned() throws IOException {
    // Act & Assert
    assertFalse(RVEAGuide.runOnDTLZ2().isEmpty());
  }

  @Test
  @DisplayName(
      "given minimal budgets, when the comparison runs, then every algorithm is run on every"
          + " problem")
  void givenMinimalBudgets_whenCompared_thenEveryProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Act
    RVEAGuide.compare(tempDir.toString(), 2, 1000, 1000, 4);

    // Assert
    Path data = tempDir.resolve("comparison/data");
    for (String algorithm : new String[] {"RVEA", "RVEAStar", "iRVEA", "NSGAII"}) {
      for (String problem :
          new String[] {"DTLZ2", "DTLZ2.6D", "DTLZ5", "DTLZ7", "DTLZ2Minus", "ZDT1"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
  }
}
