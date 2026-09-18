package org.uma.evolver.cli.runner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("given ParallelNSGA-II, when familyOf is called, then it returns EVOLUTIONARY")
    void givenParallelNSGAII_whenFamilyOfCalled_thenItReturnsEvolutionary() {
      // Arrange & Act & Assert
      assertEquals(
          MetaAlgorithmRegistry.Family.EVOLUTIONARY,
          MetaAlgorithmRegistry.familyOf("ParallelNSGA-II"));
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
    @DisplayName(
        "given an unknown algorithm name, when familyOf is called, then it fails listing the"
            + " supported algorithms")
    void givenUnknownAlgorithm_whenFamilyOfCalled_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> MetaAlgorithmRegistry.familyOf("SMPSO"));
      assertTrue(exception.getMessage().contains("ParallelNSGA-II"));
      assertTrue(exception.getMessage().contains("AsyncNSGA-II"));
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
              () -> MetaAlgorithmRegistry.resolveFlat("SMPSO", null, null));
      assertTrue(exception.getMessage().contains("ParallelNSGA-II"));
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
          () -> MetaAlgorithmRegistry.resolveFlatAsync("ParallelNSGA-II", null, null));
    }
  }

  @Nested
  @DisplayName("When validating a tree-encoding meta-optimizer algorithm: ")
  class ValidateTreeTestCases {

    @Test
    @DisplayName("given ParallelNSGA-II, when validateTreeAlgorithm is called, then it does not fail")
    void givenParallelNSGAII_whenValidateTreeAlgorithmCalled_thenItDoesNotFail() {
      // Arrange & Act & Assert
      assertDoesNotThrow(() -> MetaAlgorithmRegistry.validateTreeAlgorithm("ParallelNSGA-II"));
    }

    @Test
    @DisplayName(
        "given an unknown algorithm, when validateTreeAlgorithm is called, then it fails listing"
            + " the supported algorithms")
    void givenUnknownAlgorithm_whenValidateTreeAlgorithmCalled_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> MetaAlgorithmRegistry.validateTreeAlgorithm("SMPSO"));
      assertTrue(exception.getMessage().contains("ParallelNSGA-II"));
    }
  }
}
