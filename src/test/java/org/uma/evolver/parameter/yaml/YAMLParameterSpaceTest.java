package org.uma.evolver.parameter.yaml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.Parameter;

/**
 * Test class for {@link YAMLParameterSpace}.
 *
 * <p>This test suite verifies the functionality of loading and processing parameter configurations
 * from YAML files, including error handling and edge cases.
 */
@DisplayName("YAMLParameterSpace Tests")
class YAMLParameterSpaceTest {

  private ParameterFactory<?> parameterFactory;

  @BeforeEach
  void setUp() {
    parameterFactory = new DoubleParameterFactory();
  }

  @Nested
  @DisplayName("When loading parameters from YAML")
  class LoadParametersTest {

    @Test
    @DisplayName("should load all parameter types correctly")
    void shouldLoadAllParameterTypes() {
      // Arrange
      String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";

      // Act
      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlFilePath, parameterFactory);

      // Assert
      assertAll(
          () ->
              assertTrue(
                  parameterSpace.get("categoricalParam") instanceof CategoricalParameter,
                  "Should load categorical parameter"),
          () ->
              assertTrue(
                  parameterSpace.get("integerParam") instanceof IntegerParameter,
                  "Should load integer parameter"),
          () ->
              assertTrue(
                  parameterSpace.get("doubleParam") instanceof DoubleParameter,
                  "Should load double parameter"));
    }

    @Test
    @DisplayName("should load parameter values correctly")
    void shouldLoadParameterValuesCorrectly() {
      // Arrange
      String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";

      // Act
      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlFilePath, parameterFactory);
      CategoricalParameter catParam = (CategoricalParameter) parameterSpace.get("categoricalParam");

      // Assert
      assertAll(
          () ->
              assertEquals(3, catParam.validValues().size(), "Should load all categorical values"),
          () ->
              assertIterableEquals(
                  List.of("A", "B", "C"),
                  catParam.validValues(),
                  "Should maintain order of categorical values"));
    }

    @Test
    @DisplayName("should load numeric ranges correctly")
    void shouldLoadNumericRangesCorrectly() {
      // Arrange
      String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";

      // Act
      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlFilePath, parameterFactory);
      IntegerParameter intParam = (IntegerParameter) parameterSpace.get("integerParam");
      DoubleParameter doubleParam = (DoubleParameter) parameterSpace.get("doubleParam");

      // Assert
      assertAll(
          () -> assertEquals(1, intParam.minValue(), "Should load integer min value"),
          () -> assertEquals(100, intParam.maxValue(), "Should load integer max value"),
          () -> assertEquals(0.1, doubleParam.minValue(), 0.001, "Should load double min value"),
          () -> assertEquals(1.0, doubleParam.maxValue(), 0.001, "Should load double max value"));
    }
  }

  @Nested
  @DisplayName("When handling file loading")
  class FileLoadingTest {

    @Test
    @DisplayName("should throw exception for non-existent file")
    void shouldThrowExceptionForNonExistentFile() {
      // Arrange
      String invalidPath = "nonexistent.yaml";

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> new YAMLParameterSpace(invalidPath, parameterFactory),
              "Should throw RuntimeException for non-existent file");

      String errorMessage = exception.getMessage();
      assertTrue(
          errorMessage.contains("not found") ||
          (exception.getCause() != null && exception.getCause().getMessage().contains("not found")),
          "Error message should indicate file not found. Actual message: " + errorMessage);
    }

    @Test
    @DisplayName("should throw exception for empty YAML file")
    void shouldThrowExceptionForEmptyYamlFile() {
      // Arrange
      String emptyYamlPath = "src/test/resources/parameterSpaces/EmptyParameterSpace.yaml";

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> new YAMLParameterSpace(emptyYamlPath, parameterFactory),
              "Should throw RuntimeException for empty YAML file");

      assertTrue(
          exception.getMessage().contains("Failed to load parameter definitions from YAML file"),
          "Error message should indicate failure to load YAML");
    }

    @Test
    @DisplayName("should load from classpath")
    void shouldLoadFromClasspath() {
      // Arrange
      String classpathYaml = "parameterSpaces/TestParameterSpace.yaml";

      // Act
      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(classpathYaml, parameterFactory);

      // Assert
      assertFalse(
          parameterSpace.parameters().isEmpty(), "Should load parameters from classpath resource");
    }
  }

  @Nested
  @DisplayName("When handling invalid configurations")
  class InvalidConfigurationsTest {

    @Test
    @DisplayName("should throw exception for invalid parameter type")
    void shouldThrowExceptionForInvalidParameterType() {
      // Arrange
      String invalidYaml = "src/test/resources/parameterSpaces/InvalidParameterType.yaml";

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> new YAMLParameterSpace(invalidYaml, parameterFactory),
              "Should throw RuntimeException for invalid parameter type");

      String errorMessage = exception.getMessage();
      assertTrue(
          errorMessage.contains("Unsupported parameter type: invalid_type") ||
          (exception.getCause() != null && exception.getCause().getMessage().contains("Unsupported parameter type: invalid_type")),
          "Error message should indicate unsupported parameter type. Actual message: " + errorMessage);
    }

    @Test
    @DisplayName("should throw exception for unsupported parameter type")
    void shouldThrowExceptionForUnsupportedType() {
      // Arrange
      String invalidYaml = "src/test/resources/parameterSpaces/UnsupportedTypeParameterSpace.yaml";

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> new YAMLParameterSpace(invalidYaml, parameterFactory),
              "Should throw RuntimeException for unsupported type");

      String errorMessage = exception.getMessage();
      assertTrue(
          errorMessage.contains("Unsupported parameter type: unsupported_type") ||
          (exception.getCause() != null && exception.getCause().getMessage().contains("Unsupported parameter type: unsupported_type")),
          "Error message should indicate unsupported type. Actual message: " + errorMessage);
    }

    @Test
    @DisplayName("should throw exception for invalid range values")
    void shouldThrowExceptionForInvalidRange() {
      // Arrange
      String invalidYaml = "src/test/resources/parameterSpaces/InvalidRangeParameterSpace.yaml";

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> new YAMLParameterSpace(invalidYaml, parameterFactory),
              "Should throw RuntimeException for invalid range");

      String errorMessage = exception.getMessage();
      assertTrue(
          errorMessage.contains("must be less than") ||
          (exception.getCause() != null && exception.getCause().getMessage().contains("must be less than")),
          "Error message should indicate invalid range. Actual message: " + errorMessage);
    }
  }

  @Nested
  @DisplayName("When handling parameter space operations")
  class ParameterSpaceOperationsTest {

    private YAMLParameterSpace parameterSpace;

    @BeforeEach
    void setUp() {
      String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";
      parameterSpace = new YAMLParameterSpace(yamlFilePath, parameterFactory);
    }

    @Test
    @DisplayName("should retrieve parameter by name")
    void shouldRetrieveParameterByName() {
      // Act
      Parameter<?> param = parameterSpace.get("categoricalParam");

      // Assert
      assertNotNull(param, "Should retrieve parameter by name");
      assertEquals(
          "categoricalParam", param.name(), "Retrieved parameter should have correct name");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for non-existent parameter")
    void shouldThrowExceptionForNonExistentParameter() {
      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> parameterSpace.get("nonExistentParam"),
          "Should throw IllegalArgumentException for non-existent parameter");
    }

    @Test
    @DisplayName("should return all parameters")
    void shouldReturnAllParameters() {
      // Act
      Map<String, Parameter<?>> params = parameterSpace.parameters();

      // Assert
      assertAll(
          () -> assertFalse(params.isEmpty(), "Should return non-empty parameter map"),
          () ->
              assertTrue(
                  params.containsKey("categoricalParam"), "Should contain categorical parameter"),
          () -> assertTrue(params.containsKey("integerParam"), "Should contain integer parameter"),
          () -> assertTrue(params.containsKey("doubleParam"), "Should contain double parameter"));
    }
  }

  @Nested
  @DisplayName("When handling parameter factory")
  class ParameterFactoryTest {

    @Test
    @DisplayName("should use provided parameter factory")
    void shouldUseProvidedParameterFactory() {
      // Arrange
      String yamlFilePath = "src/test/resources/parameterSpaces/TestParameterSpace.yaml";
      ParameterFactory<?> customFactory =
          new DoubleParameterFactory() {
            @Override
            public CategoricalParameter createParameter(String name, List<String> values) {
              // Custom implementation for testing
              return new CategoricalParameter(name, values);
            }
          };

      // Act
      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlFilePath, customFactory);
      Parameter<?> param = parameterSpace.get("categoricalParam");

      // Assert
      assertNotNull(param, "Should create parameter using custom factory");
      assertEquals(
          "categoricalParam", param.name(), "Should use custom factory to create parameter");
    }
  }
}
