package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class SolveRequestYamlLoader")
class SolveRequestYamlLoaderTest {

  @TempDir private Path tempDir;

  private static final String REQUIRED_FIELDS =
      """
      algorithmName: NSGA-II
      populationSize: 100
      yamlParameterSpaceFile: NSGAIIDouble.yaml
      problem: ZDT1
      maxEvaluations: 25000
      outputDirectory: results/solve/ZDT1
      """;

  private static final String INLINE_CONFIGURATION =
      "configuration: --algorithmResult population --crossover SBX\n";

  private Path writeRequestFile(String content) throws IOException {
    Path requestFile = tempDir.resolve("request.yaml");
    Files.writeString(requestFile, content);
    return requestFile;
  }

  @Nested
  @DisplayName("When loading a valid request: ")
  class ValidRequestTestCases {

    @Test
    @DisplayName(
        "given only the required fields and an inline configuration, when loaded, then the"
            + " optional fields take their defaults")
    void givenRequiredFieldsOnly_whenLoaded_thenOptionalFieldsTakeTheirDefaults()
        throws IOException {
      // Arrange
      Path requestFile = writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION);

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals("NSGA-II", request.algorithmName());
      assertEquals("Double", request.encoding());
      assertEquals(100, request.populationSize());
      assertEquals(new ProblemSpec("ZDT1"), request.problem());
      assertEquals("--algorithmResult population --crossover SBX", request.configuration());
      assertNull(request.configurationFile());
      assertNull(request.referenceFrontFileName());
      assertEquals(25000, request.maxEvaluations());
      assertEquals(1, request.numberOfIndependentRuns());
      assertNull(request.seed());
      assertTrue(request.indicatorNames().isEmpty());
      assertNull(request.statusFrequency());
      assertNull(request.frontFrequency());
      assertFalse(request.writePopulation());
      assertTrue(request.extraConfig().isEmpty());
    }

    @Test
    @DisplayName("given a frontFrequency, when loaded, then the request has it")
    void givenFrontFrequency_whenLoaded_thenItIsRead() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION + "frontFrequency: 250\n");

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals(250, request.frontFrequency());
    }

    @Test
    @DisplayName("given writePopulation with a frontFrequency, when loaded, then it is read")
    void givenWritePopulation_whenLoaded_thenItIsRead() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(
              REQUIRED_FIELDS
                  + INLINE_CONFIGURATION
                  + "frontFrequency: 250\nwritePopulation: true\n");

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertTrue(request.writePopulation());
    }

    @Test
    @DisplayName("given a statusFrequency, when loaded, then the request has it")
    void givenStatusFrequency_whenLoaded_thenItIsRead() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION + "statusFrequency: 500\n");

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals(500, request.statusFrequency());
    }

    @Test
    @DisplayName(
        "given a configurationFile, when loaded, then the configuration is the first one of that"
            + " file")
    void givenConfigurationFile_whenLoaded_thenConfigurationIsReadFromIt() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(
              REQUIRED_FIELDS
                  + "configurationFile: defaultConfigurations/NSGAIIDoubleDefault.txt\n");

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals("defaultConfigurations/NSGAIIDoubleDefault.txt", request.configurationFile());
      assertTrue(request.configuration().startsWith("--algorithmResult population"));
    }

    @Test
    @DisplayName(
        "given a problem with constructor arguments, a seed, runs and indicators, when loaded,"
            + " then they are all carried over")
    void givenAllOptionalFields_whenLoaded_thenTheyAreCarriedOver() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(
              REQUIRED_FIELDS.replace(
                      "problem: ZDT1",
                      "problem: {class: org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1,"
                          + " args: [7, 3]}")
                  + INLINE_CONFIGURATION
                  + "referenceFrontFileName: resources/referenceFronts/DTLZ1.3D.csv\n"
                  + "numberOfIndependentRuns: 10\n"
                  + "seed: 42\n"
                  + "indicatorNames: [Epsilon, NormalizedHypervolume]\n");

      // Act
      SolveRequest request = SolveRequestYamlLoader.load(requestFile);

      // Assert
      assertEquals(
          new ProblemSpec("org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1", List.of(7, 3)),
          request.problem());
      assertEquals(10, request.numberOfIndependentRuns());
      assertEquals(42L, request.seed());
      assertEquals(List.of("Epsilon", "NormalizedHypervolume"), request.indicatorNames());
    }
  }

  @Nested
  @DisplayName("When loading an invalid request: ")
  class InvalidRequestTestCases {

    @Test
    @DisplayName("given neither configuration nor configurationFile, when loaded, then it fails")
    void givenNoConfiguration_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile = writeRequestFile(REQUIRED_FIELDS);

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("configurationFile"));
    }

    @Test
    @DisplayName("given both configuration and configurationFile, when loaded, then it fails")
    void givenBothConfigurations_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(
              REQUIRED_FIELDS
                  + INLINE_CONFIGURATION
                  + "configurationFile: defaultConfigurations/NSGAIIDoubleDefault.txt\n");

      // Act & Assert
      assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
    }

    @Test
    @DisplayName(
        "given indicators without a reference front, when loaded, then it fails with a clear"
            + " message")
    void givenIndicatorsWithoutReferenceFront_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION + "indicatorNames: [Epsilon]\n");

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("referenceFrontFileName"));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({
      "statusFrequency, 0",
      "statusFrequency, -5",
      "statusFrequency, often",
      "statusFrequency, 2.5",
      "frontFrequency, 0",
      "frontFrequency, often"
    })
    @DisplayName("given a frequency that is not a positive integer, when loaded, then it fails")
    void givenInvalidFrequency_whenLoaded_thenItFails(String field, String value)
        throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION + field + ": " + value + "\n");

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains(field));
    }

    @Test
    @DisplayName("given writePopulation without a frontFrequency, when loaded, then it fails")
    void givenWritePopulationWithoutFrontFrequency_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS + INLINE_CONFIGURATION + "writePopulation: true\n");

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("frontFrequency"));
    }

    @Test
    @DisplayName("given a request without a problem, when loaded, then it fails")
    void givenNoProblem_whenLoaded_thenItFails() throws IOException {
      // Arrange
      Path requestFile =
          writeRequestFile(REQUIRED_FIELDS.replace("problem: ZDT1\n", "") + INLINE_CONFIGURATION);

      // Act & Assert
      JMetalException exception =
          assertThrows(JMetalException.class, () -> SolveRequestYamlLoader.load(requestFile));
      assertTrue(exception.getMessage().contains("problem"));
    }
  }

  static Stream<Path> bundledRequestFiles() throws IOException {
    return Files.list(Path.of("src/main/resources/cli/solving"))
        .filter(path -> path.toString().endsWith("-request.yaml"))
        .sorted();
  }

  @Nested
  @DisplayName("When loading the request files bundled with Evolver: ")
  class BundledRequestFilesTestCases {

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.cli.solving.SolveRequestYamlLoaderTest#bundledRequestFiles")
    @DisplayName("given a bundled request file, when loaded, then it is parsed")
    void givenBundledRequestFile_whenLoaded_thenItIsParsed(Path requestFile) {
      // Arrange & Act & Assert
      assertDoesNotThrow(() -> SolveRequestYamlLoader.load(requestFile));
    }
  }
}
