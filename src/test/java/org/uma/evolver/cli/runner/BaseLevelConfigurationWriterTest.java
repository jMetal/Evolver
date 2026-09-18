package org.uma.evolver.cli.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Unit tests for class BaseLevelConfigurationWriter")
class BaseLevelConfigurationWriterTest {

  @TempDir private Path tempDir;

  @Test
  @DisplayName(
      "given a BaseLevelConfig built in Java, when saved and reloaded via"
          + " BaseLevelConfigurationReader, then every field round-trips")
  void givenConfig_whenSavedAndReloaded_thenFieldsRoundTrip() throws IOException {
    // Arrange
    BaseLevelConfig original =
        new BaseLevelConfig(
            "NSGA-II",
            100,
            1,
            "NSGAIIDouble.yaml",
            Map.of("weightVectorFilesDirectory", "resources/weightVectors"),
            List.of("RE31", "RE32"),
            List.of("resources/referenceFronts/RE31.csv", "resources/referenceFronts/RE32.csv"),
            List.of(10000, 10000),
            List.of("Epsilon", "NormalizedHypervolume"));
    Path outputFile = tempDir.resolve("roundtrip.yaml");

    // Act
    BaseLevelConfigurationWriter.save(original, outputFile);
    BaseLevelConfig reloaded = BaseLevelConfigurationReader.load(outputFile.toString());

    // Assert
    assertEquals(original, reloaded);
  }

  @Test
  @DisplayName(
      "given a BaseLevelConfig with no extraConfig, when saved and reloaded, then extraConfig is"
          + " empty rather than absent")
  void givenConfigWithoutExtraConfig_whenSavedAndReloaded_thenExtraConfigIsEmpty()
      throws IOException {
    // Arrange
    BaseLevelConfig original =
        new BaseLevelConfig(
            "NSGA-II",
            100,
            1,
            "NSGAIIDouble.yaml",
            null,
            List.of("ZDT4"),
            List.of("resources/referenceFronts/ZDT4.csv"),
            List.of(12000),
            List.of("Epsilon"));
    Path outputFile = tempDir.resolve("roundtrip-no-extra.yaml");

    // Act
    BaseLevelConfigurationWriter.save(original, outputFile);
    BaseLevelConfig reloaded = BaseLevelConfigurationReader.load(outputFile.toString());

    // Assert
    assertEquals(Map.of(), reloaded.extraConfig());
  }
}
