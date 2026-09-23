package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

/**
 * Smoke tests for {@link TrainingRunner}: one per registered flat/tree meta-optimizer engine,
 * each running a real (tiny-budget) training job end-to-end and asserting it reaches {@code
 * FINISHED} and writes its output files — not correctness of the tuning result, just that the
 * whole pipeline (base-level algorithm, meta-optimizer engine, observers, output files, status
 * file) wires together and runs without exploding for each engine {@link MetaAlgorithmRegistry}
 * knows about.
 *
 * <p>Base-level/meta-search recipes are the real bundled files under {@code
 * src/main/resources/{baseLevelConfigurations,metaOptimizerConfigurations}/}, copied into a new
 * config with the evaluation/population budgets cut down for speed — so a change to a bundled
 * recipe's operator flags is exercised here too, instead of a hand-rolled duplicate that could
 * drift from it.
 *
 * <p>{@code AsyncNSGA-II} (the {@link MetaAlgorithmRegistry.Family#ASYNCHRONOUS} family) is
 * deliberately not covered here: {@code AsynchronousMultiThreadedGeneticAlgorithm}'s worker
 * threads are plain, non-daemon {@code Thread}s running an unconditional {@code while (true)}
 * loop (see {@code org.uma.jmetal.parallel.asynchronous.multithreaded.Worker}) that never
 * terminate on their own — the same reason {@code TrainingRunnerMain} and the async {@code
 * org.uma.evolver.example.training} examples must call {@code System.exit(0)} after {@code
 * run()}. Running it in-process here would leak those threads into the shared test JVM and hang
 * the build. The other three families (evolutionary, particle-swarm, tree) all evaluate through
 * {@code MultiThreadedEvaluation}, which uses the common {@code ForkJoinPool}'s daemon threads, so
 * they are safe to run in-process.
 */
@DisplayName("TrainingRunner smoke tests")
@Tag("integration")
class TrainingRunnerSmokeIT {

  private static final int SMOKE_BASE_POPULATION_SIZE = 10;
  private static final int SMOKE_BASE_EVALUATIONS = 300;
  private static final int SMOKE_META_MAX_EVALUATIONS = 10;
  private static final int SMOKE_META_POPULATION_SIZE = 4;
  private static final int SMOKE_NUMBER_OF_CORES = 2;

  private static BaseLevelConfig smokeBaseLevel(String fileName) {
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.load(fileName);
    return new BaseLevelConfig(
        baseLevel.algorithmName(),
        baseLevel.encoding(),
        SMOKE_BASE_POPULATION_SIZE,
        baseLevel.numberOfIndependentRuns(),
        baseLevel.yamlParameterSpaceFile(),
        baseLevel.extraConfig(),
        baseLevel.trainingProblemNames(),
        baseLevel.trainingReferenceFrontFileNames(),
        baseLevel.trainingProblemNames().stream().map(name -> SMOKE_BASE_EVALUATIONS).toList(),
        baseLevel.indicatorNames());
  }

  private static FlatMetaSearchConfig smokeFlatMetaSearch(String fileName) {
    FlatMetaSearchConfig metaSearch =
        (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load(fileName);
    return new FlatMetaSearchConfig(
        metaSearch.algorithm(),
        SMOKE_META_MAX_EVALUATIONS,
        SMOKE_META_POPULATION_SIZE,
        SMOKE_NUMBER_OF_CORES,
        metaSearch.operatorFlags());
  }

  private static TreeMetaSearchConfig smokeTreeMetaSearch(String fileName) {
    TreeMetaSearchConfig metaSearch =
        (TreeMetaSearchConfig) MetaOptimizerConfigurationReader.load(fileName);
    return new TreeMetaSearchConfig(
        metaSearch.algorithm(),
        SMOKE_META_MAX_EVALUATIONS,
        SMOKE_META_POPULATION_SIZE,
        SMOKE_META_POPULATION_SIZE,
        SMOKE_NUMBER_OF_CORES,
        metaSearch.crossoverProbability(),
        metaSearch.mutationProbability(),
        metaSearch.mutationDistributionIndex());
  }

  private static void assertRunFinished(TrainingRequest request, Path tempDir) throws IOException {
    Path statusFile = tempDir.resolve("status.yaml");

    Path outputDirectory =
        assertDoesNotThrow(() -> new TrainingRunner().run(request, statusFile));

    assertTrue(Files.exists(outputDirectory.resolve("METADATA.txt")));
    assertTrue(Files.exists(outputDirectory.resolve("INDICATORS.csv")));
    assertTrue(Files.exists(outputDirectory.resolve("CONFIGURATIONS.csv")));

    @SuppressWarnings("unchecked")
    Map<String, Object> status =
        (Map<String, Object>) new Yaml().load(Files.newBufferedReader(statusFile));
    assertTrue("FINISHED".equals(status.get("status")), "status.yaml: " + status);
  }

  @Nested
  @DisplayName("Given NSGA-II (flat encoding)")
  class ParallelNsgaIIFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch =
          smokeFlatMetaSearch("MetaNSGAIIFlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given SPEA2 (flat encoding)")
  class Spea2Flat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch = smokeFlatMetaSearch("MetaSPEA2FlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given SMPSO (flat encoding)")
  class SmpsoFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch = smokeFlatMetaSearch("MetaSMPSOFlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given RandomSearch (flat encoding)")
  class RandomSearchFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch =
          smokeFlatMetaSearch("MetaRandomSearchFlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given NSGA-II (Permutation base-level encoding)")
  class PermutationNsgaIIFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("TwoBiObjectiveTSPPermutationBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch =
          smokeFlatMetaSearch("MetaNSGAIIFlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given NSGA-II (tree encoding)")
  class ParallelNsgaIITree {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      TreeMetaSearchConfig metaSearch =
          smokeTreeMetaSearch("MetaNSGAIITreeConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }
}
