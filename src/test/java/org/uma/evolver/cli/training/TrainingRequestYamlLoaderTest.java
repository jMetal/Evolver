package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class TrainingRequestYamlLoader")
class TrainingRequestYamlLoaderTest {

  @TempDir private Path tempDir;

  private static final String BASE_LEVEL_LINE = "baseLevel: Zdt4NSGAIIBaseLevel.yaml\n";
  private static final String OUTPUT_DIRECTORY_LINE = "outputDirectory: results/nsgaii/ZDT4\n";

  private Path writeRequestFile(String metaSearchLine) throws IOException {
    Path requestFile = tempDir.resolve("request.yaml");
    Files.writeString(requestFile, BASE_LEVEL_LINE + metaSearchLine + OUTPUT_DIRECTORY_LINE);
    return requestFile;
  }

  @Nested
  @DisplayName("When loading a request whose baseLevel/metaSearch reference existing files: ")
  class ValidRequestTestCases {

    @Test
    @DisplayName(
        "given a request referencing Zdt4NSGAIIBaseLevel.yaml and"
            + " MetaNSGAIIFlatConfiguration.yaml, when loaded, then baseLevel, metaSearch"
            + " and outputDirectory are all built from those files")
    void givenValidFlatRequest_whenLoaded_thenRequestIsBuilt() throws IOException {
      // Arrange
      Path requestFile = writeRequestFile("metaSearch: MetaNSGAIIFlatConfiguration.yaml\n");

      // Act
      TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals("NSGA-II", request.baseLevel().algorithmName());
      assertEquals("results/nsgaii/ZDT4", request.outputDirectory());
      assertEquals(100, request.writeFrequency());
      assertEquals(100, request.statusFrequency());
      assertNull(request.frontPlotFrequency());
      FlatMetaSearchConfig metaSearch = (FlatMetaSearchConfig) request.metaSearch();
      assertEquals("NSGA-II", metaSearch.algorithm());
      assertEquals(2000, metaSearch.metaMaxEvaluations());
      assertEquals(50, metaSearch.metaPopulationSize());
      assertEquals(8, metaSearch.numberOfCores());
      assertTrue(metaSearch.operatorFlags().contains("--crossover"));
    }

    @Test
    @DisplayName(
        "given a request referencing MetaNSGAIITreeConfiguration.yaml, when loaded, then"
            + " a TreeMetaSearchConfig is built from that file")
    void givenValidTreeRequest_whenLoaded_thenTreeConfigIsBuilt() throws IOException {
      // Arrange
      Path requestFile = writeRequestFile("metaSearch: MetaNSGAIITreeConfiguration.yaml\n");

      // Act
      TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);

      // Assert
      TreeMetaSearchConfig metaSearch = (TreeMetaSearchConfig) request.metaSearch();
      assertEquals("NSGA-II", metaSearch.algorithm());
      assertEquals(50, metaSearch.metaPopulationSize());
    }

    @Test
    @DisplayName(
        "given a request with explicit writeFrequency/statusFrequency, when loaded, then those"
            + " override the default")
    void givenRequestWithExplicitFrequencies_whenLoaded_thenTheyOverrideTheDefault()
        throws IOException {
      // Arrange
      Path requestFile = tempDir.resolve("request.yaml");
      Files.writeString(
          requestFile,
          BASE_LEVEL_LINE
              + "metaSearch: MetaNSGAIIFlatConfiguration.yaml\n"
              + OUTPUT_DIRECTORY_LINE
              + "writeFrequency: 1\n"
              + "statusFrequency: 50\n");

      // Act
      TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals(1, request.writeFrequency());
      assertEquals(50, request.statusFrequency());
    }

    @Test
    @DisplayName(
        "given a request with an explicit frontPlotFrequency, when loaded, then it is carried"
            + " over instead of the default absent value")
    void givenRequestWithFrontPlotFrequency_whenLoaded_thenItIsCarriedOver() throws IOException {
      // Arrange
      Path requestFile = tempDir.resolve("request.yaml");
      Files.writeString(
          requestFile,
          BASE_LEVEL_LINE
              + "metaSearch: MetaNSGAIIFlatConfiguration.yaml\n"
              + OUTPUT_DIRECTORY_LINE
              + "frontPlotFrequency: 20\n");

      // Act
      TrainingRequest request = TrainingRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals(20, request.frontPlotFrequency());
    }
  }

  @Nested
  @DisplayName("When loading a request with a missing or invalid field: ")
  class InvalidRequestTestCases {

    @Test
    @DisplayName(
        "given a request without a metaSearch field, when loaded, then it fails with a clear"
            + " message")
    void givenRequestWithoutMetaSearch_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile = writeRequestFile("");

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> TrainingRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("metaSearch"));
    }

    @Test
    @DisplayName(
        "given a request referencing a non-existent base-level configuration file, when loaded,"
            + " then it fails with a clear message")
    void givenRequestWithUnknownBaseLevelFile_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile =
          tempDir.resolve("request.yaml");
      Files.writeString(
          requestFile,
          "baseLevel: DoesNotExist.yaml\n"
              + "metaSearch: MetaNSGAIIFlatConfiguration.yaml\n"
              + OUTPUT_DIRECTORY_LINE);

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> TrainingRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("DoesNotExist.yaml"));
    }

    @Test
    @DisplayName(
        "given a request referencing a non-existent meta-optimizer configuration file, when"
            + " loaded, then it fails with a clear message")
    void givenRequestWithUnknownMetaSearchFile_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile = writeRequestFile("metaSearch: DoesNotExist.yaml\n");

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> TrainingRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("DoesNotExist.yaml"));
    }
  }

  static Stream<Path> bundledRequestFiles() throws IOException {
    return Files.list(Path.of("src/main/resources/cli/training"))
        .filter(path -> path.toString().endsWith("-request.yaml"))
        .sorted();
  }

  @Nested
  @DisplayName("When loading the request files bundled with Evolver: ")
  class BundledRequestFilesTestCases {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "org.uma.evolver.cli.training.TrainingRequestYamlLoaderTest#bundledRequestFiles")
    @DisplayName(
        "given a bundled request file, when loaded, then its base-level and meta-search files are"
            + " found and parsed")
    void givenBundledRequestFile_whenLoaded_thenItIsParsed(Path requestFile) {
      // Arrange & Act & Assert
      assertDoesNotThrow(() -> TrainingRequestYamlLoader.load(requestFile));
    }
  }
}
