package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class MetaAlgorithmRegistry")
class MetaAlgorithmRegistryTest {

  @Nested
  @DisplayName("When classifying a flat-encoding meta-optimizer algorithm's family: ")
  class FamilyOfTestCases {

    @Test
    @DisplayName("given NSGA-II, when familyOf is called, then it returns EVOLUTIONARY")
    void givenNSGAII_whenFamilyOfCalled_thenItReturnsEvolutionary() {
      // Arrange & Act & Assert
      assertEquals(
          MetaAlgorithmRegistry.Family.EVOLUTIONARY,
          MetaAlgorithmRegistry.familyOf("NSGA-II"));
    }

    @Test
    @DisplayName("given AsyncNSGA-II, when familyOf is called, then it returns ASYNCHRONOUS")
    void givenAsyncNSGAII_whenFamilyOfCalled_thenItReturnsAsynchronous() {
      // Arrange & Act & Assert
      assertEquals(
          MetaAlgorithmRegistry.Family.ASYNCHRONOUS,
          MetaAlgorithmRegistry.familyOf("AsyncNSGA-II"));
    }

    @Test
    @DisplayName("given SPEA2, when familyOf is called, then it returns EVOLUTIONARY")
    void givenSpea2_whenFamilyOfCalled_thenItReturnsEvolutionary() {
      // Arrange & Act & Assert
      assertEquals(MetaAlgorithmRegistry.Family.EVOLUTIONARY, MetaAlgorithmRegistry.familyOf("SPEA2"));
    }

    @Test
    @DisplayName("given SMPSO, when familyOf is called, then it returns PARTICLE_SWARM")
    void givenSmpso_whenFamilyOfCalled_thenItReturnsParticleSwarm() {
      // Arrange & Act & Assert
      assertEquals(
          MetaAlgorithmRegistry.Family.PARTICLE_SWARM, MetaAlgorithmRegistry.familyOf("SMPSO"));
    }

    @Test
    @DisplayName("given RandomSearch, when familyOf is called, then it returns RANDOM_SEARCH")
    void givenRandomSearch_whenFamilyOfCalled_thenItReturnsRandomSearch() {
      // Arrange & Act & Assert
      assertEquals(
          MetaAlgorithmRegistry.Family.RANDOM_SEARCH,
          MetaAlgorithmRegistry.familyOf("RandomSearch"));
    }

    @Test
    @DisplayName(
        "given an unknown algorithm name, when familyOf is called, then it fails listing the"
            + " supported algorithms")
    void givenUnknownAlgorithm_whenFamilyOfCalled_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaAlgorithmRegistry.familyOf("Unknown-Algorithm"));
      assertTrue(exception.getMessage().contains("NSGA-II"));
      assertTrue(exception.getMessage().contains("AsyncNSGA-II"));
      assertTrue(exception.getMessage().contains("SPEA2"));
      assertTrue(exception.getMessage().contains("SMPSO"));
      assertTrue(exception.getMessage().contains("RandomSearch"));
    }
  }

  @Nested
  @DisplayName("When resolving a flat-encoding meta-optimizer algorithm: ")
  class ResolveFlatTestCases {

    @Test
    @DisplayName(
        "given an unknown algorithm name, when resolveFlat is called, then it fails listing the"
            + " supported algorithms")
    void givenUnknownAlgorithm_whenResolveFlatCalled_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class,
              () -> MetaAlgorithmRegistry.resolveFlat("Unknown-Algorithm", null, null));
      assertTrue(exception.getMessage().contains("NSGA-II"));
    }

    @Test
    @DisplayName(
        "given an asynchronous-family algorithm name, when resolveFlat is called, then it fails")
    void givenAsynchronousAlgorithm_whenResolveFlatCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class,
          () -> MetaAlgorithmRegistry.resolveFlat("AsyncNSGA-II", null, null));
    }

    @Test
    @DisplayName(
        "given a particle-swarm-family algorithm name, when resolveFlat is called, then it fails")
    void givenParticleSwarmAlgorithm_whenResolveFlatCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class, () -> MetaAlgorithmRegistry.resolveFlat("SMPSO", null, null));
    }

    @Test
    @DisplayName(
        "given a random-search-family algorithm name, when resolveFlat is called, then it fails")
    void givenRandomSearchAlgorithm_whenResolveFlatCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class,
          () -> MetaAlgorithmRegistry.resolveFlat("RandomSearch", null, null));
    }

    @Test
    @DisplayName(
        "given NSGA-II with an offspringPopulationSize operator flag, when resolveFlat is called,"
            + " then it fails because the offspring size is fixed to the population size")
    void givenNSGAIIWithOffspringFlag_whenResolveFlatCalled_thenItFails() {
      // Arrange
      var config =
          new FlatMetaSearchConfig(
              "NSGA-II", 1000, 50, 1, List.of("--offspringPopulationSize", "10"));

      // Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaAlgorithmRegistry.resolveFlat("NSGA-II", null, config));
      assertTrue(exception.getMessage().contains("offspringPopulationSize"));
    }

    @Test
    @DisplayName(
        "given NSGA-II with an algorithmResult operator flag, when resolveFlat is called, then it"
            + " fails because the result is always the final population")
    void givenNSGAIIWithAlgorithmResultFlag_whenResolveFlatCalled_thenItFails() {
      // Arrange
      var config =
          new FlatMetaSearchConfig(
              "NSGA-II", 1000, 50, 1, List.of("--algorithmResult", "externalArchive"));

      // Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaAlgorithmRegistry.resolveFlat("NSGA-II", null, config));
      assertTrue(exception.getMessage().contains("algorithmResult"));
    }

    @Test
    @DisplayName(
        "given SPEA2 with an offspringPopulationSize operator flag, when resolveFlat is called,"
            + " then it fails listing the allowed fields")
    void givenSpea2WithOffspringFlag_whenResolveFlatCalled_thenItFails() {
      // Arrange
      var config =
          new FlatMetaSearchConfig("SPEA2", 1000, 50, 1, List.of("--offspringPopulationSize", "10"));

      // Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaAlgorithmRegistry.resolveFlat("SPEA2", null, config));
      assertTrue(exception.getMessage().contains("offspringPopulationSize"));
      assertTrue(exception.getMessage().contains("mutationProbabilityFactor"));
    }
  }

  @Nested
  @DisplayName("When resolving an asynchronous flat-encoding meta-optimizer algorithm: ")
  class ResolveFlatAsyncTestCases {

    @Test
    @DisplayName(
        "given an evolutionary-family algorithm name, when resolveFlatAsync is called, then it"
            + " fails")
    void givenEvolutionaryAlgorithm_whenResolveFlatAsyncCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class,
          () -> MetaAlgorithmRegistry.resolveFlatAsync("NSGA-II", null, null));
    }
  }

  @Nested
  @DisplayName("When resolving a particle-swarm flat-encoding meta-optimizer algorithm: ")
  class ResolveFlatPsoTestCases {

    @Test
    @DisplayName(
        "given an evolutionary-family algorithm name, when resolveFlatPso is called, then it"
            + " fails")
    void givenEvolutionaryAlgorithm_whenResolveFlatPsoCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class,
          () -> MetaAlgorithmRegistry.resolveFlatPso("NSGA-II", null, null));
    }
  }

  @Nested
  @DisplayName("When resolving a random-search flat-encoding meta-optimizer algorithm: ")
  class ResolveFlatRandomSearchTestCases {

    @Test
    @DisplayName(
        "given an evolutionary-family algorithm name, when resolveFlatRandomSearch is called,"
            + " then it fails")
    void givenEvolutionaryAlgorithm_whenResolveFlatRandomSearchCalled_thenItFails() {
      // Arrange & Act & Assert
      assertThrows(
          JMetalException.class,
          () -> MetaAlgorithmRegistry.resolveFlatRandomSearch("NSGA-II", null, null));
    }
  }

  @Nested
  @DisplayName("When validating a tree-encoding meta-optimizer algorithm: ")
  class ValidateTreeTestCases {

    @Test
    @DisplayName("given NSGA-II, when validateTreeAlgorithm is called, then it does not fail")
    void givenNSGAII_whenValidateTreeAlgorithmCalled_thenItDoesNotFail() {
      // Arrange & Act & Assert
      assertDoesNotThrow(() -> MetaAlgorithmRegistry.validateTreeAlgorithm("NSGA-II"));
    }

    @Test
    @DisplayName(
        "given an unknown algorithm, when validateTreeAlgorithm is called, then it fails listing"
            + " the supported algorithms")
    void givenUnknownAlgorithm_whenValidateTreeAlgorithmCalled_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class,
              () -> MetaAlgorithmRegistry.validateTreeAlgorithm("Unknown-Algorithm"));
      assertTrue(exception.getMessage().contains("NSGA-II"));
    }
  }
}
