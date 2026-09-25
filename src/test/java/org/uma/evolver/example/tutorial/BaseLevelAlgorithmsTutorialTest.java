package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Keeps tutorial E2 ("Base-level algorithms") from rotting: its page shows the output of {@link
 * BaseLevelAlgorithmsTutorial}, so these tests run it with a small budget and check the facts the
 * page relies on.
 */
@DisplayName("Unit tests for class BaseLevelAlgorithmsTutorial")
class BaseLevelAlgorithmsTutorialTest {

  @Nested
  @DisplayName("When running the tutorial: ")
  class RunTestCases {

    @Test
    @DisplayName(
        "given the tutorial, when it is run, then every step prints its result and"
            + " step 2 writes the VAR/FUN files")
    void givenTutorial_whenRun_thenEveryStepCompletes(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      PrintStream standardOutput = System.out;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      System.setOut(new PrintStream(output));

      // Act
      try {
        BaseLevelAlgorithmsTutorial.run(tempDir.toString());
      } finally {
        System.setOut(standardOutput);
      }

      // Assert
      String printed = output.toString();
      assertTrue(printed.contains("100 solutions after 25000 evaluations"), printed);
      assertTrue(printed.contains("Standard configuration on ZDT1: EP = "), printed);
      assertTrue(printed.contains("Other configuration on ZDT1:    EP = "), printed);
      assertTrue(printed.contains("1 configuration(s) in the file"), printed);
      assertTrue(printed.contains("Default configuration on ZDT2:  EP = "), printed);
      assertTrue(printed.contains("binary solutions; the first one has objectives -"), printed);
      assertTrue(Files.exists(tempDir.resolve("VAR.csv")));
      assertTrue(Files.exists(tempDir.resolve("FUN.csv")));
    }
  }

  @Nested
  @DisplayName("When computing the quality indicators of a front: ")
  class IndicatorsTestCases {

    @Test
    @DisplayName(
        "given the reference front itself, when its indicators are computed, then both are zero"
            + " (the best possible value)")
    void givenReferenceFront_whenIndicatorsComputed_thenBothAreZero() throws IOException {
      // Arrange
      double[][] referenceFront = VectorUtils.readVectors("resources/referenceFronts/ZDT1.csv", ",");
      var problem = new ZDT1();
      List<DoubleSolution> front =
          Arrays.stream(referenceFront)
              .map(
                  point -> {
                    DoubleSolution solution = problem.createSolution();
                    solution.objectives()[0] = point[0];
                    solution.objectives()[1] = point[1];
                    return solution;
                  })
              .toList();

      // Act
      String indicators = BaseLevelAlgorithmsTutorial.indicators(front, "ZDT1.csv");

      // Assert
      assertEquals("EP = 0.0000, NHV = 0.0000", indicators);
    }
  }
}
