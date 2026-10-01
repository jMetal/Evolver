package org.uma.evolver.meta.builder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.meta.algorithm.RandomSearch;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("ComputingTimeLimit and the time limit of the meta-optimizer builders")
class ComputingTimeLimitTest {

  @Test
  @DisplayName("Given minutes with decimals, when converting, then the duration is exact to the millisecond")
  void givenMinutesWithDecimalsWhenConvertingThenDurationIsExact() {
    // Arrange / Act
    Duration half = ComputingTimeLimit.toDuration(0.5);
    Duration ninety = ComputingTimeLimit.toDuration(90);

    // Assert
    assertEquals(Duration.ofSeconds(30), half);
    assertEquals(Duration.ofMinutes(90), ninety);
  }

  @Test
  @DisplayName("Given zero, negative or non-finite minutes, when checking, then it is rejected")
  void givenInvalidMinutesWhenCheckingThenItIsRejected() {
    // Arrange / Act / Assert
    assertThrows(IllegalArgumentException.class, () -> ComputingTimeLimit.checkMinutes(0.0));
    assertThrows(IllegalArgumentException.class, () -> ComputingTimeLimit.checkMinutes(-1.0));
    assertThrows(IllegalArgumentException.class, () -> ComputingTimeLimit.checkMinutes(Double.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> ComputingTimeLimit.checkMinutes(Double.POSITIVE_INFINITY));
  }

  @Test
  @DisplayName("Given evaluations set first, when setting the time, then the builder rejects it")
  void givenEvaluationsSetFirstWhenSettingTimeThenBuilderRejectsIt() {
    // Arrange
    var builder = new MetaRandomSearchBuilder<>(new ZDT1()).setMaxEvaluations(100);

    // Act / Assert
    assertThrows(IllegalStateException.class, () -> builder.setMaxComputingTimeMinutes(1.0));
  }

  @Test
  @DisplayName("Given the time set first, when setting evaluations, then the builder rejects it")
  void givenTimeSetFirstWhenSettingEvaluationsThenBuilderRejectsIt() {
    // Arrange
    var builder = new MetaRandomSearchBuilder<>(new ZDT1()).setMaxComputingTimeMinutes(1.0);

    // Act / Assert
    assertThrows(IllegalStateException.class, () -> builder.setMaxEvaluations(100));
  }

  @Test
  @DisplayName("Given only the time or only the evaluations, when configuring, then there is no error")
  void givenOnlyOneLimitWhenConfiguringThenThereIsNoError() {
    // Arrange / Act / Assert
    assertDoesNotThrow(() -> new MetaRandomSearchBuilder<>(new ZDT1()).setMaxEvaluations(100));
    assertDoesNotThrow(
        () -> new MetaRandomSearchBuilder<>(new ZDT1()).setMaxComputingTimeMinutes(0.1));
  }

  @Test
  @DisplayName("Given the time reached before the initial population, when checking the asynchronous termination, then it is not met")
  void givenTimeReachedBeforeInitialPopulationWhenCheckingAsynchronousTerminationThenItIsNotMet() {
    // Arrange
    Termination termination = ComputingTimeLimit.asynchronousTermination(0.5, 10);

    // Act
    boolean met = termination.isMet(Map.of("EVALUATIONS", 9, "COMPUTING_TIME", 60_000L));

    // Assert
    assertFalse(met);
  }

  @Test
  @DisplayName("Given the initial population evaluated, when checking the asynchronous termination, then it depends on the time")
  void givenInitialPopulationEvaluatedWhenCheckingAsynchronousTerminationThenItDependsOnTime() {
    // Arrange
    Termination termination = ComputingTimeLimit.asynchronousTermination(0.5, 10);

    // Act
    boolean beforeLimit = termination.isMet(Map.of("EVALUATIONS", 10, "COMPUTING_TIME", 29_999L));
    boolean atLimit = termination.isMet(Map.of("EVALUATIONS", 10, "COMPUTING_TIME", 30_000L));

    // Assert
    assertFalse(beforeLimit);
    assertTrue(atLimit);
  }

  @Test
  @DisplayName("Given evaluations set first, when setting the time in the asynchronous NSGA-II builder, then it is rejected")
  void givenEvaluationsSetFirstWhenSettingTimeInAsyncNsgaIIBuilderThenItIsRejected() {
    // Arrange
    var builder = new MetaAsyncNSGAIIBuilder(new ZDT1()).setMaxEvaluations(100);

    // Act / Assert
    assertThrows(IllegalStateException.class, () -> builder.setMaxComputingTimeMinutes(1.0));
  }

  @Test
  @DisplayName("Given the time set first, when setting evaluations in the asynchronous GA builder, then it is rejected")
  void givenTimeSetFirstWhenSettingEvaluationsInAsyncGaBuilderThenItIsRejected() {
    // Arrange
    var builder = new MetaAsyncGeneticAlgorithmBuilder(new ZDT1()).setMaxComputingTimeMinutes(1.0);

    // Act / Assert
    assertThrows(IllegalStateException.class, () -> builder.setMaxEvaluations(100));
  }

  @Test
  @DisplayName("Given a time limit, when building a random search, then it is bounded by time")
  void givenTimeLimitWhenBuildingRandomSearchThenItIsBoundedByTime() {
    // Arrange / Act
    RandomSearch<?> randomSearch =
        new MetaRandomSearchBuilder<>(new ZDT1()).setMaxComputingTimeMinutes(0.5).build();

    // Assert
    assertEquals(30_000, randomSearch.maxComputingTimeMillis());
  }
}
