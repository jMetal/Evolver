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
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
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

@DisplayName("Unit tests for class TreeMetaOptimizationProblem")
class TreeMetaOptimizationProblemTest {

  private BaseLevelAlgorithm<DoubleSolution> baseAlgorithm;
  private List<Problem<DoubleSolution>> singleProblem;
  private List<String> singleReferenceFront;
  private List<QualityIndicator> singleIndicator;
  private EvaluationBudgetStrategy singleProblemStrategy;
  private TreeSolutionGenerator solutionGenerator;
  private static final int ONE_RUN = 1;

  @BeforeEach
  void setUp() {
    var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
    baseAlgorithm = new DoubleNSGAII(100, parameterSpace);
    singleProblem = List.of(new ZDT1());
    singleReferenceFront = List.of("resources/referenceFronts/ZDT1.csv");
    singleIndicator = List.of(new NormalizedHypervolume());
    singleProblemStrategy = new FixedEvaluationsStrategy(List.of(25000));
    solutionGenerator = new TreeSolutionGenerator(parameterSpace);
  }

  private TreeMetaOptimizationProblem<DoubleSolution> createValidInstance() {
    return new TreeMetaOptimizationProblem<>(
        baseAlgorithm, singleProblem, singleReferenceFront,
        singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator);
  }

  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When constructing")
  class ConstructorTests {

    @Test
    @DisplayName("Given null base algorithm, when constructing, then throw NullParameterException")
    void givenNullBaseAlgorithm_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              null, singleProblem, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given null problems list, when constructing, then throw NullParameterException")
    void givenNullProblems_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, null, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given null reference front file names, when constructing, then throw NullParameterException")
    void givenNullReferenceFronts_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, null,
              singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given null indicators list, when constructing, then throw NullParameterException")
    void givenNullIndicators_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              null, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given null evaluation strategy, when constructing, then throw NullParameterException")
    void givenNullEvaluationStrategy_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, null, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given null solution generator, when constructing, then throw NullParameterException")
    void givenNullSolutionGenerator_whenConstructing_thenThrowException() {
      assertThrows(NullParameterException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN, null));
    }

    @Test
    @DisplayName("Given mismatched problems and reference fronts sizes, when constructing, then throw exception")
    void givenMismatchedSizes_whenConstructing_thenThrowException() {
      List<Problem<DoubleSolution>> twoProblems = List.of(new ZDT1(), new ZDT4());
      assertThrows(InvalidConditionException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, twoProblems, singleReferenceFront,
              singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given non-existent reference front file, when constructing, then throw JMetalException")
    void givenNonExistentReferenceFrontFile_whenConstructing_thenThrowException() {
      List<String> invalidFile = List.of("non_existent_file.csv");
      assertThrows(JMetalException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, invalidFile,
              singleIndicator, singleProblemStrategy, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given incompatible evaluation strategy, when constructing, then throw exception")
    void givenIncompatibleStrategy_whenConstructing_thenThrowException() {
      var strategyForTwoProblems = new FixedEvaluationsStrategy(List.of(25000, 25000));
      assertThrows(InvalidConditionException.class, () ->
          new TreeMetaOptimizationProblem<>(
              baseAlgorithm, singleProblem, singleReferenceFront,
              singleIndicator, strategyForTwoProblems, ONE_RUN, solutionGenerator));
    }

    @Test
    @DisplayName("Given valid arguments, when constructing, then instance is created successfully")
    void givenValidArguments_whenConstructing_thenInstanceIsCreated() {
      assertDoesNotThrow(TreeMetaOptimizationProblemTest.this::createValidInstance);
    }

    @Test
    @DisplayName("Given multiple problems and matching fronts, when constructing, then instance is created")
    void givenMultipleProblemsWithMatchingFronts_whenConstructing_thenInstanceIsCreated() {
      List<Problem<DoubleSolution>> twoProblems = List.of(new ZDT1(), new ZDT4());
      List<String> twoFronts = List.of(
          "resources/referenceFronts/ZDT1.csv",
          "resources/referenceFronts/ZDT4.csv");
      var twoStrategy = new FixedEvaluationsStrategy(List.of(25000, 25000));
      assertDoesNotThrow(() -> new TreeMetaOptimizationProblem<>(
          baseAlgorithm, twoProblems, twoFronts,
          singleIndicator, twoStrategy, ONE_RUN, solutionGenerator));
    }
  }

  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When accessing properties")
  class AccessorTests {

    private TreeMetaOptimizationProblem<DoubleSolution> problem;

    @BeforeEach
    void setUp() {
      problem = createValidInstance();
    }

    @Test
    @DisplayName("Given one indicator, when getting number of objectives, then return one")
    void givenOneIndicator_whenGettingNumberOfObjectives_thenReturnOne() {
      assertEquals(1, problem.numberOfObjectives());
    }

    @Test
    @DisplayName("Given two indicators, when getting number of objectives, then return two")
    void givenTwoIndicators_whenGettingNumberOfObjectives_thenReturnTwo() {
      List<QualityIndicator> twoIndicators = List.of(new NormalizedHypervolume(), new Epsilon());
      var twoIndicatorProblem = new TreeMetaOptimizationProblem<>(
          baseAlgorithm, singleProblem, singleReferenceFront,
          twoIndicators, singleProblemStrategy, ONE_RUN, solutionGenerator);
      assertEquals(2, twoIndicatorProblem.numberOfObjectives());
    }

    @Test
    @DisplayName("Given valid instance, when getting number of variables, then return top-level parameter count")
    void givenValidInstance_whenGettingNumberOfVariables_thenReturnTopLevelParameterCount() {
      int expected = baseAlgorithm.parameterSpace().topLevelParameters().size();
      assertEquals(expected, problem.numberOfVariables());
      assertTrue(problem.numberOfVariables() > 0);
    }

    @Test
    @DisplayName("Given valid instance, when getting number of constraints, then return zero")
    void givenValidInstance_whenGettingNumberOfConstraints_thenReturnZero() {
      assertEquals(0, problem.numberOfConstraints());
    }

    @Test
    @DisplayName("Given valid instance, when getting name, then return expected name")
    void givenValidInstance_whenGettingName_thenReturnExpectedName() {
      assertEquals("TreeMetaOptimizationProblem", problem.name());
    }

    @Test
    @DisplayName("Given valid instance, when getting problems, then return the problem list")
    void givenValidInstance_whenGettingProblems_thenReturnProblemList() {
      assertNotNull(problem.problems());
      assertEquals(1, problem.problems().size());
    }

    @Test
    @DisplayName("Given valid instance, when getting evaluation strategy, then return the strategy")
    void givenValidInstance_whenGettingEvaluationStrategy_thenReturnStrategy() {
      assertEquals(singleProblemStrategy, problem.evaluationBudgetStrategy());
    }
  }

  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("When creating solutions")
  class SolutionCreationTests {

    private TreeMetaOptimizationProblem<DoubleSolution> problem;

    @BeforeEach
    void setUp() {
      problem = createValidInstance();
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution is not null")
    void givenValidInstance_whenCreatingSolution_thenSolutionIsNotNull() {
      DerivationTreeSolution solution = problem.createSolution();
      assertNotNull(solution);
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution has correct number of objectives")
    void givenValidInstance_whenCreatingSolution_thenSolutionHasCorrectObjectiveCount() {
      DerivationTreeSolution solution = problem.createSolution();
      assertEquals(problem.numberOfObjectives(), solution.objectives().length);
    }

    @Test
    @DisplayName("Given valid instance, when creating a solution, then solution has at least one root")
    void givenValidInstance_whenCreatingSolution_thenSolutionHasAtLeastOneRoot() {
      DerivationTreeSolution solution = problem.createSolution();
      assertFalse(solution.roots().isEmpty());
    }

    @Test
    @DisplayName("Given valid instance, when creating two solutions, then they are different objects")
    void givenValidInstance_whenCreatingTwoSolutions_thenTheyAreDifferentObjects() {
      DerivationTreeSolution solution1 = problem.createSolution();
      DerivationTreeSolution solution2 = problem.createSolution();
      assertTrue(solution1 != solution2);
    }
  }
}
