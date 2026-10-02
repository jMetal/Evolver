package org.uma.evolver.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.jmetal.qualityindicator.impl.GeneralizedSpread;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class IndicatorRegistry")
class IndicatorRegistryTest {

  @Nested
  @DisplayName("When resolving an indicator name: ")
  class ResolveTestCases {

    @Test
    @DisplayName("given Spread, when resolved, then it returns jMetal's Spread")
    void givenSpread_whenResolved_thenItReturnsASpread() {
      // Act & Assert
      assertInstanceOf(Spread.class, IndicatorRegistry.resolve("Spread"));
    }

    @Test
    @DisplayName("given GeneralizedSpread, when resolved, then it returns jMetal's GeneralizedSpread")
    void givenGeneralizedSpread_whenResolved_thenItReturnsAGeneralizedSpread() {
      // Act & Assert
      assertInstanceOf(GeneralizedSpread.class, IndicatorRegistry.resolve("GeneralizedSpread"));
    }

    @Test
    @DisplayName("given an unknown name, when resolved, then it fails")
    void givenUnknownName_whenResolved_thenItFails() {
      // Act & Assert
      assertThrows(JMetalException.class, () -> IndicatorRegistry.resolve("NotAnIndicator"));
    }
  }

  @Nested
  @DisplayName("When checking whether an indicator applies to a number of objectives: ")
  class CheckApplicableTestCases {

    @Test
    @DisplayName("given Spread and two objectives, when checked, then it is accepted")
    void givenSpreadAndTwoObjectives_whenChecked_thenItIsAccepted() {
      // Act & Assert
      assertDoesNotThrow(() -> IndicatorRegistry.checkApplicable("Spread", 2));
    }

    @ParameterizedTest(name = "{0} objectives")
    @ValueSource(ints = {3, 5})
    @DisplayName("given Spread and more than two objectives, when checked, then it is rejected")
    void givenSpreadAndMoreThanTwoObjectives_whenChecked_thenItIsRejected(int objectives) {
      // Act
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> IndicatorRegistry.checkApplicable("Spread", objectives));

      // Assert
      assertTrue(exception.getMessage().contains("GeneralizedSpread"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GeneralizedSpread", "Epsilon", "NormalizedHypervolume"})
    @DisplayName("given an indicator for any number of objectives, when checked with three, then it is accepted")
    void givenIndicatorForAnyNumberOfObjectives_whenCheckedWithThree_thenItIsAccepted(
        String indicatorName) {
      // Act & Assert
      assertDoesNotThrow(() -> IndicatorRegistry.checkApplicable(indicatorName, 3));
    }
  }
}
