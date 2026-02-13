package org.uma.evolver.trainingset;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.util.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.util.trainingset.TrainingSet;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("DTLZ3DTrainingSet Tests")
class DTLZ3DTrainingSetTest {

  private TrainingSet<DoubleSolution> trainingSet;

  @BeforeEach
  void setUp() {
    trainingSet = new DTLZ3DTrainingSet();
  }

  @Nested
  @DisplayName("Problem list tests")
  class ProblemListTests {

    @Test
    @DisplayName("given new instance when getting problem list then returns seven DTLZ problems")
    void givenNewInstance_whenGettingProblemList_thenReturnsSevenDTLZProblems() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertEquals(7, problems.size());
    }

    @Test
    @DisplayName("given new instance when getting problem list then contains all DTLZ variants")
    void givenNewInstance_whenGettingProblemList_thenContainsAllDTLZVariants() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      List<String> problemNames =
          problems.stream().map(p -> p.getClass().getSimpleName()).toList();
      assertTrue(problemNames.contains("DTLZ1"));
      assertTrue(problemNames.contains("DTLZ2"));
      assertTrue(problemNames.contains("DTLZ3"));
      assertTrue(problemNames.contains("DTLZ4"));
      assertTrue(problemNames.contains("DTLZ5"));
      assertTrue(problemNames.contains("DTLZ6"));
      assertTrue(problemNames.contains("DTLZ7"));
    }

    @Test
    @DisplayName("given new instance when getting problems then all have three objectives")
    void givenNewInstance_whenGettingProblems_thenAllHaveThreeObjectives() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertTrue(problems.stream().allMatch(p -> p.numberOfObjectives() == 3));
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
    @DisplayName("given new instance when getting reference fronts then all paths contain DTLZ")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsContainDTLZ() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("DTLZ")));
    }

    @Test
    @DisplayName("given new instance when getting reference fronts then all paths indicate 3D")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsIndicate3D() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("3D")));
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
    @DisplayName("given new instance when getting evaluations then default is 16000")
    void givenNewInstance_whenGettingEvaluations_thenDefaultIs16000() {
      // Arrange - done in setUp

      // Act
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();

      // Assert
      assertTrue(evaluations.stream().allMatch(e -> e == 16000));
    }
  }

  @Nested
  @DisplayName("Name tests")
  class NameTests {

    @Test
    @DisplayName("given new instance when getting name then returns DTLZ3D")
    void givenNewInstance_whenGettingName_thenReturnsDTLZ3D() {
      // Arrange - done in setUp

      // Act
      String name = trainingSet.name();

      // Assert
      assertEquals("DTLZ3D", name);
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
      trainingSet.setEvaluationsToOptimize(20000);

      // Assert
      assertTrue(trainingSet.evaluationsToOptimize().stream().allMatch(e -> e == 20000));
    }

    @Test
    @DisplayName("given instance when setting evaluation list then problems have individual values")
    void givenInstance_whenSettingEvaluationList_thenProblemsHaveIndividualValues() {
      // Arrange
      List<Integer> customEvaluations = List.of(10000, 12000, 14000, 16000, 18000, 20000, 22000);

      // Act
      trainingSet.setEvaluationsToOptimize(customEvaluations);

      // Assert
      assertEquals(customEvaluations, trainingSet.evaluationsToOptimize());
    }
  }
}
