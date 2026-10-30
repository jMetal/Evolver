package org.uma.evolver.algorithm.rvea;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;

@DisplayName("Unit tests for class EffectiveViolationReplacement")
class EffectiveViolationReplacementTest {

  private static final FakeDoubleProblem PROBLEM = new FakeDoubleProblem(2, 2, 2);

  private static DoubleSolution solution(double first, double second) {
    DoubleSolution solution = PROBLEM.createSolution();
    solution.constraints()[0] = first;
    solution.constraints()[1] = second;
    return solution;
  }

  @Test
  @DisplayName(
      "given a violation measure, then the replacement sees it in the constraints and the"
          + " originals are returned")
  void givenAViolationMeasure_whenReplacing_thenTheReplacementSeesItAndTheOriginalsAreReturned() {
    // Arrange: a measure that counts any violation below 1 as feasible
    DoubleSolution slightly = solution(-0.5, 0.0);
    DoubleSolution largely = solution(-2.0, -1.0);
    List<double[]> seen = new ArrayList<>();
    var replacement =
        new EffectiveViolationReplacement<DoubleSolution>(
            (population, offspring) -> {
              population.forEach(solution -> seen.add(solution.constraints().clone()));
              offspring.forEach(solution -> seen.add(solution.constraints().clone()));
              return List.of(offspring.get(0));
            },
            solution -> {
              double violation =
                  -(Math.min(0, solution.constraints()[0]) + Math.min(0, solution.constraints()[1]));
              return violation < 1.0 ? 0.0 : violation;
            });

    // Act
    List<DoubleSolution> survivors = replacement.replace(List.of(slightly), List.of(largely));

    // Assert
    assertArrayEquals(new double[] {0.0, 0.0}, seen.get(0));
    assertArrayEquals(new double[] {-3.0, 0.0}, seen.get(1));
    assertEquals(1, survivors.size());
    assertSame(largely, survivors.get(0));
    assertArrayEquals(new double[] {-2.0, -1.0}, largely.constraints());
  }

  @Test
  @DisplayName(
      "given a violation precomputed by the problem, then the replacement sees that of the"
          + " technique")
  void givenAPrecomputedViolation_whenReplacing_thenTheReplacementSeesThatOfTheTechnique() {
    // Arrange: the problem precomputes a violation of 3, which the technique treats as feasible
    DoubleSolution solution = solution(2.0, 1.0);
    ConstraintHandling.overallConstraintViolationDegree(solution, -3.0);
    ConstraintHandling.numberOfViolatedConstraints(solution, 2);
    List<Double> violations = new ArrayList<>();
    List<Integer> violated = new ArrayList<>();
    var replacement =
        new EffectiveViolationReplacement<DoubleSolution>(
            (population, offspring) -> {
              violations.add(ConstraintHandling.overallConstraintViolationDegree(population.get(0)));
              violated.add(ConstraintHandling.numberOfViolatedConstraints(population.get(0)));
              return population;
            },
            candidate -> 0.0);

    // Act
    replacement.replace(List.of(solution), List.of());

    // Assert
    assertEquals(List.of(0.0), violations);
    assertEquals(List.of(0), violated);
    assertEquals(-3.0, ConstraintHandling.overallConstraintViolationDegree(solution));
  }
}
