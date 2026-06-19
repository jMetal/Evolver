package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Integration tests verifying that PAES variants work correctly as base-level algorithms within
 * both meta-problem encodings: flat double ({@link MetaOptimizationProblem}) and derivation tree
 * ({@link TreeMetaOptimizationProblem}).
 */
@DisplayName("PAES meta-optimization integration tests")
@Tag("integration")
class PAESMetaOptimizationIT {

  // ──────────────────────────────────────────────────────────────────────────
  // DoublePAES — flat double encoding (MetaOptimizationProblem)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When DoublePAES is used as the base algorithm in MetaOptimizationProblem")
  class DoublePAESInFlatEncoding {

    @Test
    @DisplayName("Given ZDT1 and NormalizedHypervolume, when evaluating, then objective is in [0, 1]")
    void givenZDT1AndNormalizedHypervolume_whenEvaluating_thenObjectiveIsInValidRange() {
      // Arrange
      var paes = new DoublePAES(
          100, new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));

      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(10000));

      var metaProblem = new MetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1);
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
    @DisplayName("Given two indicators, when evaluating, then both objectives are computed")
    void givenTwoIndicators_whenEvaluating_thenBothObjectivesAreComputed() {
      // Arrange
      var paes = new DoublePAES(
          100, new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));

      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(10000));

      var metaProblem = new MetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(2, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0, "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[1] >= 0.0, "Epsilon should be non-negative");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // DoublePAES — derivation tree encoding (TreeMetaOptimizationProblem)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When DoublePAES is used as the base algorithm in TreeMetaOptimizationProblem")
  class DoublePAESInTreeEncoding {

    @Test
    @DisplayName("Given ZDT1 and NormalizedHypervolume, when evaluating, then objective is in [0, 1]")
    void givenZDT1AndNormalizedHypervolume_whenEvaluating_thenObjectiveIsInValidRange() {
      // Arrange
      var parameterSpace = new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory());
      var paes = new DoublePAES(100, parameterSpace);
      var solutionGenerator = new TreeSolutionGenerator(parameterSpace);

      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(10000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1, solutionGenerator);
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

    @Test
    @DisplayName("Given two indicators, when evaluating, then both objectives are computed")
    void givenTwoIndicators_whenEvaluating_thenBothObjectivesAreComputed() {
      // Arrange
      var parameterSpace = new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory());
      var paes = new DoublePAES(100, parameterSpace);
      var solutionGenerator = new TreeSolutionGenerator(parameterSpace);

      List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
      List<String> fronts = List.of("resources/referenceFronts/ZDT1.csv");
      List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(10000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(2, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0, "NormalizedHypervolume should be non-negative");
      assertTrue(solution.objectives()[1] >= 0.0, "Epsilon should be non-negative");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // PermutationPAES — flat double encoding (MetaOptimizationProblem)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When PermutationPAES is used as the base algorithm in MetaOptimizationProblem")
  class PermutationPAESInFlatEncoding {

    @Test
    @DisplayName("Given KroAB100TSP and Epsilon, when evaluating, then objective is non-negative")
    void givenKroAB100TSPAndEpsilon_whenEvaluating_thenObjectiveIsNonNegative() throws IOException {
      // Arrange
      var paes = new PermutationPAES(
          100, new YAMLParameterSpace("PAESPermutation.yaml", new PermutationParameterFactory()));

      List<Problem<PermutationSolution<Integer>>> problems = List.of(new KroAB100TSP());
      List<String> fronts = List.of("resources/referenceFrontsTSP/KroAB100TSP.csv");
      List<QualityIndicator> indicators = List.of(new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(1000));

      var metaProblem = new MetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1);
      DoubleSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0, "Epsilon should be non-negative");
      assertTrue(metaProblem.parameters().size() > 0,
          "Flattened permutation parameter list should be non-empty");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // PermutationPAES — derivation tree encoding (TreeMetaOptimizationProblem)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When PermutationPAES is used as the base algorithm in TreeMetaOptimizationProblem")
  class PermutationPAESInTreeEncoding {

    @Test
    @DisplayName("Given KroAB100TSP and Epsilon, when evaluating, then objective is non-negative")
    void givenKroAB100TSPAndEpsilon_whenEvaluating_thenObjectiveIsNonNegative() throws IOException {
      // Arrange
      var parameterSpace =
          new YAMLParameterSpace("PAESPermutation.yaml", new PermutationParameterFactory());
      var paes = new PermutationPAES(100, parameterSpace);
      var solutionGenerator = new TreeSolutionGenerator(parameterSpace);

      List<Problem<PermutationSolution<Integer>>> problems = List.of(new KroAB100TSP());
      List<String> fronts = List.of("resources/referenceFrontsTSP/KroAB100TSP.csv");
      List<QualityIndicator> indicators = List.of(new Epsilon());
      EvaluationBudgetStrategy strategy = new FixedEvaluationsStrategy(List.of(1000));

      var metaProblem = new TreeMetaOptimizationProblem<>(
          paes, problems, fronts, indicators, strategy, 1, solutionGenerator);
      DerivationTreeSolution solution = metaProblem.createSolution();

      // Act
      metaProblem.evaluate(solution);

      // Assert
      assertEquals(1, metaProblem.numberOfObjectives());
      assertTrue(solution.objectives()[0] >= 0.0, "Epsilon should be non-negative");
    }
  }
}
