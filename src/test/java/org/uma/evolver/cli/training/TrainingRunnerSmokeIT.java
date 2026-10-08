package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        SMOKE_NUMBER_OF_CORES,
        metaSearch.operatorFlags());
  }

  private static void assertRunFinished(TrainingRequest request, Path tempDir) throws IOException {
    Path statusFile = tempDir.resolve("status.yaml");

    Path outputDirectory =
        assertDoesNotThrow(() -> new TrainingRunner(false).run(request, statusFile));

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
  @DisplayName("Given Spread as an indicator and a training set with three objectives")
  class SpreadOnThreeObjectives {

    @Test
    @DisplayName("when run, then it is rejected before the meta-optimizer starts")
    void whenRun_thenItIsRejectedBeforeStarting(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig dtlz = smokeBaseLevel("DTLZ3DNSGAIIBaseLevel.yaml");
      BaseLevelConfig baseLevel =
          new BaseLevelConfig(
              dtlz.algorithmName(),
              dtlz.encoding(),
              dtlz.populationSize(),
              dtlz.numberOfIndependentRuns(),
              dtlz.yamlParameterSpaceFile(),
              dtlz.extraConfig(),
              dtlz.trainingProblemNames(),
              dtlz.trainingReferenceFrontFileNames(),
              dtlz.trainingEvaluations(),
              List.of("Epsilon", "Spread"));
      TrainingRequest request =
          new TrainingRequest(
              baseLevel,
              smokeFlatMetaSearch("MetaNSGAIIFlatConfiguration.yaml"),
              tempDir.resolve("output").toString(),
              5,
              5,
              null);
      Path statusFile = tempDir.resolve("status.yaml");

      // Act
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> new TrainingRunner(false).run(request, statusFile));

      // Assert
      assertTrue(exception.getMessage().contains("bi-objective"));
      assertTrue(Files.readString(statusFile).contains("FAILED"));
    }
  }

  @Nested
  @DisplayName("Given NSGA-II (flat encoding) without metaPopulationSize")
  class DefaultPopulationNsgaIIFlat {

    @Test
    @DisplayName(
        "when run, then it finishes and records the default meta population size in METADATA.txt")
    void whenRun_thenItRecordsTheDefaultPopulationSize(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig configured = smokeFlatMetaSearch("MetaNSGAIIFlatConfiguration.yaml");
      FlatMetaSearchConfig metaSearch =
          new FlatMetaSearchConfig(
              configured.algorithm(),
              SMOKE_META_MAX_EVALUATIONS,
              null,
              SMOKE_NUMBER_OF_CORES,
              configured.operatorFlags());
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act
      assertRunFinished(request, tempDir);

      // Assert
      String metadata = Files.readString(tempDir.resolve("output").resolve("METADATA.txt"));
      assertTrue(
          metadata.contains(
              "Population Size: " + MetaAlgorithmRegistry.DEFAULT_POPULATION_SIZE),
          metadata);
    }
  }

  @Nested
  @DisplayName("Given AGE-MOEA (flat encoding)")
  class AgemoeaFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      FlatMetaSearchConfig metaSearch =
          smokeFlatMetaSearch("MetaAGEMOEAFlatConfiguration.yaml");
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
  @DisplayName("Given NSGA-II (Binary base-level encoding)")
  class BinaryNsgaIIFlat {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("TutorialZdt5BinaryBaseLevel.yaml");
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
  @DisplayName("Given a training set whose problems do not have the encoding of the algorithm")
  class EncodingMismatch {

    @Test
    @DisplayName("when run, then it fails before running and says which encodings do not match")
    void whenRun_thenItFailsBeforeRunning(@TempDir Path tempDir) throws IOException {
      // Arrange: ZDT4, a Double problem, for a Permutation algorithm
      BaseLevelConfig doubles = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      BaseLevelConfig baseLevel =
          new BaseLevelConfig(
              doubles.algorithmName(),
              "Permutation",
              doubles.populationSize(),
              doubles.numberOfIndependentRuns(),
              "NSGAIIPermutation.yaml",
              doubles.extraConfig(),
              doubles.trainingProblemNames(),
              doubles.trainingReferenceFrontFileNames(),
              doubles.trainingEvaluations(),
              doubles.indicatorNames());
      FlatMetaSearchConfig metaSearch =
          smokeFlatMetaSearch("MetaRandomSearchFlatConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);
      Path statusFile = tempDir.resolve("status.yaml");

      // Act & Assert
      assertThrows(RuntimeException.class, () -> new TrainingRunner(false).run(request, statusFile));
      String status = Files.readString(statusFile);
      assertTrue(status.contains("FAILED"), status);
      assertTrue(status.contains("Double-encoded"), status);
    }
  }

  @Nested
  @DisplayName("Given AGE-MOEA (tree encoding)")
  class AgemoeaTree {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      TreeMetaSearchConfig metaSearch =
          smokeTreeMetaSearch("MetaAGEMOEATreeConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given SPEA2 (tree encoding)")
  class Spea2Tree {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      TreeMetaSearchConfig metaSearch =
          smokeTreeMetaSearch("MetaSPEA2TreeConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  @Nested
  @DisplayName("Given RandomSearch (tree encoding)")
  class RandomSearchTree {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      TreeMetaSearchConfig metaSearch =
          smokeTreeMetaSearch("MetaRandomSearchTreeConfiguration.yaml");
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

  @Nested
  @DisplayName("Given AsyncNSGA-II (tree encoding)")
  class AsyncNsgaIITree {

    @Test
    @DisplayName("when run, then it finishes and writes output files")
    void whenRun_thenItFinishesAndWritesOutputFiles(@TempDir Path tempDir) throws IOException {
      // Arrange
      BaseLevelConfig baseLevel = smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml");
      TreeMetaSearchConfig metaSearch =
          smokeTreeMetaSearch("MetaAsyncNSGAIITreeConfiguration.yaml");
      TrainingRequest request =
          new TrainingRequest(
              baseLevel, metaSearch, tempDir.resolve("output").toString(), 5, 5, null);

      // Act & Assert
      assertRunFinished(request, tempDir);
    }
  }

  /**
   * The checkpoints of a training are the evaluations that are multiples of the write frequency,
   * and the final front is written once more at the end of the run. When the last meta-evaluation
   * falls on a checkpoint, VAR_CONF.txt must not repeat it.
   */
  @Nested
  @DisplayName("Given a limit that is a multiple of the write frequency")
  class FinalCheckpoint {

    private static final int WRITE_FREQUENCY = 5;
    private static final int LIMIT = 10;

    private List<String> checkpointsOf(TrainingRequest request, Path tempDir) throws IOException {
      Path outputDirectory = new TrainingRunner(false).run(request, tempDir.resolve("status.yaml"));
      return Files.readAllLines(outputDirectory.resolve("VAR_CONF.txt")).stream()
          .filter(line -> line.startsWith("# Evaluation: "))
          .toList();
    }

    @Test
    @DisplayName("when NSGA-II (flat encoding) is run, then each checkpoint is written once")
    void whenFlatRun_thenEachCheckpointIsWrittenOnce(@TempDir Path tempDir) throws IOException {
      // Arrange
      FlatMetaSearchConfig configured =
          (FlatMetaSearchConfig)
              MetaOptimizerConfigurationReader.load("MetaNSGAIIFlatConfiguration.yaml");
      var metaSearch =
          new FlatMetaSearchConfig(
              configured.algorithm(), LIMIT, WRITE_FREQUENCY, SMOKE_NUMBER_OF_CORES, configured.operatorFlags());
      var request =
          new TrainingRequest(
              smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml"),
              metaSearch,
              tempDir.resolve("output").toString(),
              WRITE_FREQUENCY,
              WRITE_FREQUENCY,
              null);

      // Act
      List<String> checkpoints = checkpointsOf(request, tempDir);

      // Assert
      assertEquals(checkpoints.stream().distinct().toList(), checkpoints);
      assertEquals("# Evaluation: " + LIMIT, checkpoints.get(checkpoints.size() - 1));
    }

    @Test
    @DisplayName("when NSGA-II (tree encoding) is run, then each checkpoint is written once")
    void whenTreeRun_thenEachCheckpointIsWrittenOnce(@TempDir Path tempDir) throws IOException {
      // Arrange
      TreeMetaSearchConfig configured =
          (TreeMetaSearchConfig)
              MetaOptimizerConfigurationReader.load("MetaNSGAIITreeConfiguration.yaml");
      var metaSearch =
          new TreeMetaSearchConfig(
              configured.algorithm(), LIMIT, WRITE_FREQUENCY, SMOKE_NUMBER_OF_CORES, configured.operatorFlags());
      var request =
          new TrainingRequest(
              smokeBaseLevel("Zdt4NSGAIIBaseLevel.yaml"),
              metaSearch,
              tempDir.resolve("output").toString(),
              WRITE_FREQUENCY,
              WRITE_FREQUENCY,
              null);

      // Act
      List<String> checkpoints = checkpointsOf(request, tempDir);

      // Assert
      assertEquals(checkpoints.stream().distinct().toList(), checkpoints);
      assertEquals("# Evaluation: " + LIMIT, checkpoints.get(checkpoints.size() - 1));
    }
  }
}
