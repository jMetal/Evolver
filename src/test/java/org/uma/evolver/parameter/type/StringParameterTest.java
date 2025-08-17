package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

@DisplayName("Unit tests for class StringParameter")
class StringParameterTest {

  @Test
  @DisplayName("Should parse string value correctly")
  void shouldParseStringValueCorrectly() {
    StringParameter param = new StringParameter("name");
    String expectedValue = "testValue";
    param.parse(new String[] {"--name", expectedValue});
    assertEquals(expectedValue, param.value());
  }

  @Test
  @DisplayName("Should handle empty string value")
  void shouldHandleEmptyStringValue() {
    StringParameter param = new StringParameter("description");
    param.parse(new String[] {"--description", ""});
    assertEquals("", param.value());
  }

  @Test
  @DisplayName("Should handle special characters in string value")
  void shouldHandleSpecialCharacters() {
    StringParameter param = new StringParameter("special");
    String testValue = "test@123#_!$%^&*()";
    param.parse(new String[] {"--special", testValue});
    assertEquals(testValue, param.value());
  }

  @Test
  @DisplayName("Should throw exception when value is missing")
  void shouldThrowExceptionWhenValueMissing() {
    StringParameter param = new StringParameter("required");
    assertThrows(
        InvalidConditionException.class, () -> param.parse(new String[] {"--required"}));
  }

  @Test
  @DisplayName("Should handle multiple parameters in arguments")
  void shouldHandleMultipleParameters() {
    StringParameter param1 = new StringParameter("first");
    StringParameter param2 = new StringParameter("second");

    String[] args = {"--first", "value1", "--second", "value2"};

    param1.parse(args);
    param2.parse(args);

    assertEquals("value1", param1.value());
    assertEquals("value2", param2.value());
  }
}
