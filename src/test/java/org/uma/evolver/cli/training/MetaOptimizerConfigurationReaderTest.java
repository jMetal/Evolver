package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class MetaOptimizerConfigurationReader")
class MetaOptimizerConfigurationReaderTest {

  @Nested
  @DisplayName("When loading a flat-encoding meta-optimizer configuration file: ")
  class FlatConfigurationTestCases {

    @Test
    @DisplayName(
        "given MetaNSGAIIFlatConfiguration.yaml, when loaded, then a FlatMetaSearchConfig"
            + " with the NSGA-II recipe is built")
    void givenParallelFlatFile_whenLoaded_thenFlatConfigIsBuilt() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaNSGAIIFlatConfiguration.yaml");

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("NSGA-II", flat.algorithm());
      assertEquals(2000, flat.metaMaxEvaluations());
      assertEquals(50, flat.metaPopulationSize());
      assertEquals(8, flat.numberOfCores());
      assertTrue(flat.operatorFlags().containsAll(java.util.List.of("--crossover", "SBX")));
      assertTrue(flat.operatorFlags().containsAll(java.util.List.of("--selection", "tournament")));
    }

    @Test
    @DisplayName(
        "given MetaAsyncNSGAIIFlatConfiguration.yaml, when loaded, then a FlatMetaSearchConfig"
            + " with the async NSGA-II recipe is built, without a selection flag")
    void givenAsyncFlatFile_whenLoaded_thenFlatConfigIsBuiltWithoutSelection() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaAsyncNSGAIIFlatConfiguration.yaml");

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("AsyncNSGA-II", flat.algorithm());
      assertTrue(flat.operatorFlags().contains("--crossover"));
      assertTrue(!flat.operatorFlags().contains("--selection"));
    }

    @Test
    @DisplayName(
        "given MetaSMPSOFlatConfiguration.yaml, when loaded, then a FlatMetaSearchConfig with the"
            + " SMPSO recipe and no operator flags is built")
    void givenSmpsoFlatFile_whenLoaded_thenFlatConfigIsBuiltWithoutOperatorFlags() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaSMPSOFlatConfiguration.yaml");

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("SMPSO", flat.algorithm());
      assertEquals(50, flat.metaPopulationSize());
      assertTrue(flat.operatorFlags().isEmpty());
    }

    @Test
    @DisplayName(
        "given MetaSPEA2FlatConfiguration.yaml, when loaded, then a FlatMetaSearchConfig with the"
            + " SPEA2 recipe is built")
    void givenSpea2FlatFile_whenLoaded_thenFlatConfigIsBuilt() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaSPEA2FlatConfiguration.yaml");

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("SPEA2", flat.algorithm());
      assertEquals(100, flat.metaPopulationSize());
    }

    @Test
    @DisplayName(
        "given MetaRandomSearchFlatConfiguration.yaml, when loaded, then a FlatMetaSearchConfig"
            + " with the RandomSearch recipe, no population size and no operator flags is built")
    void givenRandomSearchFlatFile_whenLoaded_thenFlatConfigIsBuiltWithoutPopulationOrFlags() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaRandomSearchFlatConfiguration.yaml");

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("RandomSearch", flat.algorithm());
      assertNull(flat.metaPopulationSize());
      assertTrue(flat.operatorFlags().isEmpty());
    }

    @Test
    @DisplayName(
        "given inline YAML text, when loaded via loadFromYaml, then a FlatMetaSearchConfig is"
            + " built from it, the same as from a file")
    void givenInlineYaml_whenLoadedFromYaml_thenFlatConfigIsBuilt() {
      // Arrange
      String yaml =
          """
          algorithm: AsyncNSGA-II
          encoding: flat
          metaMaxEvaluations: 2000
          metaPopulationSize: 50
          numberOfCores: 8
          crossover: SBX
          mutation: polynomial
          crossoverProbability: 0.9
          crossoverRepairStrategy: bounds
          sbxDistributionIndex: 20.0
          mutationProbabilityFactor: 1.0
          mutationRepairStrategy: bounds
          polynomialMutationDistributionIndex: 20.0
          """;

      // Act
      MetaSearchConfig config = MetaOptimizerConfigurationReader.loadFromYaml(yaml);

      // Assert
      FlatMetaSearchConfig flat = (FlatMetaSearchConfig) config;
      assertEquals("AsyncNSGA-II", flat.algorithm());
      assertEquals(50, flat.metaPopulationSize());
      assertTrue(flat.operatorFlags().containsAll(java.util.List.of("--crossover", "SBX")));
    }
  }

  @Nested
  @DisplayName("When loading a tree-encoding meta-optimizer configuration file: ")
  class TreeConfigurationTestCases {

    @Test
    @DisplayName(
        "given MetaNSGAIITreeConfiguration.yaml, when loaded, then a TreeMetaSearchConfig"
            + " is built")
    void givenParallelTreeFile_whenLoaded_thenTreeConfigIsBuilt() {
      // Arrange & Act
      MetaSearchConfig config =
          MetaOptimizerConfigurationReader.load("MetaNSGAIITreeConfiguration.yaml");

      // Assert
      TreeMetaSearchConfig tree = (TreeMetaSearchConfig) config;
      assertEquals("NSGA-II", tree.algorithm());
      assertEquals(50, tree.metaOffspringSize());
      assertEquals(0.9, tree.crossoverProbability());
    }
  }

  @Nested
  @DisplayName("When loading an invalid meta-optimizer configuration reference: ")
  class InvalidConfigurationTestCases {

    @Test
    @DisplayName("given a non-existent file name, when loaded, then it fails naming that file")
    void givenNonExistentFile_whenLoaded_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaOptimizerConfigurationReader.load("DoesNotExist.yaml"));
      assertTrue(exception.getMessage().contains("DoesNotExist.yaml"));
    }
  }
}
