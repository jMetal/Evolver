package org.uma.evolver.parameter.yaml.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

/** Test class for {@link IntegerParameterProcessor}. */
class IntegerParameterProcessorTest {
  private IntegerParameterProcessor processor;
  private ParameterSpace parameterSpace;

  @BeforeEach
  void setUp() {
    processor = new IntegerParameterProcessor();
    parameterSpace =
        new ParameterSpace() {
          @Override
          public ParameterSpace createInstance() {
            return null;
          }
        };
  }

  @Nested
  @DisplayName("When processing an integer parameter")
  class ProcessTests {
    @Test
    @DisplayName("a parameter is created if the range of values is correct")
    void whenRangeAValidRangeIsSpecifiedThenTheParameterIsCreated() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", Arrays.asList(1, 10));

      // Act
      processor.process(parameterName, config, parameterSpace);

      // Assert
      assertNotNull(parameterSpace.get(parameterName));
      IntegerParameter param = (IntegerParameter) parameterSpace.get(parameterName);
      assertEquals(1, param.minValue().intValue());
      assertEquals(10, param.maxValue().intValue());
    }


    @Test
    @DisplayName("an exception is thrown if the range of values has equals min and max range values")
    void whenRangeHasEqualMinAndMaxThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", Arrays.asList(5, 5));

      // Act & Assert
      assertThrows(
          InvalidConditionException.class,
          () -> processor.process(parameterName, config, parameterSpace));
    }
  }

  @Nested
  @DisplayName("When processing an integer parameter with invalid configuration")
  class InvalidConfigTests {
    @Test
    @DisplayName("an exception is thrown if the configuration is null")
    void whenConfigIsNullThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";

      // Act & Assert
      assertThrows(
              NullParameterException.class, () -> processor.process(parameterName, null, parameterSpace));
    }

    @Test
    @DisplayName("an exception is thrown if the configuration is not a map")
    void whenConfigIsNotAMapThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, "not a map", parameterSpace));
    }

    @Test
    @DisplayName("an exception is thrown if the range is missing")
    void whenRangeIsMissingThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, config, parameterSpace));
    }
  }

  @Nested
    class InvalidRangeTests {
    @Test
    @DisplayName("an exception is thrown if the range is not a list")
    void whenRangeIsNotAListThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", "not a list");

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, config, parameterSpace));
    }

    @Test
    @DisplayName("an exception is thrown if the range has wrong number of elements")
    void whenRangeHasWrongNumberOfElementsThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", Arrays.asList(1, 2, 3));

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, config, parameterSpace));
    }

    @ParameterizedTest
    @MethodSource("invalidRangeValues")
    @DisplayName("an exception is thrown if the range elements are invalid")
    void whenRangeElementsAreInvalidThenAnExceptionIsThrown(
        Object rangeValue, String expectedErrorMessage) {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", rangeValue);

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, config, parameterSpace));
    }

    private static Stream<Arguments> invalidRangeValues() {
      return Stream.of(
          Arguments.of(Arrays.asList(1.5, 10.5), "must be integers"),
          Arguments.of("not a list", "Range must be a list"),
          Arguments.of(Arrays.asList(10, 1), "must be less than or equal to"));
    }
  }

  @Nested
  class InvalidParameterTests {
    @Test
    @DisplayName("an exception is thrown if the values key is present")
    void whenValuesKeyIsPresentThenAnExceptionIsThrown() {
      // Arrange
      String parameterName = "intParam";
      Map<String, Object> config = new HashMap<>();
      config.put("range", Arrays.asList(1, 10));
      config.put("values", Arrays.asList(1, 2, 3));

      // Act & Assert
      assertThrows(
              InvalidConditionException.class,
              () -> processor.process(parameterName, config, parameterSpace));
    }
  }
}
