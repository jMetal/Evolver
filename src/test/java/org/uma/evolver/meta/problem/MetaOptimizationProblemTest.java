package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

/**
 * Unit tests for {@link MetaOptimizationProblem}.
 *
 * <p>These tests focus on constructor validation, accessor methods, and solution
 * creation. They do NOT run any optimization and are designed to execute quickly.
 */
@DisplayName("MetaOptimizationProblem Unit Tests")
class MetaOptimizationProblemTest {

  private BaseLevelAlgorithm<DoubleSolution> baseAlgorithm;
  private List<Problem<DoubleSolution>> singleProblem;
  private List<String> singleReferenceFront;
  private List<QualityIndicator> singleIndicator;
  private EvaluationBudgetStrategy singleProblemStrategy;
  private static final int ONE_RUN = 1;

  @BeforeEach
  void setUp() {
    baseAlgorithm = new DoubleNSGAII(
        100, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    singleProblem = List.of(new ZDT1());
    singleReferenceFront = List.of("resources/referenceFronts/ZDT1.csv");
    singleIndicator = List.of(new NormalizedHypervolume());
    singleProblemStrategy = new FixedEvaluationsStrategy(List.of(25000));
  }

  /**
   * Helper that creates a valid instance with the default single-problem setup.
   */
  private MetaOptimizationProblem<DoubleSolution> createValidInstance() {
    return new MetaOptimizationProblem<>(
        baseAlgorithm, singleProblem, singleReferenceFront,
        singleIndicator, singleProblemStrategy, ONE_RUN);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Constructor tests
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When constructing")
  class ConstructorTests {

    @Test
    @DisplayName("Given null base algorithm, when constructing, then throw NullParameterException")
    void givenNullBaseAlgorithm_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new MetaOptimizationProblem<>(
              null, singleProblem, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given null problems list, when constructing, then throw NullParameterException")
    void givenNullProblems_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, null, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given null reference front file names, when constructing, then throw NullParameterException")
    void givenNullReferenceFronts_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, null,
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given null indicators list, when constructing, then throw NullParameterException")
    void givenNullIndicators_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              null, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given null evaluation strategy, when constructing, then throw NullParameterException")
    void givenNullEvaluationStrategy_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, null, ONE_RUN));
    }

    @Test
    @DisplayName("Given mismatched problems and reference fronts sizes, when constructing, then throw exception")
    void givenMismatchedSizes_whenConstructing_thenThrowException() {
      // Arrange
      List<Problem<DoubleSolution>> twoProblems = List.of(new ZDT1(), new ZDT4());
      // singleReferenceFront has only 1 element

      // Act & Assert
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, twoProblems, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given non-existent reference front file, when constructing, then throw JMetalException")
    void givenNonExistentReferenceFrontFile_whenConstructing_thenThrowException() {
      // Arrange
      List<String> invalidFile = List.of("non_existent_file.csv");

      // Act & Assert
      assertThrows(JMetalException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, invalidFile,
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given incompatible evaluation strategy, when constructing, then throw exception")
    void givenIncompatibleStrategy_whenConstructing_thenThrowException() {
      // Arrange: strategy expects 2 problems but only 1 is provided
      var strategyForTwoProblems = new FixedEvaluationsStrategy(List.of(25000, 25000));

      // Act & Assert
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, strategyForTwoProblems, ONE_RUN));
    }

    @Test
    @DisplayName("Given empty problems list, when constructing, then throw InvalidConditionException")
    void givenEmptyProblems_whenConstructing_thenThrowException() {
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, List.of(), List.of(),
              singleIndicator, singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given empty indicators list, when constructing, then throw InvalidConditionException")
    void givenEmptyIndicators_whenConstructing_thenThrowException() {
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              List.of(), singleProblemStrategy, ONE_RUN));
    }

    @Test
    @DisplayName("Given zero independent runs, when constructing, then throw InvalidConditionException")
    void givenZeroIndependentRuns_whenConstructing_thenThrowException() {
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, singleProblemStrategy, 0));
    }

    @Test
    @DisplayName("Given negative independent runs, when constructing, then throw InvalidConditionException")
    void givenNegativeIndependentRuns_whenConstructing_thenThrowException() {
      assertThrows(InvalidConditionException.class, () ->
          new MetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, singleProblemStrategy, -1));
    }

    @Test
    @DisplayName("Given valid arguments, when constructing, then instance is created successfully")
    void givenValidArguments_whenConstructing_thenInstanceIsCreated() {
      // Act & Assert
      assertDoesNotThrow(MetaOptimizationProblemTest.this::createValidInstance);
    }

    @Test
    @DisplayName("Given multiple problems and matching fronts, when constructing, then instance is created")
    void givenMultipleProblemsWithMatchingFronts_whenConstructing_thenInstanceIsCreated() {
      // Arrange
      List<Problem<DoubleSolution>> twoProblems = List.of(new ZDT1(), new ZDT4());
      List<String> twoFronts = List.of(
          "resources/referenceFronts/ZDT1.csv",
          "resources/referenceFronts/ZDT4.csv");
      var twoStrategy = new FixedEvaluationsStrategy(List.of(25000, 25000));

      // Act & Assert
      assertDoesNotThrow(() -> new MetaOptimizationProblem<>(
          baseAlgorithm, twoProblems, twoFronts,
          singleIndicator, twoStrategy, ONE_RUN));
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Accessor tests
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When accessing properties")
  class AccessorTests {

    private MetaOptimizationProblem<DoubleSolution> problem;

    @BeforeEach
    void setUp() {
      problem = createValidInstance();
    }

    @Test
    @DisplayName("Given valid instance, when getting number of variables, then return parameter count")
    void givenValidInstance_whenGettingNumberOfVariables_thenReturnParameterCount() {
      // Act
      int numVars = problem.numberOfVariables();

      // Assert
      assertEquals(problem.parameters().size(), numVars);
      assertTrue(numVars > 0);
    }

    @Test
    @DisplayName("Given one indicator, when getting number of objectives, then return one")
    void givenOneIndicator_whenGettingNumberOfObjectives_thenReturnOne() {
      // Act & Assert
      assertEquals(1, problem.numberOfObjectives());
    }

    @Test
    @DisplayName("Given two indicators, when getting number of objectives, then return two")
    void givenTwoIndicators_whenGettingNumberOfObjectives_thenReturnTwo() {
      // Arrange
      List<QualityIndicator> twoIndicators = List.of(new NormalizedHypervolume(), new Epsilon());
      var twoIndicatorProblem = new MetaOptimizationProblem<>(
          baseAlgorithm, singleProblem, singleReferenceFront,
          twoIndicators, singleProblemStrategy, ONE_RUN);

      // Act & Assert
      assertEquals(2, twoIndicatorProblem.numberOfObjectives());
    }

    @Test
    @DisplayName("Given valid instance, when getting number of constraints, then return zero")
    void givenValidInstance_whenGettingNumberOfConstraints_thenReturnZero() {
      // Act & Assert
      assertEquals(0, problem.numberOfConstraints());
    }

    @Test
    @DisplayName("Given valid instance, when getting name, then return expected name")
    void givenValidInstance_whenGettingName_thenReturnExpectedName() {
      // Act & Assert
      assertEquals("Meta-optimization problem", problem.name());
    }

    @Test
    @DisplayName("Given valid instance, when getting parameters, then return non-empty list")
    void givenValidInstance_whenGettingParameters_thenReturnNonEmptyList() {
      // Act
      var parameters = problem.parameters();

      // Assert
      assertNotNull(parameters);
      assertFalse(parameters.isEmpty());
    }

    @Test
    @DisplayName("Given valid instance, when getting parameters, then return unmodifiable list")
    void givenValidInstance_whenGettingParameters_thenReturnUnmodifiableList() {
      // Arrange
      var parameters = problem.parameters();

      // Act & Assert
      assertThrows(UnsupportedOperationException.class, () -> parameters.clear());
    }

    @Test
    @DisplayName("Given valid instance, when getting problems, then return the problem list")
    void givenValidInstance_whenGettingProblems_thenReturnProblemList() {
      // Act
      var problems = problem.problems();

      // Assert
      assertNotNull(problems);
      assertEquals(1, problems.size());
    }

    @Test
    @DisplayName("Given valid instance, when getting evaluation strategy, then return the strategy")
    void givenValidInstance_whenGettingEvaluationStrategy_thenReturnStrategy() {
      // Act & Assert
      assertEquals(singleProblemStrategy, problem.evaluationBudgetStrategy());
    }

    @Test
    @DisplayName("Given valid instance, when getting top-level parameters, then return non-empty list")
    void givenValidInstance_whenGettingTopLevelParameters_thenReturnNonEmptyList() {
      // Act
      var topLevelParams = problem.topLevelParameters();

      // Assert
      assertNotNull(topLevelParams);
      assertFalse(topLevelParams.isEmpty());
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Solution creation tests
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When creating solutions")
  class SolutionCreationTests {

    private MetaOptimizationProblem<DoubleSolution> problem;

    @BeforeEach
    void setUp() {
      problem = createValidInstance();
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution is not null")
    void givenValidInstance_whenCreatingSolution_thenSolutionIsNotNull() {
      // Act
      DoubleSolution solution = problem.createSolution();

      // Assert
      assertNotNull(solution);
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution has correct number of variables")
    void givenValidInstance_whenCreatingSolution_thenSolutionHasCorrectVariableCount() {
      // Act
      DoubleSolution solution = problem.createSolution();

      // Assert
      assertEquals(problem.numberOfVariables(), solution.variables().size());
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution has correct number of objectives")
    void givenValidInstance_whenCreatingSolution_thenSolutionHasCorrectObjectiveCount() {
      // Act
      DoubleSolution solution = problem.createSolution();

      // Assert
      assertEquals(problem.numberOfObjectives(), solution.objectives().length);
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then all variables are in [0, 1]")
    void givenValidInstance_whenCreatingSolution_thenVariablesAreInRange() {
      // Act
      DoubleSolution solution = problem.createSolution();

      // Assert
      for (double variable : solution.variables()) {
        assertTrue(variable >= 0.0 && variable <= 1.0,
            "Variable " + variable + " is outside the expected [0, 1] range");
      }
    }

    @Test
    @DisplayName("Given valid instance, when creating two solutions, then they have different variables")
    void givenValidInstance_whenCreatingTwoSolutions_thenTheyDiffer() {
      // Act
      DoubleSolution solution1 = problem.createSolution();
      DoubleSolution solution2 = problem.createSolution();

      // Assert: with random initialization, at least one variable should differ
      boolean differ = false;
      for (int i = 0; i < solution1.variables().size(); i++) {
        if (!solution1.variables().get(i).equals(solution2.variables().get(i))) {
          differ = true;
          break;
        }
      }
      assertTrue(differ, "Two randomly created solutions should differ in at least one variable");
    }
  }
}
