package org.uma.evolver.parameter.yaml;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Map;

class YAMLParameterSpaceTest {

  @Test
  void loadParametersFromYAML_WithValidYAML_ShouldLoadParameters() {
    // Given
    String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";
    
    // When
    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlFilePath);
    
    // Then
    assertNotNull(parameterSpace);
    assertTrue(parameterSpace.get("categoricalParam") instanceof CategoricalParameter);
    assertTrue(parameterSpace.get("integerParam") instanceof IntegerParameter);
    assertTrue(parameterSpace.get("doubleParam") instanceof DoubleParameter);
    
    // Verify values for categorical parameter
    CategoricalParameter catParam = (CategoricalParameter) parameterSpace.get("categoricalParam");
    assertEquals(3, catParam.validValues().size());
    assertTrue(catParam.validValues().contains("A"));
    assertTrue(catParam.validValues().contains("B"));
    assertTrue(catParam.validValues().contains("C"));
    
    // Verify values for integer parameter
    IntegerParameter intParam = (IntegerParameter) parameterSpace.get("integerParam");
    assertEquals(1, intParam.minValue());
    assertEquals(100, intParam.maxValue());
    
    // Verify values for double parameter
    DoubleParameter doubleParam = (DoubleParameter) parameterSpace.get("doubleParam");
    assertEquals(0.1, doubleParam.minValue(), 0.0001);
    assertEquals(1.0, doubleParam.maxValue(), 0.0001);
  }

  @Test
  void loadParametersFromYAML_WithNonExistentFile_ShouldThrowException() {
    // Given
    String invalidYamlPath = "nonexistent.yaml";
    
    // When/Then
    assertThrows(JMetalException.class, () -> {
      new YAMLParameterSpace(invalidYamlPath);
    });
  }

  @Test
  void loadParametersFromYAML_WithEmptyYAML_ShouldReturnEmptyParameterSpace() {
    // Given
    String emptyYamlPath = "src/test/resources/parameterSpaces/EmptyParameterSpace.yaml";
    
    // When
    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(emptyYamlPath);
    
    // Then
    assertTrue(parameterSpace.parameters().isEmpty());
  }

  @Test
  void loadParametersFromYAML_WithInvalidParameterType_ShouldThrowException() {
    // Given
    String invalidTypeYaml = "src/test/resources/parameterSpaces/InvalidParameterType.yaml";
    
    // When/Then
    assertThrows(JMetalException.class, () -> {
      new YAMLParameterSpace(invalidTypeYaml);
    });
  }
}
