package org.uma.evolver.parameter.yaml.processors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

import java.util.Arrays;
import java.util.List;

class DoubleParameterProcessorTest {

  private static final double EPSILON = 0.000001;

  @Test
  void shouldProcessRange() {
    // Given
    DoubleParameterProcessor processor = new DoubleParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Double> range = Arrays.asList(1.0, 10.0);

    // When
    processor.process("testParam", range, parameterSpace);

    // Then
    DoubleParameter param = (DoubleParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(1.0, param.minValue(), EPSILON);
    assertEquals(10.0, param.maxValue(), EPSILON);
  }

  @Test
  void shouldProcessDiscreteValues() {
    // Given
    DoubleParameterProcessor processor = new DoubleParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Double> values = Arrays.asList(1.0, 2.5, 3.7, 5.2);

    // When
    processor.process("testParam", values, parameterSpace);

    // Then
    CategoricalParameter param = (CategoricalParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(4, param.validValues().size());
    assertTrue(param.validValues().contains("1.0"));
    assertTrue(param.validValues().contains("2.5"));
    assertTrue(param.validValues().contains("3.7"));
    assertTrue(param.validValues().contains("5.2"));
  }

  @Test
  void shouldThrowOnInvalidRange() {
    // Given
    DoubleParameterProcessor processor = new DoubleParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Double> invalidRange = Arrays.asList(10.0, 1.0); // min > max

    // When/Then
    assertThrows(InvalidConditionException.class, () -> {
      processor.process("testParam", invalidRange, parameterSpace);
    });
  }

  @Test
  void shouldHandleIntegerValues() {
    // Given
    DoubleParameterProcessor processor = new DoubleParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> values = Arrays.asList(1, 2, 3);

    // When
    processor.process("testParam", values, parameterSpace);

    // Then
    CategoricalParameter param = (CategoricalParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(3, param.validValues().size());
    assertTrue(param.validValues().contains("1.0"));
    assertTrue(param.validValues().contains("2.0"));
    assertTrue(param.validValues().contains("3.0"));
  }
}
