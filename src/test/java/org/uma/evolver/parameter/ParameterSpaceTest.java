package org.uma.evolver.parameter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;

class ParameterSpaceTest {

  @Test
  @DisplayName("Getting a missing parameter names it and lists the defined ones")
  void givenMissingParameterWhenGetThenMessageNamesItAndListsDefinedOnes() {
    // Arrange
    ParameterSpace space = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());

    // Act
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> space.get("missing"));

    // Assert
    assertTrue(exception.getMessage().startsWith("Parameter not found: missing"));
    assertTrue(exception.getMessage().contains("selection"));
  }
}
