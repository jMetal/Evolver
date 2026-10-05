package org.uma.evolver.irace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class IraceParameterDescriptionGenerator")
class IraceParameterDescriptionGeneratorTest {

  @Nested
  @DisplayName("When generating the parameter file of a parameter space: ")
  class DescriptionTestCases {

    @Test
    @DisplayName(
        "given NSGAIIDouble.yaml and the Double factory, when generated, then it matches the"
            + " bundled irace parameter file")
    void givenNSGAIIDouble_whenGenerated_thenItMatchesTheBundledParameterFile()
        throws IOException {
      // Arrange
      String expected;
      try (InputStream stream =
          getClass().getClassLoader().getResourceAsStream("irace/parameters-NSGAII.txt")) {
        expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }

      // Act
      String description =
          IraceParameterDescriptionGenerator.description("NSGAIIDouble.yaml", "Double");

      // Assert
      assertEquals(expected.strip(), description.strip());
    }

    @ParameterizedTest(name = "{0} with the {1} factory")
    @CsvSource({
      "AGEMOEADouble.yaml, Double",
      "MOEADBinary.yaml, Binary",
      "MOEADDouble.yaml, Double",
      "MOEADPermutation.yaml, Permutation",
      "MOPSO.yaml, MOPSO",
      "NSGAIIBinary.yaml, Binary",
      "NSGAIIDouble.yaml, Double",
      "NSGAIIIDouble.yaml, Double",
      "NSGAIIPermutation.yaml, Permutation",
      "PAESBinary.yaml, Binary",
      "PAESDouble.yaml, Double",
      "PAESPermutation.yaml, Permutation",
      "RDEMOEADouble.yaml, Double",
      "RDEMOEAPermutation.yaml, Permutation",
      "RVEADouble.yaml, Double",
      "SMSEMOABinary.yaml, Binary",
      "SMSEMOADouble.yaml, Double",
      "SMSEMOAPermutation.yaml, Permutation",
      "SSMOEADouble.yaml, Double"
    })
    @DisplayName(
        "given a base-level parameter space and its factory, when generated, then every"
            + " top-level parameter is described")
    void givenBaseLevelParameterSpace_whenGenerated_thenItIsNotEmpty(
        String parameterSpaceFile, String factoryName) {
      // Act
      String description =
          IraceParameterDescriptionGenerator.description(parameterSpaceFile, factoryName);

      // Assert
      assertFalse(description.isBlank());
      assertTrue(description.contains("\"--"));
    }
  }

  @Nested
  @DisplayName("When choosing the parameter factory: ")
  class ParameterFactoryTestCases {

    @Test
    @DisplayName("given every accepted name, when resolved, then a factory is returned")
    void givenAcceptedNames_whenResolved_thenAFactoryIsReturned() {
      // Act & Assert
      for (String name : IraceParameterDescriptionGenerator.FACTORY_NAMES) {
        assertTrue(IraceParameterDescriptionGenerator.parameterFactory(name) != null, name);
      }
    }

    @Test
    @DisplayName("given an unknown name, when resolved, then a JMetalException is thrown")
    void givenUnknownName_whenResolved_thenAJMetalExceptionIsThrown() {
      // Act & Assert
      assertThrows(
          JMetalException.class,
          () -> IraceParameterDescriptionGenerator.parameterFactory("Tree"));
    }
  }
}
