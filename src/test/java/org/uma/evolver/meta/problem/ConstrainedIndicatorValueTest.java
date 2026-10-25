package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for the value of an indicator on a training set with constraints")
class ConstrainedIndicatorValueTest {

  @Nested
  @DisplayName("When the threshold of an indicator is asked")
  class Threshold {

    @Test
    @DisplayName("NHV, EP and IGD+ have 1, and HV- has 0")
    void givenTheIndicators_whenAsked_thenTheirThresholdsAreTheWorstValues() {
      // Act and Assert
      assertEquals(1.0, ConstrainedIndicatorValue.threshold(new NormalizedHypervolume()));
      assertEquals(1.0, ConstrainedIndicatorValue.threshold(new Epsilon()));
      assertEquals(1.0, ConstrainedIndicatorValue.threshold(new InvertedGenerationalDistancePlus()));
      assertEquals(0.0, ConstrainedIndicatorValue.threshold(new HypervolumeMinus()));
    }

    @Test
    @DisplayName("an indicator without a known threshold is rejected")
    void givenAnUnknownIndicator_whenAsked_thenItFails() {
      // Act and Assert
      assertThrows(
          JMetalException.class,
          () -> ConstrainedIndicatorValue.threshold(new GenerationalDistance()));
    }
  }

  @Nested
  @DisplayName("When violations are scaled")
  class Scaling {

    @Test
    @DisplayName("the scale is the power of ten above the median violation")
    void givenASample_whenScaled_thenTheScaleIsThePowerOfTenAboveItsMedian() {
      // Act and Assert
      assertEquals(1.0, ConstrainedIndicatorValue.scale(new double[] {0.2, 0.37, 0.9}));
      assertEquals(1000.0, ConstrainedIndicatorValue.scale(new double[] {12.0, 250.0, 9000.0}));
      assertEquals(1.0, ConstrainedIndicatorValue.scale(new double[] {0.0, 0.0, 0.0}));
    }

    @Test
    @DisplayName("a scaled violation is in [0, 1) and grows with the violation")
    void givenViolations_whenScaled_thenTheyAreBoundedAndOrdered() {
      // Act
      double none = ConstrainedIndicatorValue.scaledViolation(0.0, 10.0);
      double small = ConstrainedIndicatorValue.scaledViolation(1.0, 10.0);
      double huge = ConstrainedIndicatorValue.scaledViolation(1e9, 10.0);

      // Assert
      assertEquals(0.0, none);
      assertTrue(none < small && small < huge && huge < 1.0);
    }
  }

  @Nested
  @DisplayName("When the values of two configurations are compared")
  class Ordering {

    @Test
    @DisplayName("a single run without feasible solutions is worse than any quality")
    void givenAFailedRun_whenCompared_thenItIsWorseThanAnyFeasibleConfiguration() {
      // Arrange: 5 problems; the worst feasible configuration has the threshold on all of them
      double worstFeasible = ConstrainedIndicatorValue.value(0, 0.0, 1.0);
      // one failed run, almost feasible, perfect quality elsewhere (its problem takes 1)
      double almostFeasible = ConstrainedIndicatorValue.value(1, 1e-9, (0.0 * 4 + 1.0) / 5);

      // Act and Assert
      assertTrue(worstFeasible < almostFeasible);
    }

    @Test
    @DisplayName("with the same failed runs, less violation is better whatever the quality")
    void givenTheSameFailedRuns_whenCompared_thenLessViolationIsBetter() {
      // Act
      double lessViolation = ConstrainedIndicatorValue.value(2, 0.2, 1.0);
      double moreViolation = ConstrainedIndicatorValue.value(2, 1.8, 0.2);

      // Assert: mean violations 0.1 and 0.9, with the best and worst qualities swapped
      assertTrue(lessViolation < moreViolation);
    }

    @Test
    @DisplayName("without failed runs, the quality decides")
    void givenNoFailedRuns_whenCompared_thenTheQualityDecides() {
      // Act and Assert
      assertEquals(0.3, ConstrainedIndicatorValue.value(0, 0.0, 0.3));
      assertTrue(
          ConstrainedIndicatorValue.value(0, 0.0, 0.3) < ConstrainedIndicatorValue.value(0, 0.0, 0.4));
    }
  }
}
