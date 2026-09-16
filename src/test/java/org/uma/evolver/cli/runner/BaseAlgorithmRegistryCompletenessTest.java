package org.uma.evolver.cli.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drift-detection test: every concrete {@code org.uma.evolver.algorithm.*} class must be
 * acknowledged here, either as registered in {@link BaseAlgorithmRegistry} or as a documented,
 * deliberate omission. Adding a new base-level algorithm/encoding class without updating either
 * this map or {@code BaseAlgorithmRegistry} fails this test, instead of silently going unnoticed.
 *
 * <p>Evolver-Studio (the companion Streamlit tool) keeps its own mirror of this same information
 * in {@code evolver_studio/catalogue.py}, with an analogous drift-detection test there
 * ({@code tests/test_catalogue.py}) that scans the same {@code parameterSpaces/} directory this
 * class's algorithms drive. See Evolver-Studio's {@code CLAUDE.md}, "Relationship to Evolver".
 */
@DisplayName("Unit tests for class BaseAlgorithmRegistry completeness")
class BaseAlgorithmRegistryCompletenessTest {

  private static final Path ALGORITHM_SOURCE_ROOT =
      Paths.get("src/main/java/org/uma/evolver/algorithm");

  // One entry per concrete (non-"Base*") class under org.uma.evolver.algorithm/**, explaining its
  // status. Update this map whenever a class is added, removed, or newly wired into
  // BaseAlgorithmRegistry — that is the point of this test.
  private static final Map<String, String> KNOWN_ALGORITHM_CLASSES =
      Map.ofEntries(
          Map.entry("DoubleNSGAII", "registered in BaseAlgorithmRegistry as \"NSGA-II\""),
          Map.entry("DoubleMOEAD", "registered in BaseAlgorithmRegistry as \"MOEAD\""),
          Map.entry("BinaryNSGAII", "not yet registered"),
          Map.entry("PermutationNSGAII", "not yet registered"),
          Map.entry("BinaryMOEAD", "not yet registered"),
          Map.entry("PermutationMOEAD", "not yet registered"),
          Map.entry("DoubleSMSEMOA", "not yet registered"),
          Map.entry("BinarySMSEMOA", "not yet registered"),
          Map.entry("PermutationSMSEMOA", "not yet registered"),
          Map.entry("DoubleRDEMOEA", "not yet registered"),
          Map.entry("PermutationRDEMOEA", "not yet registered"),
          Map.entry("DoubleAGEMOEA", "not yet registered"),
          Map.entry("DoubleRVEA", "not yet registered"),
          Map.entry("BaseMOPSO", "not yet registered (particle swarm, no per-encoding subclasses)"));

  @Test
  @DisplayName(
      "given the algorithm source tree, when scanning for concrete algorithm classes, then every"
          + " one is accounted for in KNOWN_ALGORITHM_CLASSES")
  void givenAlgorithmSourceTree_whenScanning_thenEveryClassIsAccountedFor() throws IOException {
    // Arrange & Act
    List<String> untriaged;
    try (Stream<Path> files = Files.walk(ALGORITHM_SOURCE_ROOT, 2)) {
      // Concrete algorithm classes live one level down, in a per-family subpackage (e.g.
      // nsgaii/DoubleNSGAII.java) -- files directly under algorithm/ are shared infrastructure
      // (BaseLevelAlgorithm.java, EvolutionaryAlgorithmBuilder.java,
      // ParticleSwarmOptimizationBuilder.java), not algorithm implementations.
      untriaged =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .filter(path -> !path.getParent().equals(ALGORITHM_SOURCE_ROOT))
              .map(path -> path.getFileName().toString().replace(".java", ""))
              .filter(name -> !name.startsWith("Base"))
              .filter(name -> !KNOWN_ALGORITHM_CLASSES.containsKey(name))
              .sorted()
              .toList();
    }

    // Assert
    assertTrue(
        untriaged.isEmpty(),
        "New base-level algorithm class(es) not yet triaged: "
            + untriaged
            + ". Either wire them into BaseAlgorithmRegistry.resolve(), or add them to"
            + " KNOWN_ALGORITHM_CLASSES in this test with a reason.");
  }
}
