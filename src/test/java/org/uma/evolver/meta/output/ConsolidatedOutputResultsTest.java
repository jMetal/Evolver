package org.uma.evolver.meta.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("Unit tests for class ConsolidatedOutputResults")
class ConsolidatedOutputResultsTest {

  @TempDir private Path tempDir;

  private MetaOptimizationProblem<DoubleSolution> problem;
  private List<QualityIndicator> indicators;
  private List<DoubleSolution> population;

  @BeforeEach
  void setUp() {
    indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    problem =
        new MetaOptimizationProblem<>(
            new DoubleNSGAII(
                100, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory())),
            List.of(new ZDT1()),
            List.of("resources/referenceFronts/ZDT1.csv"),
            indicators,
            new FixedEvaluationsStrategy(List.of(1000)),
            1);
    // Two non-dominated configurations and a third one dominated by both
    population = List.of(solution(0.1, 0.5), solution(0.5, 0.1), solution(0.9, 0.9));
  }

  private DoubleSolution solution(double ep, double nhv) {
    DoubleSolution solution = problem.createSolution();
    solution.objectives()[0] = ep;
    solution.objectives()[1] = nhv;
    return solution;
  }

  private ConsolidatedOutputResults outputResults() {
    return new ConsolidatedOutputResults(
        problem,
        "ZDT1",
        indicators,
        tempDir.toString(),
        MetaOptimizerConfig.builder().baseLevelAlgorithmName("NSGA-II").build());
  }

  private long dataRows(String fileName) throws IOException {
    return Files.readAllLines(tempDir.resolve(fileName)).size() - 1; // minus the header
  }

  @Nested
  @DisplayName("When writing a checkpoint: ")
  class WriteResultsTestCases {

    @Test
    @DisplayName(
        "given the default settings, when a population is written, then only its non-dominated"
            + " solutions are written, and no population files")
    void givenDefaultSettings_whenWritten_thenOnlyNonDominatedSolutionsAreWritten()
        throws IOException {
      // Arrange
      ConsolidatedOutputResults outputResults = outputResults();
      outputResults.updateEvaluations(50);

      // Act
      outputResults.writeResultsToFiles(population);

      // Assert
      assertEquals(2, dataRows("INDICATORS.csv"));
      assertEquals(2, dataRows("CONFIGURATIONS.csv"));
      assertFalse(Files.exists(tempDir.resolve("POPULATION_INDICATORS.csv")));
      assertFalse(Files.exists(tempDir.resolve("POPULATION_CONFIGURATIONS.csv")));
    }

    @Test
    @DisplayName(
        "given writePopulation, when two checkpoints are written, then the population files have"
            + " every solution of both, with the same header as the other files")
    void givenWritePopulation_whenTwoCheckpointsWritten_thenWholePopulationIsWritten()
        throws IOException {
      // Arrange
      ConsolidatedOutputResults outputResults = outputResults().writePopulation(true);

      // Act
      outputResults.updateEvaluations(50);
      outputResults.writeResultsToFiles(population);
      outputResults.updateEvaluations(100);
      outputResults.writeResultsToFiles(population);

      // Assert
      assertEquals(4, dataRows("INDICATORS.csv"));
      assertEquals(6, dataRows("POPULATION_INDICATORS.csv"));
      assertEquals(6, dataRows("POPULATION_CONFIGURATIONS.csv"));
      List<String> populationIndicators =
          Files.readAllLines(tempDir.resolve("POPULATION_INDICATORS.csv"));
      assertEquals(
          Files.readAllLines(tempDir.resolve("INDICATORS.csv")).get(0),
          populationIndicators.get(0));
      assertEquals("100,2,0.9,0.9", populationIndicators.get(6));
      assertEquals(
          Files.readAllLines(tempDir.resolve("CONFIGURATIONS.csv")).get(0),
          Files.readAllLines(tempDir.resolve("POPULATION_CONFIGURATIONS.csv")).get(0));
    }
  }

  @Nested
  @DisplayName("Time of each checkpoint in VAR_CONF.txt")
  class CheckpointTime {

    @Test
    @DisplayName("Given the computing time of the meta-optimizer, when written, then each checkpoint has the evaluations and the minutes")
    void givenComputingTimeWhenWrittenThenCheckpointHasEvaluationsAndMinutes() throws IOException {
      // Arrange
      ConsolidatedOutputResults outputResults = outputResults();

      // Act
      outputResults.updateEvaluations(1000);
      outputResults.updateComputingTime(192_000);
      outputResults.writeResultsToFiles(population);

      // Assert
      List<String> lines = Files.readAllLines(tempDir.resolve("VAR_CONF.txt"));
      assertEquals("# Evaluation: 1000", lines.get(0));
      assertEquals("# Time (min): 3.200", lines.get(1));
      assertTrue(lines.get(2).contains(" | "));
    }

    @Test
    @DisplayName("Given no computing time, when written, then the time elapsed since creation is used")
    void givenNoComputingTimeWhenWrittenThenElapsedTimeIsUsed() throws IOException {
      // Arrange
      ConsolidatedOutputResults outputResults = outputResults();

      // Act
      outputResults.updateEvaluations(500);
      outputResults.writeResultsToFiles(population);

      // Assert
      List<String> lines = Files.readAllLines(tempDir.resolve("VAR_CONF.txt"));
      assertEquals("# Evaluation: 500", lines.get(0));
      assertTrue(lines.get(1).matches("# Time \\(min\\): \\d+\\.\\d{3}"), lines.get(1));
    }
  }
}
