package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.CauchySolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.OppositionBasedSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.SobolSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

/**
 * Unit tests for {@link CreateInitialSolutionsDoubleParameter}.
 * Tests all initialization strategies available in jMetal 7.0 including
 * default, scatterSearch, latinHypercubeSampling, sobol, cauchy, and oppositionBased.
 */
@DisplayName("CreateInitialSolutionsDoubleParameter Tests")
class CreateInitialSolutionsDoubleParameterTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTestCases {

    @Test
    @DisplayName("Given valid strategies list, when creating parameter, then does not throw exception")
    void givenValidStrategiesList_whenCreatingParameter_thenDoesNotThrowException() {
      // Given
      List<String> validStrategies = List.of(
          CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY,
          CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH,
          CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING,
          CreateInitialSolutionsDoubleParameter.SOBOL,
          CreateInitialSolutionsDoubleParameter.CAUCHY,
          CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED
      );

      // When & Then
      assertDoesNotThrow(() -> new CreateInitialSolutionsDoubleParameter(validStrategies));
    }

    @Test
    @DisplayName("Given subset of valid strategies, when creating parameter, then does not throw exception")
    void givenSubsetOfValidStrategies_whenCreatingParameter_thenDoesNotThrowException() {
      // Given
      List<String> strategies = List.of(
          CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY,
          CreateInitialSolutionsDoubleParameter.SOBOL
      );

      // When & Then
      assertDoesNotThrow(() -> new CreateInitialSolutionsDoubleParameter(strategies));
    }

    @Test
    @DisplayName("Given custom name and valid strategies, when creating parameter, then does not throw exception")
    void givenCustomNameAndValidStrategies_whenCreatingParameter_thenDoesNotThrowException() {
      // Given
      String customName = "customInitialization";
      List<String> strategies = List.of(
          CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY,
          CreateInitialSolutionsDoubleParameter.SOBOL
      );

      // When & Then
      assertDoesNotThrow(() -> new CreateInitialSolutionsDoubleParameter(customName, strategies));
    }
  }

  @Nested
  @DisplayName("Default Strategy Tests")
  class DefaultStrategyTestCases {

    @Test
    @DisplayName("Given default strategy, when getting solutions creation, then return RandomSolutionsCreation")
    void givenDefaultStrategy_whenGettingSolutionsCreation_thenReturnRandomSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(RandomSolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given default strategy, when creating solutions, then return correct population size")
    void givenDefaultStrategy_whenCreatingSolutions_thenReturnCorrectPopulationSize() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 50;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
      List<DoubleSolution> solutions = strategy.create();

      // Then
      assertEquals(populationSize, solutions.size());
    }
  }

  @Nested
  @DisplayName("Scatter Search Strategy Tests")
  class ScatterSearchStrategyTestCases {

    @Test
    @DisplayName("Given scatter search strategy, when getting solutions creation, then return ScatterSearchSolutionsCreation")
    void givenScatterSearchStrategy_whenGettingSolutionsCreation_thenReturnScatterSearchSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(ScatterSearchSolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given scatter search with custom reference set size, when getting solutions creation, then return ScatterSearchSolutionsCreation")
    void givenScatterSearchWithCustomReferenceSetSize_whenGettingSolutionsCreation_thenReturnScatterSearchSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;
      int referenceSetSize = 10;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize, referenceSetSize);

      // Then
      assertInstanceOf(ScatterSearchSolutionsCreation.class, strategy);
    }
  }

  @Nested
  @DisplayName("Latin Hypercube Sampling Strategy Tests")
  class LatinHypercubeSamplingStrategyTestCases {

    @Test
    @DisplayName("Given latin hypercube sampling strategy, when getting solutions creation, then return LatinHypercubeSamplingSolutionsCreation")
    void givenLatinHypercubeSamplingStrategy_whenGettingSolutionsCreation_thenReturnLatinHypercubeSamplingSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(LatinHypercubeSamplingSolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given latin hypercube sampling strategy, when creating solutions, then return correct population size")
    void givenLatinHypercubeSamplingStrategy_whenCreatingSolutions_thenReturnCorrectPopulationSize() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 50;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
      List<DoubleSolution> solutions = strategy.create();

      // Then
      assertEquals(populationSize, solutions.size());
    }
  }

  @Nested
  @DisplayName("Sobol Strategy Tests")
  class SobolStrategyTestCases {

    @Test
    @DisplayName("Given sobol strategy, when getting solutions creation, then return SobolSolutionsCreation")
    void givenSobolStrategy_whenGettingSolutionsCreation_thenReturnSobolSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.SOBOL));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.SOBOL});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(SobolSolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given sobol strategy, when creating solutions, then return correct population size")
    void givenSobolStrategy_whenCreatingSolutions_thenReturnCorrectPopulationSize() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.SOBOL));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.SOBOL});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 50;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
      List<DoubleSolution> solutions = strategy.create();

      // Then
      assertEquals(populationSize, solutions.size());
    }
  }

  @Nested
  @DisplayName("Cauchy Strategy Tests")
  class CauchyStrategyTestCases {

    @Test
    @DisplayName("Given cauchy strategy, when getting solutions creation, then return CauchySolutionsCreation")
    void givenCauchyStrategy_whenGettingSolutionsCreation_thenReturnCauchySolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.CAUCHY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.CAUCHY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(CauchySolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given cauchy strategy, when creating solutions, then return correct population size")
    void givenCauchyStrategy_whenCreatingSolutions_thenReturnCorrectPopulationSize() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.CAUCHY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.CAUCHY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 50;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
      List<DoubleSolution> solutions = strategy.create();

      // Then
      assertEquals(populationSize, solutions.size());
    }
  }

  @Nested
  @DisplayName("Opposition Based Strategy Tests")
  class OppositionBasedStrategyTestCases {

    @Test
    @DisplayName("Given opposition based strategy, when getting solutions creation, then return OppositionBasedSolutionsCreation")
    void givenOppositionBasedStrategy_whenGettingSolutionsCreation_thenReturnOppositionBasedSolutionsCreation() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 100;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);

      // Then
      assertInstanceOf(OppositionBasedSolutionsCreation.class, strategy);
    }

    @Test
    @DisplayName("Given opposition based strategy, when creating solutions, then return correct population size")
    void givenOppositionBasedStrategy_whenCreatingSolutions_thenReturnCorrectPopulationSize() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 50;

      // When
      SolutionsCreation<DoubleSolution> strategy =
          parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
      List<DoubleSolution> solutions = strategy.create();

      // Then
      assertEquals(populationSize, solutions.size());
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTestCases {

    @Test
    @DisplayName("Given unknown strategy, when parsing parameter, then throw JMetalException")
    void givenUnknownStrategy_whenParsingParameter_thenThrowJMetalException() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));

      // When & Then
      assertThrows(JMetalException.class,
          () -> parameter.parse(new String[]{"--createInitialSolutions", "unknownStrategy"}));
    }

    @Test
    @DisplayName("Given null problem, when getting solutions creation, then throw NullParameterException")
    void givenNullProblem_whenGettingSolutionsCreation_thenThrowNullParameterException() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY});
      Problem<DoubleSolution> problem = null;
      int populationSize = 100;

      // When & Then
      assertThrows(NullParameterException.class,
          () -> parameter.getCreateInitialSolutionsStrategy(problem, populationSize));
    }

    @Test
    @DisplayName("Given zero population size, when getting solutions creation, then throw InvalidConditionException")
    void givenZeroPopulationSize_whenGettingSolutionsCreation_thenThrowIllegalArgumentException() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = 0;

      // When & Then
      assertThrows(InvalidConditionException.class,
          () -> parameter.getCreateInitialSolutionsStrategy(problem, populationSize));
    }

    @Test
    @DisplayName("Given negative population size, when getting solutions creation, then throw InvalidConditionException")
    void givenNegativePopulationSize_whenGettingSolutionsCreation_thenThrowIllegalArgumentException() {
      // Given
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(List.of(
              CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY));
      parameter.parse(new String[]{"--createInitialSolutions", CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY});
      DoubleProblem problem = new FakeDoubleProblem(3, 2, 0);
      int populationSize = -10;

      // When & Then
      assertThrows(InvalidConditionException.class,
          () -> parameter.getCreateInitialSolutionsStrategy(problem, populationSize));
    }
  }

  @Nested
  @DisplayName("All Strategies Integration Tests")
  class AllStrategiesIntegrationTestCases {

    @Test
    @DisplayName("Given all strategies, when creating solutions with each, then all work correctly")
    void givenAllStrategies_whenCreatingSolutionsWithEach_thenAllWorkCorrectly() {
      // Given
      List<String> allStrategies = List.of(
          CreateInitialSolutionsDoubleParameter.DEFAULT_STRATEGY,
          CreateInitialSolutionsDoubleParameter.SCATTER_SEARCH,
          CreateInitialSolutionsDoubleParameter.LATIN_HYPERCUBE_SAMPLING,
          CreateInitialSolutionsDoubleParameter.SOBOL,
          CreateInitialSolutionsDoubleParameter.CAUCHY,
          CreateInitialSolutionsDoubleParameter.OPPOSITION_BASED
      );
      CreateInitialSolutionsDoubleParameter parameter =
          new CreateInitialSolutionsDoubleParameter(allStrategies);
      DoubleProblem problem = new FakeDoubleProblem(5, 2, 0);
      int populationSize = 100;

      // When & Then
      for (String strategy : allStrategies) {
        parameter.parse(new String[]{"--createInitialSolutions", strategy});
        SolutionsCreation<DoubleSolution> solutionsCreation =
            parameter.getCreateInitialSolutionsStrategy(problem, populationSize);
        List<DoubleSolution> solutions = solutionsCreation.create();

        assertNotNull(solutions, "Solutions should not be null for strategy: " + strategy);
        assertEquals(populationSize, solutions.size(),
            "Population size should match for strategy: " + strategy);
        assertTrue(solutions.stream().allMatch(s -> s.variables().size() == 5),
            "All solutions should have correct number of variables for strategy: " + strategy);
      }
    }
  }
}
