package org.uma.evolver.trainingset;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("ZDTTrainingSet Tests")
class ZDTTrainingSetTest {

  private TrainingSet<DoubleSolution> trainingSet;

  @BeforeEach
  void setUp() {
    trainingSet = new ZDTTrainingSet();
  }

  @Nested
  @DisplayName("Problem list tests")
  class ProblemListTests {

    @Test
    @DisplayName("given new instance when getting problem list then returns five ZDT problems")
    void givenNewInstance_whenGettingProblemList_thenReturnsFiveZDTProblems() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertEquals(5, problems.size());
    }

    @Test
    @DisplayName(
        "given new instance when getting problem list then contains ZDT1 ZDT2 ZDT3 ZDT4 ZDT6")
    void givenNewInstance_whenGettingProblemList_thenContainsExpectedZDTVariants() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      List<String> problemNames =
          problems.stream().map(p -> p.getClass().getSimpleName()).toList();
      assertTrue(problemNames.contains("ZDT1"));
      assertTrue(problemNames.contains("ZDT2"));
      assertTrue(problemNames.contains("ZDT3"));
      assertTrue(problemNames.contains("ZDT4"));
      assertTrue(problemNames.contains("ZDT6"));
      assertFalse(problemNames.contains("ZDT5")); // ZDT5 is binary, not included
    }

    @Test
    @DisplayName("given new instance when getting problems then all are bi-objective")
    void givenNewInstance_whenGettingProblems_thenAllAreBiObjective() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertTrue(problems.stream().allMatch(p -> p.numberOfObjectives() == 2));
    }
  }

  @Nested
  @DisplayName("Reference fronts tests")
  class ReferenceFrontsTests {

    @Test
    @DisplayName(
        "given new instance when getting reference fronts then returns same count as problems")
    void givenNewInstance_whenGettingReferenceFronts_thenReturnsSameCountAsProblems() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertEquals(trainingSet.problemList().size(), fronts.size());
    }

    @Test
    @DisplayName("given new instance when getting reference fronts then all paths contain ZDT")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsContainZDT() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("ZDT")));
    }

    @Test
    @DisplayName("given new instance when getting reference fronts then paths are csv files")
    void givenNewInstance_whenGettingReferenceFronts_thenPathsAreCsvFiles() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.endsWith(".csv")));
    }
  }

  @Nested
  @DisplayName("Evaluations tests")
  class EvaluationsTests {

    @Test
    @DisplayName(
        "given new instance when getting evaluations then returns same count as problems")
    void givenNewInstance_whenGettingEvaluations_thenReturnsSameCountAsProblems() {
      // Arrange - done in setUp

      // Act
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();

      // Assert
      assertEquals(trainingSet.problemList().size(), evaluations.size());
    }

    @Test
    @DisplayName("given new instance when getting evaluations then default is 10000")
    void givenNewInstance_whenGettingEvaluations_thenDefaultIs10000() {
      // Arrange - done in setUp

      // Act
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();

      // Assert
      assertTrue(evaluations.stream().allMatch(e -> e == 10000));
    }
  }

  @Nested
  @DisplayName("Name tests")
  class NameTests {

    @Test
    @DisplayName("given new instance when getting name then returns ZDT")
    void givenNewInstance_whenGettingName_thenReturnsZDT() {
      // Arrange - done in setUp

      // Act
      String name = trainingSet.name();

      // Assert
      assertEquals("ZDT", name);
    }
  }

  @Nested
  @DisplayName("Evaluation modification tests")
  class EvaluationModificationTests {

    @Test
    @DisplayName("given instance when setting single evaluation then all problems updated")
    void givenInstance_whenSettingSingleEvaluation_thenAllProblemsUpdated() {
      // Arrange - done in setUp

      // Act
      trainingSet.setEvaluationsToOptimize(25000);

      // Assert
      assertTrue(trainingSet.evaluationsToOptimize().stream().allMatch(e -> e == 25000));
    }

    @Test
    @DisplayName("given instance when setting evaluation list then problems have individual values")
    void givenInstance_whenSettingEvaluationList_thenProblemsHaveIndividualValues() {
      // Arrange
      List<Integer> customEvaluations = List.of(8000, 10000, 12000, 15000, 20000);

      // Act
      trainingSet.setEvaluationsToOptimize(customEvaluations);

      // Assert
      assertEquals(customEvaluations, trainingSet.evaluationsToOptimize());
    }
  }
}
