package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class CategoricalParameter")
class CategoricalParameterTest {

  private static final String NAME = "color";
  private static final List<String> VALID_VALUES = List.of("red", "green", "blue");

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("an exception is raised if the valid values list is null")
    void givenNullValidValuesThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalParameter(NAME, null);

      // Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the valid values list is empty")
    void givenEmptyValidValuesThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalParameter(NAME, List.of());

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the name is null")
    void givenNullNameThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalParameter(null, VALID_VALUES);

      // Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the list of valid values contains duplicates")
    void givenDuplicateValidValuesThenAnExceptionIsThrown() {
      // Arrange
      List<String> duplicatedValues = Arrays.asList("A", "B", "A");

      // Act
      Executable executable = () -> new CategoricalParameter(NAME, duplicatedValues);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("a CategoricalParameter instance is created with valid name and values")
    void givenValidNameAndValuesThenInstanceIsCreated() {
      // Act
      CategoricalParameter parameter = new CategoricalParameter(NAME, VALID_VALUES);

      // Assert
      assertNotNull(parameter);
      assertEquals(NAME, parameter.name());
      assertEquals(VALID_VALUES, parameter.validValues());
    }
  }

  @Nested
  @DisplayName("When calling the parse method")
  class ParseMethodTests {

    @Test
    @DisplayName("an exception is raised if the value is not in the valid list")
    void givenInvalidValueThenAnExceptionIsThrown() {
      // Arrange
      CategoricalParameter parameter = new CategoricalParameter(NAME, VALID_VALUES);
      String[] arguments = {"--color", "yellow"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter is not found")
    void givenMissingParameterNameThenAnExceptionIsThrown() {
      // Arrange
      CategoricalParameter parameter = new CategoricalParameter(NAME, VALID_VALUES);
      String[] arguments = {"--size", "red"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("the value is set correctly if it is valid")
    void givenValidValueThenValueIsSet() {
      // Arrange
      CategoricalParameter parameter = new CategoricalParameter(NAME, VALID_VALUES);
      String[] arguments = {"--color", "green"};

      // Act
      parameter.parse(arguments);

      // Assert
      assertEquals("green", parameter.value());
    }
  }

  @Test
  @DisplayName("A call to the toString() method prints name, value, and valid values")
  void givenParameterWhenCallingToStringThenIncludesExpectedFields() {
    // Arrange
    CategoricalParameter parameter = new CategoricalParameter(NAME, VALID_VALUES);
    parameter.value("blue");

    // Act
    String result = parameter.toString();

    // Assert
    assertTrue(result.contains("Name: " + NAME));
    assertTrue(result.contains("Value: blue"));
    assertTrue(result.contains("Valid values: " + VALID_VALUES));
  }
}
