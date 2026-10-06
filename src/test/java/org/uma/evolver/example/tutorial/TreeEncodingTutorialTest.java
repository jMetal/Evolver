package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.TreeMetaSearchConfig;

@DisplayName("Unit tests for class TreeEncodingTutorial")
class TreeEncodingTutorialTest {

  @Nested
  @DisplayName("When summarizing a training: ")
  class SummaryTestCases {

    @Test
    @DisplayName(
        "given an INDICATORS.csv with two checkpoints, when the best NHV is read, then it is the"
            + " smallest of the last checkpoint")
    void givenTwoCheckpoints_whenTheBestNhvIsRead_thenItIsTheSmallestOfTheLast(
        @TempDir Path tempDir) throws IOException {
      // Arrange
      Path indicators = tempDir.resolve("INDICATORS.csv");
      Files.writeString(
          indicators,
          """
          Evaluation,SolutionId,EP,NHV
          100,0,0.5,0.001
          200,0,0.1,0.30
          200,1,0.2,0.20
          """);

      // Act
      double best = TreeEncodingTutorial.bestNhvOfFinalFront(indicators);

      // Assert: 0.001 belongs to an earlier checkpoint
      assertEquals(0.20, best, 1e-12);
    }

    @Test
    @DisplayName("given a METADATA.txt, when read, then the meta-evaluations performed are returned")
    void givenMetadata_whenRead_thenTheMetaEvaluationsAreReturned(@TempDir Path tempDir)
        throws IOException {
      // Arrange
      Path metadata = tempDir.resolve("METADATA.txt");
      Files.writeString(
          metadata, "--- Execution ---\nWall-clock time: 0h 2m 6s\nMeta-evaluations performed: 1050\n");

      // Act & Assert
      assertEquals(1050, TreeEncodingTutorial.metaEvaluations(metadata));
    }

    @Test
    @DisplayName("given an even number of values, when the median is taken, then it is the mean of the two in the middle")
    void givenAnEvenNumberOfValues_whenTheMedianIsTaken_thenItIsTheMeanOfTheMiddleTwo() {
      assertEquals(2.5, TreeEncodingTutorial.median(new double[] {4, 1, 3, 2}), 1e-12);
      assertEquals(3.0, TreeEncodingTutorial.median(new double[] {5, 3, 1}), 1e-12);
    }
  }

  @Nested
  @DisplayName("When reading the meta-optimizer of the tutorial: ")
  class MetaSearchTestCases {

    @Test
    @DisplayName("given the tree meta-optimizer of the tutorial, when read, then it is NSGA-II with the tree encoding stopped after 2 minutes")
    void givenTheTreeMetaOptimizer_whenRead_thenItIsTreeNsgaiiStoppedByTime() {
      // Act
      var config =
          MetaOptimizerConfigurationReader.load("TutorialTimeTreeNSGAIIMetaSearch.yaml");

      // Assert
      assertEquals(TreeMetaSearchConfig.class, config.getClass());
      assertEquals("NSGA-II", config.algorithm());
      assertEquals(2.0, config.metaMaxComputingTimeMinutes(), 1e-12);
    }
  }
}
