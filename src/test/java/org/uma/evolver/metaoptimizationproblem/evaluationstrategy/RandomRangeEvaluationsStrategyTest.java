package org.uma.evolver.metaoptimizationproblem.evaluationstrategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("RandomRangeEvaluationsStrategy Tests")
class RandomRangeEvaluationsStrategyTest {
  private static final int MIN_EVALUATIONS = 1000;
  private static final int MAX_EVALUATIONS = 5000;
  private Random mockRandom;
  private RandomRangeEvaluationsStrategy strategy;

  @BeforeEach
  void setUp() {
    mockRandom = mock(Random.class);
    when(mockRandom.nextInt(anyInt())).thenReturn(1000); // Return a fixed value for predictable tests
    strategy = new RandomRangeEvaluationsStrategy(MIN_EVALUATIONS, MAX_EVALUATIONS, mockRandom);
  }

  @Test
  @DisplayName("Constructor throws exception when random is null")
  void constructorThrowsWhenRandomIsNull() {
    assertThrows(
        NullParameterException.class,
        () -> new RandomRangeEvaluationsStrategy(MIN_EVALUATIONS, MAX_EVALUATIONS, null));
  }

  @Test
  @DisplayName("Constructor throws exception when minEvaluations is not positive")
  void constructorThrowsWhenMinEvaluationsIsNotPositive() {
    assertThrows(
        InvalidConditionException.class,
        () -> new RandomRangeEvaluationsStrategy(0, MAX_EVALUATIONS, mockRandom));
    assertThrows(
        InvalidConditionException.class,
        () -> new RandomRangeEvaluationsStrategy(-1, MAX_EVALUATIONS, mockRandom));
  }

  @Test
  @DisplayName("Constructor throws exception when maxEvaluations is less than minEvaluations")
  void constructorThrowsWhenMaxLessThanMin() {
    assertThrows(
        InvalidConditionException.class,
        () -> new RandomRangeEvaluationsStrategy(MIN_EVALUATIONS, MIN_EVALUATIONS - 1, mockRandom));
  }

  @Test
  @DisplayName("getEvaluations returns value within range")
  void getEvaluationsReturnsValueWithinRange() {
    // Arrange
    int randomValue = 1000;
    when(mockRandom.nextInt(MAX_EVALUATIONS - MIN_EVALUATIONS + 1)).thenReturn(randomValue);
    int expectedEvaluations = MIN_EVALUATIONS + randomValue;

    // Act
    int actualEvaluations = strategy.getEvaluations(0);

    // Assert
    assertEquals(expectedEvaluations, actualEvaluations);
    assertTrue(actualEvaluations >= MIN_EVALUATIONS);
    assertTrue(actualEvaluations <= MAX_EVALUATIONS);
  }

  @Test
  @DisplayName("getEvaluations throws exception when problem index is negative")
  void getEvaluationsThrowsWhenIndexIsNegative() {
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> strategy.getEvaluations(-1));
  }

  @Test
  @DisplayName("validate throws exception when number of problems is not positive")
  void validateThrowsWhenNumberOfProblemsIsNotPositive() {
    // Test with zero
    assertThrows(
        InvalidConditionException.class,
        () -> strategy.validate(0));
    
    // Test with negative number
    assertThrows(
        InvalidConditionException.class,
        () -> strategy.validate(-1));
  }

  @Test
  @DisplayName("validate does not throw when number of problems is positive")
  void validateDoesNotThrowWhenNumberOfProblemsIsPositive() {
    assertDoesNotThrow(() -> strategy.validate(1));
    assertDoesNotThrow(() -> strategy.validate(10));
  }

  @Test
  @DisplayName("getMinEvaluations returns correct value")
  void getMinEvaluationsReturnsCorrectValue() {
    assertEquals(MIN_EVALUATIONS, strategy.getMinEvaluations());
  }

  @Test
  @DisplayName("getMaxEvaluations returns correct value")
  void getMaxEvaluationsReturnsCorrectValue() {
    assertEquals(MAX_EVALUATIONS, strategy.getMaxEvaluations());
  }
}
