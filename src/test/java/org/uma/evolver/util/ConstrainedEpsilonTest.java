package org.uma.evolver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.qualityindicator.impl.Epsilon;

@DisplayName("Unit tests for class ConstrainedEpsilon")
class ConstrainedEpsilonTest {

  private static final double TOLERANCE = 1e-12;
  private static final double[][] REFERENCE_FRONT = {{0.0, 1.0}, {0.5, 0.5}, {1.0, 0.0}};

  private ConstrainedEpsilon indicator;

  @BeforeEach
  void setUp() {
    indicator = new ConstrainedEpsilon();
    indicator.referenceFront(REFERENCE_FRONT);
  }

  @Test
  @DisplayName("given feasible points, when computed, then the value is their EP")
  void givenFeasiblePoints_whenComputed_thenTheValueIsTheirEpsilon() {
    // Arrange
    double[][] front = {{0.1, 1.1}, {0.6, 0.6}, {1.1, 0.1}};

    // Act and Assert
    assertEquals(0.1, indicator.compute(front), TOLERANCE);
    assertEquals(new Epsilon(REFERENCE_FRONT).compute(front), indicator.compute(front), TOLERANCE);
  }

  @Test
  @DisplayName("given infeasible points too, when computed, then only the feasible ones count")
  void givenInfeasiblePointsToo_whenComputed_thenOnlyTheFeasibleOnesCount() {
    // Arrange: the infeasible points are the reference front itself
    double[][] front = {{0.0, 1.0}, {0.5, 0.5}, {1.0, 0.0}, {0.2, 0.9}};

    // Act and Assert: from (0.2, 0.9) to (1, 0) the second objective needs 0.9
    assertEquals(0.9, indicator.compute(front, new double[] {3.0, 1.0, 2.0, 0.0}), TOLERANCE);
  }

  @Test
  @DisplayName("given a distant feasible front, when computed, then the value is the threshold")
  void givenADistantFeasibleFront_whenComputed_thenTheValueIsTheThreshold() {
    // Act and Assert
    assertEquals(1.0, indicator.compute(new double[][] {{7.0, 8.0}}));
  }

  @Test
  @DisplayName("given no feasible points, when computed, then it is 1 plus the least violation")
  void givenNoFeasiblePoints_whenComputed_thenItIsTheThresholdPlusTheLeastViolation() {
    // Act and Assert
    double[][] front = {{0.0, 1.0}, {1.0, 0.0}};
    assertEquals(1.25, indicator.compute(front, new double[] {0.5, 0.25}), TOLERANCE);
  }

  @Test
  @DisplayName("given other scales, when computed, then the objectives are normalized")
  void givenOtherScales_whenComputed_thenTheObjectivesAreNormalized() {
    // Arrange: the same fronts with objectives in [10, 110] and [-2, 0]
    indicator.referenceFront(new double[][] {{10.0, 0.0}, {60.0, -1.0}, {110.0, -2.0}});

    // Act and Assert
    double[][] front = {{20.0, 0.2}, {70.0, -0.8}, {120.0, -1.8}};
    assertEquals(0.1, indicator.compute(front), TOLERANCE);
  }

  @Test
  @DisplayName("given invalid arguments, when computed, then it fails")
  void givenInvalidArguments_whenComputed_thenItFails() {
    // Arrange
    double[][] front = {{0.0, 1.0}};
    var withoutReferenceFront = new ConstrainedEpsilon();

    // Act and Assert
    assertThrows(RuntimeException.class, () -> withoutReferenceFront.compute(front));
    assertThrows(RuntimeException.class, () -> indicator.compute(new double[0][]));
    assertThrows(RuntimeException.class, () -> indicator.compute(front, new double[2]));
    assertThrows(RuntimeException.class, () -> indicator.compute(new double[][] {{1.0, 2.0, 3.0}}));
  }
}
