package org.uma.evolver.trainingset;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.util.trainingset.RE3DTrainingSet;
import org.uma.evolver.util.trainingset.TrainingSet;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("RE3DTrainingSet Tests")
class RE3DTrainingSetTest {

  private TrainingSet<DoubleSolution> trainingSet;

  @BeforeEach
  void setUp() {
    trainingSet = new RE3DTrainingSet();
  }

  @Nested
  @DisplayName("Problem list tests")
  class ProblemListTests {

    @Test
    @DisplayName("given new instance when getting problem list then returns seven RE problems")
    void givenNewInstance_whenGettingProblemList_thenReturnsSevenREProblems() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertEquals(7, problems.size());
    }

    @Test
    @DisplayName("given new instance when getting problem list then contains RE31 to RE37")
    void givenNewInstance_whenGettingProblemList_thenContainsRE31ToRE37() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      List<String> problemNames =
          problems.stream().map(p -> p.getClass().getSimpleName()).toList();
      assertTrue(problemNames.contains("RE31"));
      assertTrue(problemNames.contains("RE32"));
      assertTrue(problemNames.contains("RE33"));
      assertTrue(problemNames.contains("RE34"));
      assertTrue(problemNames.contains("RE35"));
      assertTrue(problemNames.contains("RE36"));
      assertTrue(problemNames.contains("RE37"));
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
    @DisplayName("given new instance when getting reference fronts then all paths contain RE3")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsContainRE3() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("RE3")));
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
    @DisplayName("given new instance when getting evaluations then default is 7000")
    void givenNewInstance_whenGettingEvaluations_thenDefaultIs7000() {
      // Arrange - done in setUp

      // Act
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();

      // Assert
      assertTrue(evaluations.stream().allMatch(e -> e == 7000));
    }
  }

  @Nested
  @DisplayName("Name tests")
  class NameTests {

    @Test
    @DisplayName("given new instance when getting name then returns RE3D")
    void givenNewInstance_whenGettingName_thenReturnsRE3D() {
      // Arrange - done in setUp

      // Act
      String name = trainingSet.name();

      // Assert
      assertEquals("RE3D", name);
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
      trainingSet.setEvaluationsToOptimize(15000);

      // Assert
      assertTrue(trainingSet.evaluationsToOptimize().stream().allMatch(e -> e == 15000));
    }

    @Test
    @DisplayName("given instance when setting evaluation list then problems have individual values")
    void givenInstance_whenSettingEvaluationList_thenProblemsHaveIndividualValues() {
      // Arrange
      List<Integer> customEvaluations = List.of(5000, 6000, 7000, 8000, 9000, 10000, 11000);

      // Act
      trainingSet.setEvaluationsToOptimize(customEvaluations);

      // Assert
      assertEquals(customEvaluations, trainingSet.evaluationsToOptimize());
    }
  }
}
