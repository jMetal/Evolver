package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class BaseLevelConfigurationReader")
class BaseLevelConfigurationReaderTest {

  @Nested
  @DisplayName("When loading an existing base-level configuration file: ")
  class ValidConfigurationTestCases {

    @Test
    @DisplayName(
        "given Zdt4NSGAIIBaseLevel.yaml, when loaded, then a BaseLevelConfig with the ZDT4 recipe"
            + " is built")
    void givenZdt4File_whenLoaded_thenConfigIsBuilt() {
      // Arrange & Act
      BaseLevelConfig config = BaseLevelConfigurationReader.load("Zdt4NSGAIIBaseLevel.yaml");

      // Assert
      assertEquals("NSGA-II", config.algorithmName());
      assertEquals(100, config.populationSize());
      assertEquals(1, config.numberOfIndependentRuns());
      assertEquals("NSGAIIDouble.yaml", config.yamlParameterSpaceFile());
      assertEquals(java.util.List.of("ZDT4"), config.trainingProblemNames());
    }

    @Test
    @DisplayName(
        "given MoeadZdt4BaseLevel.yaml, when loaded, then extraConfig carries"
            + " weightVectorFilesDirectory")
    void givenMoeadFile_whenLoaded_thenExtraConfigIsCarried() {
      // Arrange & Act
      BaseLevelConfig config = BaseLevelConfigurationReader.load("MoeadZdt4BaseLevel.yaml");

      // Assert
      assertEquals("MOEAD", config.algorithmName());
      assertEquals("resources/weightVectors", config.extraConfig().get("weightVectorFilesDirectory"));
    }

    @Test
    @DisplayName(
        "given inline YAML text, when loaded via loadFromYaml, then a BaseLevelConfig is built"
            + " from it, the same as from a file")
    void givenInlineYaml_whenLoadedFromYaml_thenConfigIsBuilt() {
      // Arrange
      String yaml =
          """
          algorithmName: NSGA-II
          populationSize: 100
          numberOfIndependentRuns: 1
          yamlParameterSpaceFile: NSGAIIDouble.yaml
          trainingProblemNames: [DTLZ1]
          trainingReferenceFrontFileNames: [resources/referenceFronts/DTLZ1.3D.csv]
          trainingEvaluations: [16000]
          indicatorNames: [Epsilon]
          """;

      // Act
      BaseLevelConfig config = BaseLevelConfigurationReader.loadFromYaml(yaml);

      // Assert
      assertEquals("NSGA-II", config.algorithmName());
      assertEquals(java.util.List.of("DTLZ1"), config.trainingProblemNames());
    }
  }

  @Nested
  @DisplayName("When loading an invalid base-level configuration reference: ")
  class InvalidConfigurationTestCases {

    @Test
    @DisplayName("given a non-existent file name, when loaded, then it fails naming that file")
    void givenNonExistentFile_whenLoaded_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> BaseLevelConfigurationReader.load("DoesNotExist.yaml"));
      assertTrue(exception.getMessage().contains("DoesNotExist.yaml"));
    }

    @Test
    @DisplayName(
        "given inline YAML text missing a required field, when loaded via loadFromYaml, then it"
            + " fails naming that field, not a file")
    void givenInlineYamlMissingField_whenLoadedFromYaml_thenItFails() {
      // Arrange
      String yaml = "algorithmName: NSGA-II\n";

      // Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> BaseLevelConfigurationReader.loadFromYaml(yaml));
      assertTrue(exception.getMessage().contains("populationSize"));
    }
  }
}
