package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.cli.BaseAlgorithmRegistry;

/**
 * Coherence test for {@link DescribeMain}'s manifest: every name the registries claim to support
 * must actually resolve, so the metadata tables added for introspection ({@link
 * BaseAlgorithmRegistry#registeredAlgorithms()}, {@link MetaAlgorithmRegistry#registeredAlgorithms()})
 * cannot drift from the {@code switch}-based resolution logic they sit alongside — see {@code
 * docs/proposals/cli-describe-manifest.md}.
 */
@DisplayName("Unit tests for class DescribeMain")
class DescribeMainTest {

  @Nested
  @DisplayName("When checking that every registered base algorithm actually resolves: ")
  class BaseAlgorithmCoherenceTestCases {

    @Test
    @DisplayName(
        "given every BaseAlgorithmRegistry.registeredAlgorithms() entry, when resolved with"
            + " minimal arguments, then none of them fail")
    void givenRegisteredBaseAlgorithms_whenResolved_thenNoneFail() {
      // Arrange
      List<BaseAlgorithmRegistry.BaseAlgorithmDescriptor> algorithms =
          BaseAlgorithmRegistry.registeredAlgorithms();
      assertFalse(algorithms.isEmpty());

      // Act & Assert
      for (BaseAlgorithmRegistry.BaseAlgorithmDescriptor algorithm : algorithms) {
        Map<String, String> extraConfig =
            algorithm.requiredExtraConfigKeys().stream()
                .collect(java.util.stream.Collectors.toMap(key -> key, key -> "placeholder"));
        assertDoesNotThrow(
            () ->
                BaseAlgorithmRegistry.resolve(
                    algorithm.name(),
                    algorithm.encoding(),
                    10,
                    minimalParameterSpaceFor(algorithm),
                    extraConfig),
            "registered but not resolvable: " + algorithm.name());
      }
    }

    private org.uma.evolver.parameter.ParameterSpace minimalParameterSpaceFor(
        BaseAlgorithmRegistry.BaseAlgorithmDescriptor algorithm) {
      String fileName =
          switch (algorithm.name() + "/" + algorithm.encoding()) {
            case "MOEAD/Double" -> "MOEADDouble.yaml";
            case "NSGA-II/Permutation" -> "NSGAIIPermutation.yaml";
            default -> "NSGAIIDouble.yaml";
          };
      return BaseAlgorithmRegistry.resolveParameterSpace(algorithm.encoding(), fileName);
    }
  }

  @Nested
  @DisplayName("When checking that every registered meta-algorithm actually classifies: ")
  class MetaAlgorithmCoherenceTestCases {

    @Test
    @DisplayName(
        "given every MetaAlgorithmRegistry.registeredAlgorithms() entry, when familyOf is"
            + " called, then it matches the descriptor's own family")
    void givenRegisteredMetaAlgorithms_whenFamilyOfCalled_thenItMatches() {
      // Arrange
      List<MetaAlgorithmRegistry.MetaAlgorithmDescriptor> algorithms =
          MetaAlgorithmRegistry.registeredAlgorithms();
      assertFalse(algorithms.isEmpty());

      // Act & Assert
      for (MetaAlgorithmRegistry.MetaAlgorithmDescriptor algorithm : algorithms) {
        assertEquals(
            algorithm.family(),
            MetaAlgorithmRegistry.familyOf(algorithm.name()),
            "registered family mismatch for: " + algorithm.name());
      }
    }

    @Test
    @DisplayName(
        "given every registered algorithm claiming tree support, when validateTreeAlgorithm is"
            + " called, then it does not fail")
    void givenTreeSupportingAlgorithms_whenValidateTreeAlgorithmCalled_thenItDoesNotFail() {
      // Arrange & Act & Assert
      MetaAlgorithmRegistry.registeredAlgorithms().stream()
          .filter(MetaAlgorithmRegistry.MetaAlgorithmDescriptor::supportsTree)
          .forEach(
              algorithm ->
                  assertDoesNotThrow(
                      () -> MetaAlgorithmRegistry.validateTreeAlgorithm(algorithm.name()),
                      "claims tree support but validateTreeAlgorithm rejects it: "
                          + algorithm.name()));
    }
  }

  @Nested
  @DisplayName("When building the manifest: ")
  class ManifestTestCases {

    @Test
    @DisplayName("given the registries, when manifest is built, then it lists every registry")
    void givenRegistries_whenManifestBuilt_thenItListsEveryRegistry() {
      // Arrange & Act
      Map<String, Object> manifest = DescribeMain.manifest();

      // Assert
      assertEquals(3, ((List<?>) manifest.get("baseAlgorithms")).size());
      assertEquals(6, ((List<?>) manifest.get("metaAlgorithms")).size());
      assertTrue(((List<?>) manifest.get("problems")).contains("ZDT4"));
      assertTrue(((List<?>) manifest.get("indicators")).contains("Epsilon"));
      assertTrue(manifest.containsKey("resourceDirectories"));
      assertTrue(manifest.containsKey("schemas"));
    }

    @Test
    @DisplayName(
        "given the solve request record, when manifest is built, then its schema marks the"
            + " configuration fields as optional and the problem as required")
    @SuppressWarnings("unchecked")
    void givenSolveRequest_whenManifestBuilt_thenSchemaMarksOptionalFields() {
      // Arrange & Act
      Map<String, Object> schemas = (Map<String, Object>) DescribeMain.manifest().get("schemas");
      List<Map<String, Object>> fields = (List<Map<String, Object>>) schemas.get("solveRequest");

      // Assert
      Map<String, Boolean> required = new HashMap<>();
      fields.forEach(
          field -> required.put((String) field.get("name"), (Boolean) field.get("required")));
      assertTrue(required.get("problem"));
      assertTrue(required.get("maxEvaluations"));
      assertFalse(required.get("configuration"));
      assertFalse(required.get("configurationFile"));
      assertFalse(required.get("seed"));
    }
  }
}
