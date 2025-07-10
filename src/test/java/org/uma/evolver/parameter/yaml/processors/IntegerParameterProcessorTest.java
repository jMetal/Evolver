package org.uma.evolver.parameter.yaml.processors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

import java.util.Arrays;
import java.util.List;

class IntegerParameterProcessorTest {

  @Test
  void shouldProcessRange() {
    // Given
    IntegerParameterProcessor processor = new IntegerParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> range = Arrays.asList(1, 10);

    // When
    processor.process("testParam", range, parameterSpace);

    // Then
    IntegerParameter param = (IntegerParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(1, param.minValue());
    assertEquals(10, param.maxValue());
  }

  @Test
  void shouldProcessDiscreteValues() {
    // Given
    IntegerParameterProcessor processor = new IntegerParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> values = Arrays.asList(1, 2, 3, 5, 8);

    // When
    processor.process("testParam", values, parameterSpace);

    // Then
    CategoricalIntegerParameter param = (CategoricalIntegerParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(5, param.validValues().size());
    for (int value : values) {
      assertTrue(param.validValues().contains(value));
    }
  }

  @Test
  void shouldThrowOnInvalidRange() {
    // Given
    IntegerParameterProcessor processor = new IntegerParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> invalidRange = Arrays.asList(10, 1); // min > max

    // When/Then
    assertThrows(InvalidConditionException.class, () -> {
      processor.process("testParam", invalidRange, parameterSpace);
    });
  }

  @Test
  void shouldThrowOnInvalidValueCount() {
    // Given
    IntegerParameterProcessor processor = new IntegerParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> invalidValues = List.of(1); // Only one value

    // When/Then
    assertThrows(JMetalException.class, () -> {
      processor.process("testParam", invalidValues, parameterSpace);
    });
  }
}
