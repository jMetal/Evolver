package org.uma.evolver.trainingset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.util.trainingset.AbstractTrainingSet;
import org.uma.evolver.util.trainingset.TrainingSet;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("AbstractTrainingSet Tests")
class AbstractTrainingSetTest {

  /** Concrete implementation for testing the abstract class. */
  private static class TestTrainingSet extends AbstractTrainingSet<DoubleSolution> {
    public TestTrainingSet(
        List<Problem<DoubleSolution>> problemList,
        List<String> referenceFronts,
        List<Integer> evaluationsToOptimize,
        String name) {
      super(problemList, referenceFronts, evaluationsToOptimize, name);
    }
  }

  @SuppressWarnings("unchecked")
  private Problem<DoubleSolution> createMockProblem() {
    return mock(Problem.class);
  }

  @Nested
  @DisplayName("Constructor validation tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("given null problem list when constructing then throws IllegalArgumentException")
    void givenNullProblemList_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<String> referenceFronts = List.of("front1.csv");
      List<Integer> evaluations = List.of(1000);

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(null, referenceFronts, evaluations, "Test"));
    }

    @Test
    @DisplayName("given empty problem list when constructing then throws IllegalArgumentException")
    void givenEmptyProblemList_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of();
      List<String> referenceFronts = List.of();
      List<Integer> evaluations = List.of();

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(problems, referenceFronts, evaluations, "Test"));
    }

    @Test
    @DisplayName(
        "given mismatched reference fronts size when constructing then throws IllegalArgumentException")
    void givenMismatchedReferenceFrontsSize_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(createMockProblem(), createMockProblem());
      List<String> referenceFronts = List.of("front1.csv"); // Only one front for two problems
      List<Integer> evaluations = List.of(1000, 2000);

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(problems, referenceFronts, evaluations, "Test"));
    }

    @Test
    @DisplayName(
        "given mismatched evaluations size when constructing then throws IllegalArgumentException")
    void givenMismatchedEvaluationsSize_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(createMockProblem(), createMockProblem());
      List<String> referenceFronts = List.of("front1.csv", "front2.csv");
      List<Integer> evaluations = List.of(1000); // Only one evaluation for two problems

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(problems, referenceFronts, evaluations, "Test"));
    }

    @Test
    @DisplayName("given null name when constructing then throws IllegalArgumentException")
    void givenNullName_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(createMockProblem());
      List<String> referenceFronts = List.of("front1.csv");
      List<Integer> evaluations = List.of(1000);

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(problems, referenceFronts, evaluations, null));
    }

    @Test
    @DisplayName("given blank name when constructing then throws IllegalArgumentException")
    void givenBlankName_whenConstructing_thenThrowsIllegalArgumentException() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(createMockProblem());
      List<String> referenceFronts = List.of("front1.csv");
      List<Integer> evaluations = List.of(1000);

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestTrainingSet(problems, referenceFronts, evaluations, "   "));
    }

    @Test
    @DisplayName("given valid parameters when constructing then creates instance successfully")
    void givenValidParameters_whenConstructing_thenCreatesInstanceSuccessfully() {
      // Arrange
      List<Problem<DoubleSolution>> problems = List.of(createMockProblem(), createMockProblem());
      List<String> referenceFronts = List.of("front1.csv", "front2.csv");
      List<Integer> evaluations = List.of(1000, 2000);

      // Act
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(problems, referenceFronts, evaluations, "TestSet");

      // Assert
      assertNotNull(trainingSet);
      assertEquals("TestSet", trainingSet.name());
    }
  }

  @Nested
  @DisplayName("Getter methods tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("given valid training set when getting problem list then returns correct list")
    void givenValidTrainingSet_whenGettingProblemList_thenReturnsCorrectList() {
      // Arrange
      Problem<DoubleSolution> problem1 = createMockProblem();
      Problem<DoubleSolution> problem2 = createMockProblem();
      List<Problem<DoubleSolution>> problems = List.of(problem1, problem2);
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              problems, List.of("front1.csv", "front2.csv"), List.of(1000, 2000), "Test");

      // Act
      List<Problem<DoubleSolution>> result = trainingSet.problemList();

      // Assert
      assertEquals(2, result.size());
      assertTrue(result.contains(problem1));
      assertTrue(result.contains(problem2));
    }

    @Test
    @DisplayName(
        "given valid training set when getting reference fronts then returns correct list")
    void givenValidTrainingSet_whenGettingReferenceFronts_thenReturnsCorrectList() {
      // Arrange
      List<String> expectedFronts = List.of("front1.csv", "front2.csv");
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              expectedFronts,
              List.of(1000, 2000),
              "Test");

      // Act
      List<String> result = trainingSet.referenceFronts();

      // Assert
      assertEquals(expectedFronts, result);
    }

    @Test
    @DisplayName(
        "given valid training set when getting evaluations to optimize then returns correct list")
    void givenValidTrainingSet_whenGettingEvaluationsToOptimize_thenReturnsCorrectList() {
      // Arrange
      List<Integer> expectedEvaluations = List.of(1000, 2000);
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv"),
              expectedEvaluations,
              "Test");

      // Act
      List<Integer> result = trainingSet.evaluationsToOptimize();

      // Assert
      assertEquals(expectedEvaluations, result);
    }

    @Test
    @DisplayName("given valid training set when getting name then returns correct name")
    void givenValidTrainingSet_whenGettingName_thenReturnsCorrectName() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()),
              List.of("front1.csv"),
              List.of(1000),
              "MyTrainingSet");

      // Act
      String result = trainingSet.name();

      // Assert
      assertEquals("MyTrainingSet", result);
    }
  }

  @Nested
  @DisplayName("setEvaluationsToOptimize with single value tests")
  class SetEvaluationsSingleValueTests {

    @Test
    @DisplayName(
        "given valid evaluation value when setting evaluations then all problems have same value")
    void givenValidEvaluationValue_whenSettingEvaluations_thenAllProblemsHaveSameValue() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv", "front3.csv"),
              List.of(1000, 2000, 3000),
              "Test");

      // Act
      trainingSet.setEvaluationsToOptimize(5000);

      // Assert
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();
      assertEquals(3, evaluations.size());
      assertTrue(evaluations.stream().allMatch(e -> e == 5000));
    }

    @Test
    @DisplayName("given zero evaluation value when setting evaluations then throws exception")
    void givenZeroEvaluationValue_whenSettingEvaluations_thenThrowsException() {
      // Arrange
      AbstractTrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()), List.of("front1.csv"), List.of(1000), "Test");

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> trainingSet.setEvaluationsToOptimize(0));
    }

    @Test
    @DisplayName("given negative evaluation value when setting evaluations then throws exception")
    void givenNegativeEvaluationValue_whenSettingEvaluations_thenThrowsException() {
      // Arrange
      AbstractTrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()), List.of("front1.csv"), List.of(1000), "Test");

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class, () -> trainingSet.setEvaluationsToOptimize(-100));
    }

    @Test
    @DisplayName("given valid value when setting evaluations then returns same instance")
    void givenValidValue_whenSettingEvaluations_thenReturnsSameInstance() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()), List.of("front1.csv"), List.of(1000), "Test");

      // Act
      TrainingSet<DoubleSolution> result = trainingSet.setEvaluationsToOptimize(5000);

      // Assert
      assertSame(trainingSet, result);
    }
  }

  @Nested
  @DisplayName("setEvaluationsToOptimize with list tests")
  class SetEvaluationsListTests {

    @Test
    @DisplayName(
        "given valid evaluation list when setting evaluations then problems have corresponding values")
    void givenValidEvaluationList_whenSettingEvaluations_thenProblemsHaveCorrespondingValues() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv", "front3.csv"),
              List.of(1000, 1000, 1000),
              "Test");
      List<Integer> newEvaluations = List.of(5000, 10000, 15000);

      // Act
      trainingSet.setEvaluationsToOptimize(newEvaluations);

      // Assert
      assertEquals(newEvaluations, trainingSet.evaluationsToOptimize());
    }

    @Test
    @DisplayName("given null list when setting evaluations then throws exception")
    void givenNullList_whenSettingEvaluations_thenThrowsException() {
      // Arrange
      AbstractTrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()), List.of("front1.csv"), List.of(1000), "Test");

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> trainingSet.setEvaluationsToOptimize((List<Integer>) null));
    }

    @Test
    @DisplayName("given mismatched list size when setting evaluations then throws exception")
    void givenMismatchedListSize_whenSettingEvaluations_thenThrowsException() {
      // Arrange
      AbstractTrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv"),
              List.of(1000, 2000),
              "Test");

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> trainingSet.setEvaluationsToOptimize(List.of(5000))); // Only one value for two
    }

    @Test
    @DisplayName(
        "given list with non-positive value when setting evaluations then throws exception")
    void givenListWithNonPositiveValue_whenSettingEvaluations_thenThrowsException() {
      // Arrange
      AbstractTrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv"),
              List.of(1000, 2000),
              "Test");

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> trainingSet.setEvaluationsToOptimize(List.of(5000, -100)));
    }

    @Test
    @DisplayName("given valid list when setting evaluations then returns same instance")
    void givenValidList_whenSettingEvaluations_thenReturnsSameInstance() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv"),
              List.of(1000, 2000),
              "Test");

      // Act
      TrainingSet<DoubleSolution> result =
          trainingSet.setEvaluationsToOptimize(List.of(5000, 10000));

      // Assert
      assertSame(trainingSet, result);
    }
  }

  @Nested
  @DisplayName("Immutability tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("given training set when modifying original problem list then internal list unchanged")
    void givenTrainingSet_whenModifyingOriginalProblemList_thenInternalListUnchanged() {
      // Arrange
      Problem<DoubleSolution> problem1 = createMockProblem();
      Problem<DoubleSolution> problem2 = createMockProblem();
      java.util.ArrayList<Problem<DoubleSolution>> mutableProblems = new java.util.ArrayList<>();
      mutableProblems.add(problem1);
      mutableProblems.add(problem2);

      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              mutableProblems, List.of("front1.csv", "front2.csv"), List.of(1000, 2000), "Test");

      // Act
      mutableProblems.clear();

      // Assert
      assertEquals(2, trainingSet.problemList().size());
    }

    @Test
    @DisplayName("given training set when trying to modify returned list then throws exception")
    void givenTrainingSet_whenTryingToModifyReturnedList_thenThrowsException() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem()), List.of("front1.csv"), List.of(1000), "Test");

      // Act & Assert
      assertThrows(
          UnsupportedOperationException.class, () -> trainingSet.problemList().add(createMockProblem()));
    }
  }

  @Nested
  @DisplayName("Method chaining tests")
  class MethodChainingTests {

    @Test
    @DisplayName("given training set when chaining setEvaluations calls then works correctly")
    void givenTrainingSet_whenChainingSetEvaluationsCalls_thenWorksCorrectly() {
      // Arrange
      TrainingSet<DoubleSolution> trainingSet =
          new TestTrainingSet(
              List.of(createMockProblem(), createMockProblem()),
              List.of("front1.csv", "front2.csv"),
              List.of(1000, 2000),
              "Test");

      // Act
      TrainingSet<DoubleSolution> result =
          trainingSet.setEvaluationsToOptimize(5000).setEvaluationsToOptimize(List.of(8000, 9000));

      // Assert
      assertSame(trainingSet, result);
      assertEquals(List.of(8000, 9000), trainingSet.evaluationsToOptimize());
    }
  }
}
