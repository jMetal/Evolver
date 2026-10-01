package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

/**
 * Integration tests of training jobs whose meta-optimizer is bounded by computing time: real
 * (tiny-budget) runs of every registered meta-optimizer, checking the output
 * files, the status file and the stopping condition recorded in {@code METADATA.txt}.
 */
@DisplayName("TrainingRunner with the meta-optimizer bounded by computing time")
@Tag("integration")
class TrainingRunnerComputingTimeIT {

  private static final double LIMIT_MINUTES = 0.03; // 1.8 s
  private static final long LIMIT_MILLIS = 1800;
  private static final int BASE_POPULATION_SIZE = 10;
  private static final int BASE_EVALUATIONS = 300;
  private static final int META_POPULATION_SIZE = 4;
  private static final int NUMBER_OF_CORES = 2;

  private static BaseLevelConfig baseLevel() {
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.load("Zdt4NSGAIIBaseLevel.yaml");
    return new BaseLevelConfig(
        baseLevel.algorithmName(),
        baseLevel.encoding(),
        BASE_POPULATION_SIZE,
        baseLevel.numberOfIndependentRuns(),
        baseLevel.yamlParameterSpaceFile(),
        baseLevel.extraConfig(),
        baseLevel.trainingProblemNames(),
        baseLevel.trainingReferenceFrontFileNames(),
        baseLevel.trainingProblemNames().stream().map(name -> BASE_EVALUATIONS).toList(),
        baseLevel.indicatorNames());
  }

  private static FlatMetaSearchConfig flatBoundedByTime(String fileName) {
    var configured = (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load(fileName);
    return new FlatMetaSearchConfig(
        configured.algorithm(),
        0,
        LIMIT_MINUTES,
        META_POPULATION_SIZE,
        NUMBER_OF_CORES,
        configured.operatorFlags());
  }

  private static TreeMetaSearchConfig treeBoundedByTime(String fileName) {
    var configured = (TreeMetaSearchConfig) MetaOptimizerConfigurationReader.load(fileName);
    return new TreeMetaSearchConfig(
        configured.algorithm(),
        0,
        LIMIT_MINUTES,
        META_POPULATION_SIZE,
        NUMBER_OF_CORES,
        configured.operatorFlags());
  }

  /** Runs the request and checks the output, the status file and the metadata. */
  private static void assertBoundedByTime(MetaSearchConfig metaSearch, Path tempDir)
      throws IOException {
    // Arrange
    var request =
        new TrainingRequest(baseLevel(), metaSearch, tempDir.resolve("output").toString(), 5, 5, null);
    Path statusFile = tempDir.resolve("status.yaml");

    // Act
    long start = System.currentTimeMillis();
    Path outputDirectory = new TrainingRunner().run(request, statusFile);
    long elapsed = System.currentTimeMillis() - start;

    // Assert
    assertTrue(elapsed >= LIMIT_MILLIS, "stopped before the limit: " + elapsed + " ms");
    assertTrue(Files.exists(outputDirectory.resolve("INDICATORS.csv")));
    assertTrue(Files.exists(outputDirectory.resolve("CONFIGURATIONS.csv")));

    String metadata = Files.readString(outputDirectory.resolve("METADATA.txt"));
    assertTrue(metadata.contains("Max Computing Time: 0.03 min (0h 0m 1s)"), metadata);
    assertTrue(metadata.contains("Stopping condition: computing time"), metadata);
    assertTrue(metadata.contains("--- Execution ---"), metadata);
    Matcher performed = Pattern.compile("Meta-evaluations performed: (\\d+)").matcher(metadata);
    assertTrue(performed.find(), metadata);
    assertTrue(Integer.parseInt(performed.group(1)) > 0, metadata);

    assertCheckpointsHaveTime(outputDirectory);
    List<String> varConf = Files.readAllLines(outputDirectory.resolve("VAR_CONF.txt"));
    double lastMinutes =
        varConf.stream()
            .filter(line -> line.startsWith("# Time (min): "))
            .mapToDouble(line -> Double.parseDouble(line.substring("# Time (min): ".length())))
            .reduce((first, second) -> second)
            .orElseThrow();
    assertTrue(lastMinutes >= LIMIT_MINUTES, "last checkpoint time: " + lastMinutes);

    @SuppressWarnings("unchecked")
    Map<String, Object> status =
        (Map<String, Object>) new Yaml().load(Files.newBufferedReader(statusFile));
    assertEquals("FINISHED", status.get("status"), status.toString());
    assertEquals(0, status.get("maxEvaluations"), status.toString());
    assertEquals(LIMIT_MINUTES, ((Number) status.get("maxComputingTimeMinutes")).doubleValue());
    assertTrue(((Number) status.get("elapsedMinutes")).doubleValue() >= LIMIT_MINUTES - 0.01);
    assertEquals(Integer.parseInt(performed.group(1)), status.get("evaluationsDone"));
  }

  /** Every {@code # Evaluation} line of VAR_CONF.txt is followed by a {@code # Time (min)} line. */
  private static void assertCheckpointsHaveTime(Path outputDirectory) throws IOException {
    List<String> lines = Files.readAllLines(outputDirectory.resolve("VAR_CONF.txt"));
    int checkpoints = 0;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith("# Evaluation: ")) {
        checkpoints++;
        assertTrue(lines.get(i + 1).matches("# Time \\(min\\): \\d+\\.\\d{3}"), lines.get(i + 1));
      }
    }
    assertTrue(checkpoints > 0, "no checkpoint in VAR_CONF.txt");
  }

  @Test
  @DisplayName("Given a limit on evaluations, when run, then the checkpoints also record the time")
  void givenEvaluationLimitWhenRunThenCheckpointsAlsoRecordTime(@TempDir Path tempDir)
      throws IOException {
    // Arrange
    var configured =
        (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load("MetaNSGAIIFlatConfiguration.yaml");
    var metaSearch =
        new FlatMetaSearchConfig(
            configured.algorithm(), 12, META_POPULATION_SIZE, NUMBER_OF_CORES, configured.operatorFlags());
    var request =
        new TrainingRequest(baseLevel(), metaSearch, tempDir.resolve("output").toString(), 4, 4, null);

    // Act
    Path outputDirectory = new TrainingRunner().run(request, tempDir.resolve("status.yaml"));

    // Assert
    assertCheckpointsHaveTime(outputDirectory);
    String metadata = Files.readString(outputDirectory.resolve("METADATA.txt"));
    assertTrue(metadata.contains("Stopping condition: evaluations"), metadata);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "MetaNSGAIIFlatConfiguration.yaml",
        "MetaAGEMOEAFlatConfiguration.yaml",
        "MetaSPEA2FlatConfiguration.yaml",
        "MetaSMPSOFlatConfiguration.yaml",
        "MetaRandomSearchFlatConfiguration.yaml",
        "MetaAsyncNSGAIIFlatConfiguration.yaml"
      })
  @DisplayName("Given a flat meta-optimizer, when run with a time limit, then it finishes and records it")
  void givenFlatMetaOptimizerWhenRunWithTimeLimitThenItFinishesAndRecordsIt(
      String configurationFile, @TempDir Path tempDir) throws IOException {
    assertBoundedByTime(flatBoundedByTime(configurationFile), tempDir);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "MetaNSGAIITreeConfiguration.yaml",
        "MetaAGEMOEATreeConfiguration.yaml",
        "MetaRandomSearchTreeConfiguration.yaml"
      })
  @DisplayName("Given a tree meta-optimizer, when run with a time limit, then it finishes and records it")
  void givenTreeMetaOptimizerWhenRunWithTimeLimitThenItFinishesAndRecordsIt(
      String configurationFile, @TempDir Path tempDir) throws IOException {
    assertBoundedByTime(treeBoundedByTime(configurationFile), tempDir);
  }
}
