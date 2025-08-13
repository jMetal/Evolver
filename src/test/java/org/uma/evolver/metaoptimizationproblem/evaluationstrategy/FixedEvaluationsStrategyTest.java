package org.uma.evolver.metaoptimizationproblem.evaluationstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.exception.EmptyCollectionException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("FixedEvaluationsStrategy Tests")
class FixedEvaluationsStrategyTest {

  private FixedEvaluationsStrategy strategy;
  private final List<Integer> testEvaluations = List.of(1000, 2000, 3000);

  @BeforeEach
  void setUp() {
    strategy = new FixedEvaluationsStrategy(testEvaluations);
  }

  @Test
  @DisplayName("Constructor throws exception when evaluations list is null")
  void constructorThrowsWhenEvaluationsIsNull() {
    // Arrange & Act & Assert
    assertThrows(NullParameterException.class, () -> new FixedEvaluationsStrategy(null));
  }

  @Test
  @DisplayName("Constructor throws exception when evaluations list is empty")
  void constructorThrowsWhenEvaluationsIsEmpty() {
    // Arrange & Act & Assert
    assertThrows(InvalidConditionException.class, () -> new FixedEvaluationsStrategy(List.of()));
  }

  @Test
  @DisplayName("Constructor throws exception when any evaluation count is not positive")
  void constructorThrowsWhenAnyEvaluationIsNotPositive() {
    // Arrange
    List<Integer> invalidEvaluations = List.of(1000, 0, 2000);
    
    // Act & Assert
    assertThrows(InvalidConditionException.class, () -> new FixedEvaluationsStrategy(invalidEvaluations));
  }

  @Test
  @DisplayName("getEvaluations returns correct evaluation count for valid index")
  void getEvaluationsReturnsCorrectValueForValidIndex() {
    // Arrange
    int index = 1;
    int expectedEvaluations = testEvaluations.get(index);

    // Act
    int actualEvaluations = strategy.getEvaluations(index);

    // Assert
    assertEquals(expectedEvaluations, actualEvaluations);
  }

  @Test
  @DisplayName("getEvaluations throws exception when index is negative")
  void getEvaluationsThrowsWhenIndexIsNegative() {
    // Arrange
    int invalidIndex = -1;

    // Act & Assert
    assertThrows(InvalidConditionException.class, () -> strategy.getEvaluations(invalidIndex));
  }

  @Test
  @DisplayName("getEvaluations throws exception when index is out of bounds")
  void getEvaluationsThrowsWhenIndexIsOutOfBounds() {
    // Arrange
    int outOfBoundsIndex = testEvaluations.size();

    // Act & Assert
    assertThrows(InvalidConditionException.class, () -> strategy.getEvaluations(outOfBoundsIndex));
  }

  @Test
  @DisplayName("validate passes when number of problems matches evaluations count")
  void validatePassesWhenNumberOfProblemsMatches() {
    // Arrange
    int numberOfProblems = testEvaluations.size();

    // Act & Assert (should not throw)
    strategy.validate(numberOfProblems);
  }

  @Test
  @DisplayName("validate throws when number of problems does not match evaluations count")
  void validateThrowsWhenNumberOfProblemsDoesNotMatch() {
    // Arrange
    int invalidNumberOfProblems = testEvaluations.size() + 1;

    // Act & Assert
    assertThrows(InvalidConditionException.class, () -> strategy.validate(invalidNumberOfProblems));
  }

  @Test
  @DisplayName("getEvaluations returns unmodifiable list")
  void getEvaluationsReturnsUnmodifiableList() {
    // Arrange
    var evaluations = strategy.getEvaluations();

    // Act & Assert
    assertThrows(UnsupportedOperationException.class, () -> evaluations.add(4000));
  }

  @Test
  @DisplayName("getEvaluations returns list with same values as constructor argument")
  void getEvaluationsReturnsListWithSameValues() {
    // Act
    var evaluations = strategy.getEvaluations();

    // Assert
    assertEquals(testEvaluations, evaluations);
  }
}