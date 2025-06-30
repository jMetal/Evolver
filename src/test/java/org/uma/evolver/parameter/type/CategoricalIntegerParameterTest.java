package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class CategoricalIntegerParameter")
class CategoricalIntegerParameterTest {

  private static final String NAME = "intCategoryParam";
  private static final List<Integer> VALID_VALUES = List.of(1, 2, 3, 5);

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("an exception is raised if the valid values list is null")
    void givenNullValidValuesThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalIntegerParameter(NAME, null);

      // Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the valid values list is empty")
    void givenEmptyValidValuesThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalIntegerParameter(NAME, List.of());

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the valid values list has duplicates")
    void givenDuplicateValuesThenAnExceptionIsThrown() {
      // Act
      Executable executable = () -> new CategoricalIntegerParameter(NAME, List.of(1, 2, 2, 3));

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("a parameter is created correctly with valid values")
    void givenValidValuesThenInstanceIsCreated() {
      // Act
      CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);

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
    @DisplayName("an exception is raised if the value is not an integer")
    void givenInvalidIntegerFormatThenAnExceptionIsThrown() {
      CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);
      String[] arguments = {"--intCategoryParam", "notAnInt"};

      Executable executable = () -> parameter.parse(arguments);

      assertThrows(NumberFormatException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter name is not found")
    void givenNonexistentParameterNameThenAnExceptionIsThrown() {
      CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);
      String[] arguments = {"--wrongParam", "2"};

      Executable executable = () -> parameter.parse(arguments);

      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the value is not in the valid list")
    void givenInvalidValueThenAnExceptionIsThrown() {
      CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);
      String[] arguments = {"--intCategoryParam", "42"};

      Executable executable = () -> parameter.parse(arguments);

      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("a valid value is parsed correctly")
    void givenValidValueThenItIsParsedCorrectly() {
      CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);
      String[] arguments = {"--another", "x", "--intCategoryParam", "3", "--something", "y"};

      parameter.parse(arguments);

      assertEquals(3, parameter.value());
    }
  }

  @Test
  @DisplayName("The toString() method includes name, value, and valid values")
  void toStringShouldIncludeNameValueAndValidValues() {
    CategoricalIntegerParameter parameter = new CategoricalIntegerParameter(NAME, VALID_VALUES);
    parameter.value(2);

    String result = parameter.toString();

    assertTrue(result.contains("Name: " + NAME));
    assertTrue(result.contains("Value: 2"));
    assertTrue(result.contains("Valid values: " + VALID_VALUES));
  }
}
