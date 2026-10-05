package org.uma.evolver.parameter.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.densityestimator.impl.AngleDensityEstimator;

@DisplayName("Unit tests for class LenientAngleDensityEstimator")
class LenientAngleDensityEstimatorTest {

  /** Solutions on the front f2 = 1 - f1, from f1 = 0 to f1 = 1. */
  private static List<DoubleSolution> front(int size) {
    List<DoubleSolution> solutions = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      DoubleSolution solution = new DefaultDoubleSolution(List.of(Bounds.create(0.0, 1.0)), 2, 0);
      double f1 = i / (double) (size - 1);
      solution.objectives()[0] = f1;
      solution.objectives()[1] = 1.0 - f1;
      solutions.add(solution);
    }
    return solutions;
  }

  @Test
  @DisplayName(
      "given a solution whose density has not been computed, when its value is asked, then it is"
          + " 0.0 instead of an exception")
  void givenASolutionWithoutDensity_whenItsValueIsAsked_thenItIsZero() {
    // Arrange
    var estimator = new LenientAngleDensityEstimator<DoubleSolution>(2);
    DoubleSolution solution = front(3).get(1);

    // Act & Assert
    assertEquals(0.0, estimator.value(solution));
  }

  @Test
  @DisplayName(
      "given a population that has not been computed, when sorted by the comparator, then it does"
          + " not fail")
  void givenAPopulationWithoutDensity_whenSorted_thenItDoesNotFail() {
    // Arrange
    var estimator = new LenientAngleDensityEstimator<DoubleSolution>(2);
    List<DoubleSolution> population = front(6);

    // Act & Assert
    assertDoesNotThrow(() -> population.sort(estimator.comparator()));
  }

  @Test
  @DisplayName(
      "given a computed list, when the values are asked, then they are those of jMetal's"
          + " estimator")
  void givenAComputedList_whenTheValuesAreAsked_thenTheyAreThoseOfJMetal() {
    // Arrange
    var lenient = new LenientAngleDensityEstimator<DoubleSolution>(2);
    var jmetal = new AngleDensityEstimator<DoubleSolution>(null, true, 2);
    List<DoubleSolution> solutions = front(7);

    // Act
    lenient.compute(solutions);
    jmetal.compute(solutions);

    // Assert
    for (DoubleSolution solution : solutions) {
      assertEquals(jmetal.value(solution), lenient.value(solution));
      assertTrue(lenient.value(solution) > 0.0);
    }
  }
}
