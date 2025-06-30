package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class DoubleParameter")
class DoubleParameterTest {

  private static final String NAME = "testParameter";
  private static final double LOWER_BOUND = 0.0;
  private static final double UPPER_BOUND = 10.0;

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("an exception is raised if the upper bound is lower than the lower bound")
    void givenAnUpperBoundLowerThanTheLowerBoundThenAnExceptionIsThrown() {
      // Arrange
      double invalidLowerBound = 10.0;
      double invalidUpperBound = 5.0;

      // Act
      Executable executable = () -> new DoubleParameter(NAME, invalidLowerBound, invalidUpperBound);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the lower bound is equal to the upper bound")
    void givenAnUpperBoundEqualToTheLowerBoundThenAnExceptionIsThrown() {
      // Arrange
      double invalidLowerBound = 10.0;
      double invalidUpperBound = 10.0;

      // Act
      Executable executable = () -> new DoubleParameter(NAME, invalidLowerBound, invalidUpperBound);

      // Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the name is null")
    void givenANullNameThenAnExceptionIsThrown() {
      // Arrange
      String invalidName = null;

      // Act
      Executable executable = () -> new DoubleParameter(invalidName, LOWER_BOUND, UPPER_BOUND);

      // Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("a RealParameter instance is created with valid bounds and name")
    void givenValidBoundsAndNameThenAnInstanceIsCreated() {
      // Arrange & Act
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);

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
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
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
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
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
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--testParameter", "-1.0"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("an exception is raised if the parameter value is greater than the upper bound")
    void givenAnUpperBoundViolationThenAnExceptionIsThrown() {
      // Arrange
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments = {"--testParameter", "11.0"};

      // Act
      Executable executable = () -> parameter.parse(arguments);

      // Assert
      assertThrows(JMetalException.class, executable);
    }

    @Test
    @DisplayName("the value is set correctly if the argument is a valid double")
    void givenValidStringArgumentWhenParsingThenTheValueIsSetCorrectly() {
      // Arrange
      DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
      String[] arguments =
          new String[] {
            "--otherParameter", "hello", "--testParameter", "5.5", "--anotherParameter", "10"
          };

      // Act
      parameter.parse(arguments);

      // Assert
      assertEquals(5.5, parameter.value());
    }
  }

  @Test
  @DisplayName("A call to the toString() method prints name, value, and bounds")
  void givenParameterWhenCallingToStringThenIncludesNameValueAndBounds() {
    // Arrange
    DoubleParameter parameter = new DoubleParameter(NAME, LOWER_BOUND, UPPER_BOUND);
    parameter.value(7.0);

    // Act
    String result = parameter.toString();
    System.out.println(result) ;

    // Assert
    assertTrue(result.contains("Name: " + NAME));
    assertTrue(result.contains("Value: 7.0"));
    assertTrue(result.contains("Lower bound: " + LOWER_BOUND));
    assertTrue(result.contains("Upper bound: " + UPPER_BOUND));
  }
}
