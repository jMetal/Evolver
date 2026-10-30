package org.uma.evolver.util.constrainthandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.constrainthandling.ConstrainedDominance;
import org.uma.jmetal.util.constrainthandling.EpsilonConstrained;
import org.uma.jmetal.util.constrainthandling.PushPullSearch;
import org.uma.jmetal.util.constrainthandling.StochasticRanking;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for the violation of a solution as a technique judges it")
class EffectiveViolationTest {

  /** A solution with two constraints (negative when violated). */
  private static DoubleSolution solution(double first, double second) {
    var solution = new DefaultDoubleSolution(List.of(Bounds.create(0.0, 1.0)), 2, 2);
    solution.constraints()[0] = first;
    solution.constraints()[1] = second;
    return solution;
  }

  @Nested
  @DisplayName("When the technique has a violation of a single solution")
  class Techniques {

    @Test
    @DisplayName("cdp gives the overall violation, and 0 for a feasible solution")
    void givenCdp_whenApplied_thenItIsTheOverallViolation() {
      // Arrange
      var violation = EffectiveViolation.of("cdp", new ConstrainedDominance<DoubleSolution>());

      // Act and Assert
      assertEquals(3.0, violation.applyAsDouble(solution(-2.0, -1.0)));
      assertEquals(0.0, violation.applyAsDouble(solution(0.0, 1.0)));
    }

    @Test
    @DisplayName("violatedConstraints gives the number of violated constraints")
    void givenViolatedConstraints_whenApplied_thenItIsTheNumberOfViolatedConstraints() {
      // Arrange
      var violation =
          EffectiveViolation.of(
              "violatedConstraints",
              new ConstrainedDominance<DoubleSolution>(
                  ConstrainedDominance.ViolationMeasure.NUMBER_OF_VIOLATED_CONSTRAINTS));

      // Act and Assert
      assertEquals(2.0, violation.applyAsDouble(solution(-0.1, -5.0)));
      assertEquals(1.0, violation.applyAsDouble(solution(-0.1, 2.0)));
    }

    @Test
    @DisplayName("epsilonConstrained gives 0 for a violation within the current epsilon")
    void givenEpsilonConstrained_whenApplied_thenAViolationWithinEpsilonIsZero() {
      // Arrange: the initial epsilon is the violation at the median of the first population
      var technique = new EpsilonConstrained<DoubleSolution>(0.5, 2.0, 0.8);
      technique.update(List.of(solution(0.0, 0.0), solution(-1.0, 0.0), solution(-4.0, 0.0)), 0.0);
      var violation = EffectiveViolation.of("epsilonConstrained", technique);
      double epsilon = technique.epsilon();

      // Act and Assert
      assertTrue(epsilon > 0.0, "epsilon " + epsilon);
      assertEquals(0.0, violation.applyAsDouble(solution(-epsilon / 2, 0.0)));
      assertEquals(2 * epsilon, violation.applyAsDouble(solution(-2 * epsilon, 0.0)));
    }

    @Test
    @DisplayName("pushPull gives 0 during the push stage, whatever the violation")
    void givenPushPullInThePushStage_whenApplied_thenItIsZero() {
      // Arrange
      var technique = new PushPullSearch<DoubleSolution>(1e-3, 0.1, 0.8, 2.0, 0.95, 0.1);
      var violation = EffectiveViolation.of("pushPull", technique);

      // Act and Assert
      assertTrue(technique.pushing());
      assertEquals(0.0, violation.applyAsDouble(solution(-10.0, -10.0)));
    }
  }

  @Test
  @DisplayName("a technique that ranks whole populations is rejected")
  void givenARankingTechnique_whenAsked_thenItIsRejected() {
    // Act and Assert
    assertThrows(
        JMetalException.class,
        () ->
            EffectiveViolation.of(
                "stochasticRanking", new StochasticRanking<DoubleSolution>(0.45)));
  }
}
