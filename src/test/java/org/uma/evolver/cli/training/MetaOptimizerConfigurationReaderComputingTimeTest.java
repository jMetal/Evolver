package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Meta-optimizer configurations bounded by computing time")
class MetaOptimizerConfigurationReaderComputingTimeTest {

  private static String yaml(String encoding, String limits) {
    return "algorithm: NSGA-II\nencoding: " + encoding + "\n" + limits
        + "metaPopulationSize: 20\nnumberOfCores: 2\ncrossover: SBX\n";
  }

  @Test
  @DisplayName("Given minutes with decimals, when loaded, then the flat configuration has no evaluation limit")
  void givenMinutesWithDecimalsWhenLoadedThenFlatConfigurationHasNoEvaluationLimit() {
    // Arrange
    String text = yaml("flat", "metaMaxComputingTimeMinutes: 7.5\n");

    // Act
    var config = (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.loadFromYaml(text);

    // Assert
    assertEquals(7.5, config.metaMaxComputingTimeMinutes());
    assertEquals(0, config.metaMaxEvaluations());
    assertTrue(config.boundedByComputingTime());
    assertFalse(config.operatorFlags().contains("--metaMaxComputingTimeMinutes"));
  }

  @Test
  @DisplayName("Given whole minutes, when loaded, then the tree configuration is bounded by time")
  void givenWholeMinutesWhenLoadedThenTreeConfigurationIsBoundedByTime() {
    // Arrange
    String text = yaml("tree", "metaMaxComputingTimeMinutes: 90\n");

    // Act
    var config = (TreeMetaSearchConfig) MetaOptimizerConfigurationReader.loadFromYaml(text);

    // Assert
    assertEquals(90.0, config.metaMaxComputingTimeMinutes());
    assertTrue(config.boundedByComputingTime());
  }

  @Test
  @DisplayName("Given the bundled computing-time configuration, when loaded, then it is bounded by 60 minutes")
  void givenBundledComputingTimeConfigurationWhenLoadedThenItIsBoundedBySixtyMinutes() {
    // Arrange
    String file = "MetaNSGAIIFlatComputingTimeConfiguration.yaml";

    // Act
    var config = (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.load(file);

    // Assert
    assertEquals(60.0, config.metaMaxComputingTimeMinutes());
    assertEquals(0, config.metaMaxEvaluations());
    assertTrue(config.boundedByComputingTime());
  }

  @Test
  @DisplayName("Given the bundled computing-time request, when loaded, then its meta-search is bounded by time")
  void givenBundledComputingTimeRequestWhenLoadedThenItsMetaSearchIsBoundedByTime()
      throws IOException {
    // Arrange
    Path requestFile =
        Path.of("src/main/resources/cli/training/nsgaii-re3d-computing-time-request.yaml");

    // Act
    TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);

    // Assert
    assertTrue(((FlatMetaSearchConfig) request.metaSearch()).boundedByComputingTime());
    assertEquals("results/nsgaii/RE3D-computing-time", request.outputDirectory());
  }

  @Test
  @DisplayName("Given evaluations only, when loaded, then it is bounded by evaluations")
  void givenEvaluationsOnlyWhenLoadedThenItIsBoundedByEvaluations() {
    // Arrange
    String text = yaml("flat", "metaMaxEvaluations: 500\n");

    // Act
    var config = (FlatMetaSearchConfig) MetaOptimizerConfigurationReader.loadFromYaml(text);

    // Assert
    assertEquals(500, config.metaMaxEvaluations());
    assertEquals(0.0, config.metaMaxComputingTimeMinutes());
    assertFalse(config.boundedByComputingTime());
  }

  @Test
  @DisplayName("Given both limits, when loaded, then they are rejected as mutually exclusive")
  void givenBothLimitsWhenLoadedThenTheyAreRejected() {
    // Arrange
    String text = yaml("flat", "metaMaxEvaluations: 500\nmetaMaxComputingTimeMinutes: 5\n");

    // Act
    var exception =
        assertThrows(JMetalException.class, () -> MetaOptimizerConfigurationReader.loadFromYaml(text));

    // Assert
    assertTrue(exception.getMessage().contains("mutually exclusive"), exception.getMessage());
  }

  @Test
  @DisplayName("Given no limit, when loaded, then it is rejected")
  void givenNoLimitWhenLoadedThenItIsRejected() {
    // Arrange
    String text = yaml("flat", "");

    // Act / Assert
    assertThrows(JMetalException.class, () -> MetaOptimizerConfigurationReader.loadFromYaml(text));
  }

  @Test
  @DisplayName("Given a zero or non-numeric time, when loaded, then it is rejected")
  void givenZeroOrNonNumericTimeWhenLoadedThenItIsRejected() {
    // Arrange / Act / Assert
    assertThrows(
        JMetalException.class,
        () ->
            MetaOptimizerConfigurationReader.loadFromYaml(
                yaml("flat", "metaMaxComputingTimeMinutes: 0\n")));
    assertThrows(
        JMetalException.class,
        () ->
            MetaOptimizerConfigurationReader.loadFromYaml(
                yaml("flat", "metaMaxComputingTimeMinutes: soon\n")));
  }
}
