package org.uma.evolver.parameter.yaml.processors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

class CategoricalParameterProcessorTest {

  @Test
  void shouldProcessStringCategories() {
    // Given
    CategoricalParameterProcessor processor = new CategoricalParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<String> categories = Arrays.asList("A", "B", "C");

    // When
    processor.process("testParam", categories, parameterSpace);

    // Then
    assertTrue(parameterSpace.get("testParam") instanceof CategoricalParameter);
    CategoricalParameter param = (CategoricalParameter) parameterSpace.get("testParam");
    assertEquals(3, param.validValues().size());
    assertTrue(param.validValues().contains("A"));
    assertTrue(param.validValues().contains("B"));
    assertTrue(param.validValues().contains("C"));
  }

  @Test
  void shouldProcessNumericCategories() {
    // Given
    CategoricalParameterProcessor processor = new CategoricalParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    List<Integer> categories = Arrays.asList(1, 2, 3);

    // When
    processor.process("testParam", categories, parameterSpace);

    // Then
    CategoricalIntegerParameter param = (CategoricalIntegerParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(3, param.validValues().size());
    assertTrue(param.validValues().contains(1));
    assertTrue(param.validValues().contains(2));
    assertTrue(param.validValues().contains(3));
  }

  @Test
  void shouldProcessMapCategories() {
    // Given
    CategoricalParameterProcessor processor = new CategoricalParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    Map<String, Object> categories = new HashMap<>();
    categories.put("A", null);
    categories.put("B", null);
    categories.put("C", null);

    // When
    processor.process("testParam", categories, parameterSpace);

    // Then
    assertTrue(parameterSpace.get("testParam") instanceof CategoricalParameter);
    CategoricalParameter param = (CategoricalParameter) parameterSpace.get("testParam");
    assertEquals(3, param.validValues().size());
    assertTrue(param.validValues().contains("A"));
    assertTrue(param.validValues().contains("B"));
    assertTrue(param.validValues().contains("C"));
  }

  @Test
  void shouldHandleGlobalSubParameters() {
    // Given
    CategoricalParameterProcessor processor = new CategoricalParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();
    
    Map<String, Object> parameterMap = new HashMap<>();
    parameterMap.put("values", List.of("A", "B"));
    
    Map<String, Object> subParams = new HashMap<>();
    subParams.put("intParam", Map.of("type", "integer", "range", List.of(1, 10)));
    subParams.put("doubleParam", Map.of("type", "double", "range", List.of(0.1, 1.0)));
    parameterMap.put("globalSubParameters", subParams);

    // When
    processor.process("testParam", parameterMap, parameterSpace);

    // Then
    CategoricalParameter param = (CategoricalParameter) parameterSpace.get("testParam");
    assertNotNull(param);
    assertEquals(2, param.validValues().size());
    assertTrue(param.validValues().contains("A"));
    assertTrue(param.validValues().contains("B"));
    
    // Verify sub-parameters were added to the parameter space
    assertNotNull(parameterSpace.get("intParam"));
    assertNotNull(parameterSpace.get("doubleParam"));
    
    // The actual processing of sub-parameters would be tested in the YAMLParameterSpaceTest
  }
  
  @Test
  void shouldThrowExceptionForEmptyList() {
    // Given
    CategoricalParameterProcessor processor = new CategoricalParameterProcessor();
    ParameterSpace parameterSpace = new ParameterSpace();

    // When/Then
    assertThrows(InvalidConditionException.class, () -> 
      processor.process("testParam", List.of(), parameterSpace)
    , "Should throw InvalidConditionException for empty list");
  }
}
