package org.uma.evolver.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.cli.BaseAlgorithmRegistry.BaseAlgorithmDescriptor;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.FlatMetaSearchConfig;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;
import org.yaml.snakeyaml.Yaml;

/**
 * Runs a minimal training for every algorithm and encoding that {@link BaseAlgorithmRegistry}
 * registers: it loads its parameter space, builds it with the configurations the meta-optimizer
 * samples, runs it on a problem of its encoding and computes the indicators.
 *
 * <p>The meta-optimizer samples its configurations at random, so a combination of parameters that
 * an algorithm cannot run (a component that does not exist for the encoding, or one that fails
 * with another) is found here sooner or later: a longer sweep, with 200 configurations of each
 * algorithm, found the ones that the parameter spaces no longer offer.
 */
@Tag("integration")
@DisplayName("Smoke tests of every registered base-level algorithm")
class BaseAlgorithmRegistrySmokeIT {

  private static final int BASE_POPULATION_SIZE = 100;
  private static final int BASE_EVALUATIONS = 300;
  private static final int META_MAX_EVALUATIONS = 10;
  private static final int META_POPULATION_SIZE = 4;
  private static final int NUMBER_OF_CORES = 2;

  static Stream<BaseAlgorithmDescriptor> algorithms() {
    return BaseAlgorithmRegistry.registeredAlgorithms().stream();
  }

  /** The problem, its reference front and the indicators used for an encoding. */
  private record TrainingProblem(String name, String referenceFront, List<String> indicators) {}

  private static TrainingProblem problemOf(String encoding) {
    return switch (encoding) {
      case "Binary" ->
          new TrainingProblem(
              "ZDT5",
              "resources/referenceFronts/ZDT5.csv",
              List.of("Epsilon", "NormalizedHypervolume"));
      case "Permutation" ->
          new TrainingProblem(
              "KroAB100TSP",
              "resources/referenceFrontsTSP/KroAB100TSP.csv",
              List.of("HypervolumeMinus", "Epsilon"));
      default ->
          new TrainingProblem(
              "ZDT1",
              "resources/referenceFronts/ZDT1.csv",
              List.of("Epsilon", "NormalizedHypervolume"));
    };
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("algorithms")
  @DisplayName(
      "given a registered algorithm and encoding, when trained on a problem of that encoding, then"
          + " it finishes")
  void givenARegisteredAlgorithm_whenTrained_thenItFinishes(
      BaseAlgorithmDescriptor algorithm, @TempDir Path tempDir) throws IOException {
    // Arrange: the parameter spaces are named after the algorithm and the encoding
    String parameterSpaceFile = algorithm.name().replace("-", "") + algorithm.encoding() + ".yaml";
    TrainingProblem problem = problemOf(algorithm.encoding());
    Map<String, String> extraConfig =
        algorithm.requiredExtraConfigKeys().isEmpty()
            ? Map.of()
            : Map.of("weightVectorFilesDirectory", "resources/weightVectors");
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            algorithm.name(),
            algorithm.encoding(),
            BASE_POPULATION_SIZE,
            1,
            parameterSpaceFile,
            extraConfig,
            List.of(new ProblemSpec(problem.name())),
            List.of(problem.referenceFront()),
            List.of(BASE_EVALUATIONS),
            problem.indicators());
    FlatMetaSearchConfig bundled =
        (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load("MetaNSGAIIFlatConfiguration.yaml");
    FlatMetaSearchConfig metaSearch =
        new FlatMetaSearchConfig(
            bundled.algorithm(),
            META_MAX_EVALUATIONS,
            META_POPULATION_SIZE,
            NUMBER_OF_CORES,
            bundled.operatorFlags());
    TrainingRequest request =
        new TrainingRequest(baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);
    Path statusFile = tempDir.resolve("status.yaml");

    // Act
    assertDoesNotThrow(() -> new TrainingRunner(false).run(request, statusFile));

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> status =
        (Map<String, Object>) new Yaml().load(Files.newBufferedReader(statusFile));
    assertEquals("FINISHED", status.get("status"), "status.yaml: " + status);
  }
}
