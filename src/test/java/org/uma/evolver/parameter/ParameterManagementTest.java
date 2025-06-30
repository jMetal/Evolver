package org.uma.evolver.parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.uma.evolver.parameter.ParameterManagement.decodeParameter;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;

class ParameterManagementTest {
  @Nested
  @DisplayName("Decode boolean parameter test cases")
  class DecodeBooleanParameterTestCases {
    @Test
    @DisplayName("Decode 0 returns False")
    void decodeValueZeroReturnsFalse() {
      // Arrange
      BooleanParameter parameter = new BooleanParameter("ParameterName");
      double value = 0.0;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("false", result);
    }

    @Test
    @DisplayName("Decode 0.499 returns False")
    void decodeValueZeroPoint499ReturnsFalse() {
      // Arrange
      BooleanParameter parameter = new BooleanParameter("ParameterName");
      double value = 0.499;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("false", result);
    }

    @Test
    @DisplayName("Decode 0.5 return True")
    void decodeValueZeroPointFiveReturnsTrue() {
      // Arrange
      BooleanParameter parameter = new BooleanParameter("ParameterName");
      double value = 0.5;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("true", result);
    }

    @Test
    @DisplayName("Decode 1.0 return True")
    void decodeValueOneReturnsTrue() {
      // Arrange
      BooleanParameter parameter = new BooleanParameter("ParameterName");
      double value = 1.0;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("true", result);
    }
  }

  @Nested
  @DisplayName("Decode categorical parameter test cases")
  class DecodeCategoricalParameterTestCases {
    CategoricalParameter parameter ;

    @BeforeEach
    void setup() {
      parameter =new CategoricalParameter("parameterName", List.of("A", "B", "C")) ;
    }

    @Test
    void decodeValue0ReturnsTheFirstElement() {
      // Arrange
      double value = 0.0;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("A", result);
    }

    @Test
    void decodeValueZeroPoint33ReturnsTheFirstElement() {
      // Arrange
      double value = 0.33;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("A", result);
    }

    @Test
    void decodeValueZeroPoint34ReturnsTheSecondElement() {
      // Arrange
      double value = 0.34;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("B", result);
    }

    @Test
    void decodeValueZeroPoint66ReturnsTheSecondElement() {
      // Arrange
      double value = 0.66;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("B", result);
    }

    @Test
    void decodeValueZeroPoint67ReturnsTheThirdElement() {
      // Arrange
      double value = 0.67;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("C", result);
    }

    @Test
    void decodeValueZeroPoint1ReturnsTheThirdElement() {
      // Arrange
      double value = 1.0;

      // Act
      String result = decodeParameter(parameter, value);

      // Assert
      assertEquals("C", result);
    }
  }
}
