package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class IntegerParameter")
class IntegerParameterTest {

  private static final String NAME = "testParameter";
  private static final int LOWER_BOUND = 1;
  private static final int UPPER_BOUND = 15;

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("an exception is raised if the upper bound is lower than the lower bound")
    void givenAnUpperBoundLowerThanTheLowerBoundThenAnExceptionIsThrown() {
      // Arrange
      int invalidLowerBound = 10;
      int invalidUpperBound = 5;

      // Act
      Executable executable =
          () -> new IntegerParameter(NAME, invalidLowerBound, invalidUpperBound);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the lower bound is equal to the upper bound")
    void givenAnUpperBoundEqualToTheLowerBoundThenAnExceptionIsThrown() {
      // Arrange
      int invalidLowerBound = 10;
      int invalidUpperBound = 10;

      // Act
      Executable executable =
          () -> new IntegerParameter(NAME, invalidLowerBound, invalidUpperBound);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the name is null")
    void givenANullNameThenAnExceptionIsThrown() {
      // Arrange
      String invalidName = null;

      // Act
      Executable executable = () -> new IntegerParameter(invalidName, LOWER_BOUND, UPPER_BOUND);

      // Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("a RealParameter instance is created with valid bounds and name")
    void givenValidBoundsAndNameThenAnInstanceIsCreated() {
      // Arrange & Act
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);

      // Assert
      assertNotNull(parameter);
      assertEquals(NAME, parameter.name());
      assertEquals(LOWER_BOUND, parameter.lowerBound);
      assertEquals(UPPER_BOUND, parameter.upperBound);
    }
  }

  @Nested
  @DisplayName("When calling the parse method")
  class ParseMethodTests {

    @Test
    @DisplayName("an exception is raised if the argument is not a valid double")
    void givenAnInvalidArgumentThenAnExceptionIsThrown() {
      // Arrange
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--testParameter", "invalid"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(NumberFormatException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter is not found")
    void givenANonExistentParameterThenAnExceptionIsThrown() {
      // Arrange
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--nonExistentParameter", "5.5"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter value is lower than the lower bound")
    void givenALowerBoundViolationThenAnExceptionIsThrown() {
      // Arrange
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--testParameter", "-1"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter value is greater than the upper bound")
    void givenAnUpperBoundViolationThenAnExceptionIsThrown() {
      // Arrange
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--testParameter", "20"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("the value is set correctly if the argument is a valid integer")
    void givenValidStringArgumentWhenParsingThenTheValueIsSetCorrectly() {
      // Arrange
      IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments =
          new String[] {
            "--otherParameter", "hello", "--testParameter", "5", "--anotherParameter", "10"
          };

      // Act
      parameter.parse(arguments);

      // Assert
      assertEquals(5, parameter.value());
    }
  }

  @Test
  @DisplayName("A call to the toString() method prints name, value, and bounds")
  void givenParameterWhenCallingToStringThenIncludesNameValueAndBounds() {
    // Arrange
    IntegerParameter parameter = new IntegerParameter(NAME, LOWER_BOUND, UPPER_BOUND);
    parameter.value(7);

    // Act
    String result = parameter.toString();
    System.out.println(result);

    // Assert
    assertTrue(result.contains("Name: " + NAME));
    assertTrue(result.contains("Value: 7"));
    assertTrue(result.contains("Lower bound: " + LOWER_BOUND));
    assertTrue(result.contains("Upper bound: " + UPPER_BOUND));
  }
}
