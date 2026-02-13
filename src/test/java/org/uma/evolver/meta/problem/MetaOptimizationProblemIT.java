package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.nsgaii.PermutationNSGAII;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAC100TSP;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Integration tests for {@link MetaOptimizationProblem}.
 *
 * <p>These tests execute full meta-optimization evaluations with real algorithms and problems.
 * They are slow (each evaluation runs 25,000+ function evaluations) and are intended to be
 * executed during the Maven {@code verify} phase via Failsafe.
 */
@DisplayName("MetaOptimizationProblem Integration Tests")
@Tag("integration")
class MetaOptimizationProblemIT {

  // ──────────────────────────────────────────────────────────────────────────
  // Continuous problems with NSGA-II
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating continuous problems with NSGA-II")
  class ContinuousProblemsWithNSGAII {

    private BaseLevelAlgorithm<DoubleSolution> nsgaii;

    @BeforeEach
    void setUp() {
      nsgaii = new DoubleNSGAII(
          100, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    }

    @Test
    @DisplayName("Given single ZDT1 problem and NormalizedHypervolume, when evaluating, then objective is in valid range")
    void givenSingleProblemAndOneIndicator_whenEvaluating_thenObjectiveIsInValidRange() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));

      var metaProblem = new MetaOptimizationProblem<>(
          nsgaii, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "NormalizedHypervolume should be at most 1.0");
    }

    @Test
    @DisplayName("Given five ZDT problems, when evaluating, then objective aggregates across all problems")
    void givenMultipleProblems_whenEvaluating_thenObjectiveIsComputed() {
      // Arrange
      List<Problem<DoubleSolution>> problems =
          List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());
      List<String> fronts = List.of(
          "resources/referenceFronts/ZDT1.csv",
          "resources/referenceFronts/ZDT2.csv",
          "resources/referenceFronts/ZDT3.csv",
          "resources/referenceFronts/ZDT4.csv",
          "resources/referenceFronts/ZDT6.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy =
          new FixedEvaluationsStrategy(List.of(25000, 25000, 25000, 25000, 25000));

      var metaProblem = new MetaOptimizationProblem<>(
          nsgaii, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "Mean NormalizedHypervolume across problems should be non-negative");
    }

    @Test
    @DisplayName("Given two indicators, when evaluating, then both objective values are computed")
    void givenTwoIndicators_whenEvaluating_thenBothObjectivesAreComputed() {
      // Arrange
      JMetalRandom.getInstance().setSeed(1);
      List<Problem<DoubleSolution>> problems = List.of(new ZDT4());
      List<String> fronts = List.of("resources/referenceFronts/ZDT4.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));

      var metaProblem = new MetaOptimizationProblem<>(
          nsgaii, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(2, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[1] >= 0.0,
          "Epsilon indicator should be non-negative");
    }

    @Test
    @DisplayName("Given multiple independent runs, when evaluating, then result is median across runs")
    void givenMultipleRuns_whenEvaluating_thenResultIsMedianAcrossRuns() {
      // Arrange
      JMetalRandom.getInstance().setSeed(42);
      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000));
      int threeRuns = 3;

      var metaProblem = new MetaOptimizationProblem<>(
          nsgaii, problems, fronts, indicators, strategy, threeRuns);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert: median of 3 runs should still produce a valid indicator value
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0,
          "Median NormalizedHypervolume over 3 runs should be non-negative");
      assertTrue(solution.objectives()[0] <= 1.0,
          "Median NormalizedHypervolume over 3 runs should be at most 1.0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Permutation problems with NSGA-II
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When evaluating permutation problems with NSGA-II")
  class PermutationProblemsWithNSGAII {

    @Test
    @DisplayName("Given two TSP problems and PISAHypervolume, when evaluating, then objective is computed")
    void givenTSPProblems_whenEvaluating_thenObjectiveIsComputed() throws IOException {
      // Arrange
      var nsgaii = new PermutationNSGAII(
          100, new YAMLParameterSpace("NSGAIIPermutation.yaml", new PermutationParameterFactory()));

      List<Problem<PermutationSolution<Integer>>> problems =
          List.of(new KroAB100TSP(), new KroAC100TSP());
      List<String> fronts = List.of(
          "resources/referenceFrontsTSP/KroAB100TSP.csv",
          "resources/referenceFrontsTSP/KroAC100TSP.csv");
      List<QualityIndicator> indicators = List.of(new PISAHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(25000, 25000));

      var metaProblem = new MetaOptimizationProblem<>(
          nsgaii, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      // PISAHypervolume is not normalized, so we only check it's been computed (non-NaN)
      assertTrue(Double.isFinite(solution.objectives()[0]),
          "Hypervolume should be a finite number");
    }
  }
}
