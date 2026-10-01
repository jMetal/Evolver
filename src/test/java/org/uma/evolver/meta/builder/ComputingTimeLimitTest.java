package org.uma.evolver.meta.builder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.meta.algorithm.RandomSearch;
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
  @DisplayName("Given a time limit, when building a random search, then it is bounded by time")
  void givenTimeLimitWhenBuildingRandomSearchThenItIsBoundedByTime() {
    // Arrange / Act
    RandomSearch<?> randomSearch =
        new MetaRandomSearchBuilder<>(new ZDT1()).setMaxComputingTimeMinutes(0.5).build();

    // Assert
    assertEquals(30_000, randomSearch.maxComputingTimeMillis());
  }
}
