package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.meta.strategy.RandomRangeEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Integration tests for {@link TreeMetaOptimizationProblem}.
 *
 * <p>These tests execute full meta-optimization evaluations with real algorithms and problems.
 * They are slow (each evaluation runs 25,000+ function evaluations) and are intended to be
 * executed during the Maven {@code verify} phase via Failsafe.
 */
@DisplayName("TreeMetaOptimizationProblem Integration Tests")
@Tag("integration")
class TreeMetaOptimizationProblemIT {

  private BaseLevelAlgorithm<DoubleSolution> baseAlgorithm;
  private YAMLParameterSpace parameterSpace;
  private TreeSolutionGenerator solutionGenerator;

  @BeforeEach
  void setUp() {
    parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
    baseAlgorithm = new DoubleNSGAII(100, parameterSpace);
    solutionGenerator = new TreeSolutionGenerator(parameterSpace);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Single problem, single indicator
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating with a single problem and indicator")
  class SingleProblemSingleIndicatorTests {

    @Test
    @DisplayName("Given ZDT1 and NormalizedHypervolume, when evaluating, then objective is in valid range")
    void givenZDT1AndNormalizedHypervolume_whenEvaluating_thenObjectiveIsInValidRange() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "NormalizedHypervolume should be at most 1.0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Multiple problems
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating with multiple problems")
  class MultipleProblemsTests {

    @Test
    @DisplayName("Given three ZDT problems, when evaluating, then objective aggregates across all problems")
    void givenMultipleProblems_whenEvaluating_thenObjectiveIsComputed() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1(), new ZDT2(), new ZDT4());
      List<String> fronts = List.of(
          "resources/referenceFronts/ZDT1.csv",
          "resources/referenceFronts/ZDT2.csv",
          "resources/referenceFronts/ZDT4.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy =
          new FixedEvaluationsStrategy(List.of(25000, 25000, 25000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "Mean NormalizedHypervolume across problems should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "Mean NormalizedHypervolume across problems should be at most 1.0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Multiple indicators
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating with multiple indicators")
  class MultipleIndicatorsTests {

    @Test
    @DisplayName("Given two indicators, when evaluating, then both objective values are computed")
    void givenTwoIndicators_whenEvaluating_thenBothObjectivesAreComputed() {
      // Arrange
      JMetalRandom.getInstance().setSeed(1);
      List<Problem<DoubleSolution>> problems = List.of(new ZDT4());
      List<String> fronts = List.of("resources/referenceFronts/ZDT4.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(2, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "NormalizedHypervolume should be at most 1.0");
      assertTrue(solution.objectives()[1] >= 0.0,
          "Epsilon indicator should be non-negative");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Multiple independent runs
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating with multiple independent runs")
  class MultipleRunsTests {

    @Test
    @DisplayName("Given three independent runs, when evaluating, then result is a valid indicator value")
    void givenThreeRuns_whenEvaluating_thenResultIsInValidRange() {
      // Arrange — the result is the median over the 3 runs
      JMetalRandom.getInstance().setSeed(42);
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, problems, fronts, indicators, strategy, 3, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "Median NormalizedHypervolume over 3 runs should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "Median NormalizedHypervolume over 3 runs should be at most 1.0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // RandomRangeEvaluationsStrategy
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When using RandomRangeEvaluationsStrategy")
  class RandomRangeStrategyTests {

    @Test
    @DisplayName("Given RandomRangeEvaluationsStrategy, when evaluating, then objective is computed")
    void givenRandomRangeStrategy_whenEvaluating_thenObjectiveIsComputed() {
      // Arrange
      JMetalRandom.getInstance().setSeed(7);
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new RandomRangeEvaluationsStrategy(20000, 30000);

      var metaProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "NormalizedHypervolume should be at most 1.0");
    }
  }
}
