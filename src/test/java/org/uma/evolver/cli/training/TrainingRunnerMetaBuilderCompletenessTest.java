package org.uma.evolver.cli.training;

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
 * Drift-detection test: every {@code org.uma.evolver.meta.builder.Meta*Builder} class must be
 * acknowledged here, either as wired into {@link TrainingRunner} or as a documented, deliberate
 * omission. Adding a new meta-optimizer builder without updating either this map or
 * {@code TrainingRunner} fails this test, instead of silently going unnoticed.
 *
 * <p>See {@link BaseAlgorithmRegistryCompletenessTest} for the base-level-algorithm equivalent of
 * this same mechanism, and Evolver-Studio's {@code evolver_studio/catalogue.py} /
 * {@code tests/test_catalogue.py} for its Python-side mirror.
 */
@DisplayName("Unit tests for class TrainingRunner meta-builder completeness")
class TrainingRunnerMetaBuilderCompletenessTest {

  private static final Path META_BUILDER_SOURCE_ROOT =
      Paths.get("src/main/java/org/uma/evolver/meta/builder");

  // One entry per Meta*Builder class, explaining its status. Update this map whenever a builder
  // is added, removed, or newly wired into TrainingRunner.runFlat()/runTree() — that is the point
  // of this test. RandomSearch.java is a jMetal-side algorithm class, not a Meta*Builder, and is
  // intentionally not matched by this test's naming filter.
  private static final Map<String, String> KNOWN_META_BUILDER_CLASSES =
      Map.ofEntries(
          Map.entry(
              "MetaNSGAIIBuilder",
              "not wired into TrainingRunner; superseded there by MetaAlgorithmRegistry, which"
                  + " builds DoubleNSGAII directly with a request-supplied parameter space/flags"
                  + " instead of this builder's hardcoded SBX+polynomial. Still used from"
                  + " org.uma.evolver.example.training."),
          Map.entry(
              "MetaSPEA2Builder",
              "wired into TrainingRunner.runFlat() via MetaAlgorithmRegistry, as \"SPEA2\" (same"
                  + " EvolutionaryAlgorithm shape as ParallelNSGA-II); only populationSize/"
                  + "offspringPopulationSize/maxEvaluations/numberOfCores/mutationProbabilityFactor"
                  + " are configurable, everything else (crossover/mutation/ranking/selection) is"
                  + " hardcoded by the builder itself"),
          Map.entry(
              "MetaSMPSOBuilder",
              "wired into TrainingRunner.runFlatPso() via MetaAlgorithmRegistry, as \"SMPSO\";"
                  + " flat-only (requires a DoubleProblem, incompatible with tree encoding's"
                  + " DerivationTreeSolution), and exposes no operator catalogue at all, only"
                  + " swarm size/evaluations/cores"),
          Map.entry(
              "MetaAsyncNSGAIIBuilder",
              "wired into TrainingRunner.runFlatAsync() via MetaAlgorithmRegistry, as"
                  + " \"AsyncNSGA-II\"; its crossover/mutation operators come from a request-"
                  + "supplied parameter space/flags instead of this builder's hardcoded"
                  + " SBX+polynomial defaults"),
          Map.entry("MetaAsyncGeneticAlgorithmBuilder", "not yet wired into TrainingRunner"),
          Map.entry(
              "MetaRandomSearchBuilder",
              "not yet wired into TrainingRunner; generic over the solution type, so usable with"
                  + " either encoding once wired"));

  @Test
  @DisplayName(
      "given the meta-builder source tree, when scanning for Meta*Builder classes, then every one"
          + " is accounted for in KNOWN_META_BUILDER_CLASSES")
  void givenMetaBuilderSourceTree_whenScanning_thenEveryClassIsAccountedFor() throws IOException {
    // Arrange & Act
    List<String> untriaged;
    try (Stream<Path> files = Files.list(META_BUILDER_SOURCE_ROOT)) {
      untriaged =
          files
              .map(path -> path.getFileName().toString())
              .filter(name -> name.matches("Meta.*Builder\\.java"))
              .map(name -> name.replace(".java", ""))
              .filter(name -> !KNOWN_META_BUILDER_CLASSES.containsKey(name))
              .sorted()
              .toList();
    }

    // Assert
    assertTrue(
        untriaged.isEmpty(),
        "New meta-builder class(es) not yet triaged: "
            + untriaged
            + ". Either wire them into TrainingRunner, or add them to"
            + " KNOWN_META_BUILDER_CLASSES in this test with a reason.");
  }
}
