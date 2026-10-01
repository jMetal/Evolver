package org.uma.evolver.example.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Keeps the algorithm guide of NSGA-II ({@code docs/algorithms/nsgaii.rst}) from rotting: runs its
 * first step, checks that each variant changes the default configuration, and runs the comparison
 * with minimal budgets.
 */
@Tag("integration")
@DisplayName("Integration tests for class NSGAIIGuide")
class NSGAIIGuideIT {

  @Test
  @DisplayName("given ZDT1, when NSGA-II runs with its default configuration, then it returns a front")
  void givenZdt1_whenRunWithTheDefaultConfiguration_thenAFrontIsReturned() throws IOException {
    // Act & Assert
    assertFalse(NSGAIIGuide.runOnZDT1().isEmpty());
  }

  @Test
  @DisplayName("given the default configuration, when the variants are derived, then each one changes it")
  void givenDefaultConfiguration_whenVariantsAreDerived_thenEachOneChangesIt() throws IOException {
    // Act
    Map<String, String> variants = NSGAIIGuide.variants();

    // Assert
    String standard = variants.get("NSGAII");
    assertEquals(4, variants.size());
    assertTrue(variants.get("NSGAIISteadyState").contains("--offspringPopulationSize 1 "));
    assertTrue(variants.get("NSGAIICrowdingArchive").contains("crowdingDistanceArchive"));
    assertTrue(variants.get("NSGAIIUnboundedArchive").contains("unboundedArchive"));
    variants.forEach(
        (tag, configuration) ->
            assertTrue(tag.equals("NSGAII") || !configuration.equals(standard), tag));
  }

  @Test
  @DisplayName(
      "given minimal budgets, when the comparison runs, then every variant is run on every"
          + " problem")
  void givenMinimalBudgets_whenCompared_thenEveryProblemIsCovered(@TempDir Path tempDir)
      throws IOException {
    // Act
    NSGAIIGuide.compare(tempDir.toString(), 2, 0.05, 4);

    // Assert
    Path data = tempDir.resolve("comparison/data");
    for (String algorithm :
        new String[] {
          "NSGAII", "NSGAIISteadyState", "NSGAIICrowdingArchive", "NSGAIIUnboundedArchive"
        }) {
      for (String problem : new String[] {"ZDT1", "ZDT4", "DTLZ2"}) {
        assertTrue(
            Files.exists(data.resolve(algorithm).resolve(problem).resolve("FUN1.csv")),
            algorithm + " on " + problem);
      }
    }
  }
}
