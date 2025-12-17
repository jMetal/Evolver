package org.uma.evolver.trainingset;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("WFG2DTrainingSet Tests")
class WFG2DTrainingSetTest {

  private TrainingSet<DoubleSolution> trainingSet;

  @BeforeEach
  void setUp() {
    trainingSet = new WFG2DTrainingSet();
  }

  @Nested
  @DisplayName("Problem list tests")
  class ProblemListTests {

    @Test
    @DisplayName("given new instance when getting problem list then returns nine WFG problems")
    void givenNewInstance_whenGettingProblemList_thenReturnsNineWFGProblems() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      assertEquals(9, problems.size());
    }

    @Test
    @DisplayName("given new instance when getting problem list then contains WFG1 to WFG9")
    void givenNewInstance_whenGettingProblemList_thenContainsWFG1ToWFG9() {
      // Arrange - done in setUp

      // Act
      List<Problem<DoubleSolution>> problems = trainingSet.problemList();

      // Assert
      List<String> problemNames =
          problems.stream().map(p -> p.getClass().getSimpleName()).toList();
      assertTrue(problemNames.contains("WFG1"));
      assertTrue(problemNames.contains("WFG2"));
      assertTrue(problemNames.contains("WFG3"));
      assertTrue(problemNames.contains("WFG4"));
      assertTrue(problemNames.contains("WFG5"));
      assertTrue(problemNames.contains("WFG6"));
      assertTrue(problemNames.contains("WFG7"));
      assertTrue(problemNames.contains("WFG8"));
      assertTrue(problemNames.contains("WFG9"));
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
    @DisplayName("given new instance when getting reference fronts then all paths contain WFG")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsContainWFG() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("WFG")));
    }

    @Test
    @DisplayName("given new instance when getting reference fronts then all paths indicate 2D")
    void givenNewInstance_whenGettingReferenceFronts_thenAllPathsIndicate2D() {
      // Arrange - done in setUp

      // Act
      List<String> fronts = trainingSet.referenceFronts();

      // Assert
      assertTrue(fronts.stream().allMatch(f -> f.contains("2D")));
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
    @DisplayName("given new instance when getting evaluations then default is 25000")
    void givenNewInstance_whenGettingEvaluations_thenDefaultIs25000() {
      // Arrange - done in setUp

      // Act
      List<Integer> evaluations = trainingSet.evaluationsToOptimize();

      // Assert
      assertTrue(evaluations.stream().allMatch(e -> e == 25000));
    }
  }

  @Nested
  @DisplayName("Name tests")
  class NameTests {

    @Test
    @DisplayName("given new instance when getting name then returns WFG2D")
    void givenNewInstance_whenGettingName_thenReturnsWFG2D() {
      // Arrange - done in setUp

      // Act
      String name = trainingSet.name();

      // Assert
      assertEquals("WFG2D", name);
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
      trainingSet.setEvaluationsToOptimize(30000);

      // Assert
      assertTrue(trainingSet.evaluationsToOptimize().stream().allMatch(e -> e == 30000));
    }

    @Test
    @DisplayName("given instance when setting evaluation list then problems have individual values")
    void givenInstance_whenSettingEvaluationList_thenProblemsHaveIndividualValues() {
      // Arrange
      List<Integer> customEvaluations =
          List.of(20000, 22000, 24000, 26000, 28000, 30000, 32000, 34000, 36000);

      // Act
      trainingSet.setEvaluationsToOptimize(customEvaluations);

      // Assert
      assertEquals(customEvaluations, trainingSet.evaluationsToOptimize());
    }
  }
}
