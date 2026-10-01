package org.uma.evolver.meta.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.meta.strategy.RandomRangeEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;

@DisplayName("Stopping condition in the metadata of a meta-optimization")
class MetaOptimizerConfigTest {

  @TempDir private Path tempDir;

  private String metadata(MetaOptimizerConfig config, int evaluations, long wallClockMillis)
      throws IOException {
    List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    var problem =
        new MetaOptimizationProblem<>(
            new DoubleNSGAII(
                100, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory())),
            List.of(new ZDT1()),
            List.of("resources/referenceFronts/ZDT1.csv"),
            indicators,
            new FixedEvaluationsStrategy(List.of(1000)),
            1);
    var outputResults =
        new ConsolidatedOutputResults(problem, "ZDT1", indicators, tempDir.toString(), config);
    outputResults.updateEvaluations(evaluations);
    outputResults.writeWallClockTime(wallClockMillis);
    return Files.readString(tempDir.resolve("METADATA.txt"));
  }

  @Test
  @DisplayName("Given a limit on evaluations, when writing the metadata, then it states the limit and the condition")
  void givenEvaluationLimitWhenWritingMetadataThenItStatesLimitAndCondition() throws IOException {
    // Arrange
    var config = MetaOptimizerConfig.builder().metaMaxEvaluations(2000).build();

    // Act
    String text = metadata(config, 2000, 61_000);

    // Assert
    assertTrue(text.contains("Max Evaluations: 2000"));
    assertTrue(text.contains("Stopping condition: evaluations"));
    assertFalse(text.contains("Max Computing Time"));
    assertTrue(text.contains("Wall-clock time: 0h 1m 1s (61000 ms)"));
    assertTrue(text.contains("Meta-evaluations performed: 2000"));
  }

  @Test
  @DisplayName("Given a limit on time, when writing the metadata, then it states the minutes, the condition and the evaluations reached")
  void givenTimeLimitWhenWritingMetadataThenItStatesMinutesConditionAndEvaluationsReached()
      throws IOException {
    // Arrange
    var config = MetaOptimizerConfig.builder().metaMaxComputingTimeMinutes(90).build();

    // Act
    String text = metadata(config, 1340, 5_461_000);

    // Assert
    assertTrue(text.contains("Max Computing Time: 90 min (1h 30m 0s)"));
    assertTrue(text.contains("Stopping condition: computing time"));
    assertFalse(text.contains("Stopping condition: evaluations"));
    assertTrue(text.contains("Wall-clock time: 1h 31m 1s (5461000 ms)"));
    assertTrue(text.contains("Meta-evaluations performed: 1340"));
  }

  @Test
  @DisplayName("Given minutes with decimals, when describing the limit, then the decimals are kept")
  void givenMinutesWithDecimalsWhenDescribingLimitThenDecimalsAreKept() {
    // Arrange
    var config = MetaOptimizerConfig.builder().metaMaxComputingTimeMinutes(0.5).build();

    // Act
    List<String> lines = config.stoppingConditionLines();

    // Assert
    assertEquals("Max Computing Time: 0.5 min (0h 0m 30s)", lines.get(0));
  }

  @Test
  @DisplayName("Given both limits, when building the configuration, then it is rejected")
  void givenBothLimitsWhenBuildingConfigurationThenItIsRejected() {
    // Arrange
    var builder =
        MetaOptimizerConfig.builder().metaMaxEvaluations(2000).metaMaxComputingTimeMinutes(10);

    // Act / Assert
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  @DisplayName("Given the evaluation strategies, when printing them, then they describe their budget")
  void givenEvaluationStrategiesWhenPrintingThemThenTheyDescribeTheirBudget() {
    // Arrange / Act / Assert
    assertEquals("Fixed evaluations [12000]", new FixedEvaluationsStrategy(List.of(12000)).toString());
    assertEquals(
        "Random evaluations in [1000, 5000]", new RandomRangeEvaluationsStrategy(1000, 5000).toString());
  }
}
