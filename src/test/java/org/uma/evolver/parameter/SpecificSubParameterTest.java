package org.uma.evolver.parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

class SpecificSubParameterTest {
  @Test
  @DisplayName("Constructor should correctly set condition and parameter for Integer predicate")
  void constructorShouldSetFieldsForIntegerPredicate() {
    // Arrange
    Predicate<Integer> intPredicate = value -> value > 10;
    Parameter<?> mockParam = mock(Parameter.class);
    when(mockParam.name()).thenReturn("testParam");

    // Act
    ConditionalParameter<Integer> specificParam =
        new ConditionalParameter<>(intPredicate, mockParam, "parameter description");

    // Assert
    assertNotNull(specificParam.condition());
    assertSame(mockParam, specificParam.parameter());
    assertTrue(specificParam.condition().test(15));
    assertFalse(specificParam.condition().test(5));
  }

  @Test
  @DisplayName("Constructor should correctly set condition and parameter for String predicate")
  void constructorShouldSetFieldsForStringPredicate() {
    // Arrange
    Predicate<String> stringPredicate = string -> string.equals("Value");
    Parameter<?> mockParam = mock(Parameter.class);
    when(mockParam.name()).thenReturn("stringParameter");

    // Act
    ConditionalParameter<String> specificParam =
        new ConditionalParameter<>(stringPredicate, mockParam, "parameter description");

    // Assert
    assertNotNull(specificParam.condition());
    assertSame(mockParam, specificParam.parameter());
    assertTrue(specificParam.condition().test("Value"));
    assertFalse(specificParam.condition().test("AnotherValue"));

    System.out.println(specificParam);
  }

  @Test
  @DisplayName("Constructor should handle null parameter correctly")
  void constructorShouldHandleNullParameter() {
    // Arrange & Act & Assert
    assertThrows(NullParameterException.class, () -> new ConditionalParameter<>(value -> true, null, "parameter description"));
  }

  @Test
  @DisplayName("Constructor should handle null predicate correctly")
  void constructorShouldHandleNullPredicate() {
    // Arrange
    Parameter<?> mockParam = mock(Parameter.class);

    // Act & Assert
    assertThrows(
            NullParameterException.class, () -> new ConditionalParameter<String>(null, mockParam, "parameter description"));
  }

  @Test
  @DisplayName("toString should return formatted string with condition and parameter name")
  void toStringShouldReturnFormattedString() {
    // Arrange
    Predicate<Double> doublePredicate = d -> d < 100.0;
    Parameter<?> mockParam = mock(Parameter.class);
    when(mockParam.name()).thenReturn("doubleParam");
    ConditionalParameter<Double> specificParam =
        new ConditionalParameter<>(doublePredicate, mockParam, "parameter description");

    // Act
    String result = specificParam.toString();

    // Assert
    assertTrue(result.contains("Condition:"));
    assertTrue(result.contains("Parameter: doubleParam"));
  }

  @ParameterizedTest(name = "Test with value {0}")
  @ValueSource(ints = {5, 10, 15, 20, 25})
  @DisplayName("Predicate condition should evaluate correctly for various integer values")
  void predicateConditionShouldEvaluateCorrectlyForIntegers(int value) {
    // Arrange
    Predicate<Integer> intPredicate = v -> v > 15;
    Parameter<?> mockParam = mock(Parameter.class);
    ConditionalParameter<Integer> specificParam =
        new ConditionalParameter<>(intPredicate, mockParam, "parameter description");

    // Act & Assert
    assertEquals(value > 15, specificParam.condition().test(value));
  }

  @ParameterizedTest
  @MethodSource("providePredicatesAndValues")
  @DisplayName("Different predicate types should work correctly")
  <T> void differentPredicateTypesShouldWorkCorrectly(
      Predicate<T> predicate, T trueValue, T falseValue) {
    // Arrange
    Parameter<?> mockParam = mock(Parameter.class);
    ConditionalParameter<T> specificParam = new ConditionalParameter<>(predicate, mockParam, "parameter description");

    // Act & Assert
    assertTrue(specificParam.condition().test(trueValue));
    assertFalse(specificParam.condition().test(falseValue));
  }

  static Stream<Object[]> providePredicatesAndValues() {
    return Stream.of(
        new Object[] {(Predicate<Integer>) i -> i % 2 == 0, 4, 5},
        new Object[] {(Predicate<String>) s -> s.startsWith("test"), "testing", "wrong"},
        new Object[] {(Predicate<Boolean>) b -> b, true, false},
        new Object[] {(Predicate<Double>) d -> d < 1.0, 0.5, 1.5});
  }
}
