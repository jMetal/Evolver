package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.util.ConfigurationFileReader;

/**
 * Runs every algorithm that ships a default configuration ({@code defaultConfigurations/}) through
 * {@code cli.solving} with it, on a problem of its encoding: the configurations an app such as
 * Evolver-Studio offers as the starting point of a run must run.
 */
@Tag("integration")
@DisplayName("Default configurations through the solve runner")
class SolveRunnerDefaultConfigurationsIT {

  static Stream<Arguments> defaultConfigurations() {
    return Stream.of(
        Arguments.of("NSGA-II", "Double", "NSGAIIDoubleDefault.txt"),
        Arguments.of("NSGA-II", "Binary", "NSGAIIBinaryDefault.txt"),
        Arguments.of("NSGA-II", "Permutation", "NSGAIIPermutationDefault.txt"),
        Arguments.of("NSGA-III", "Double", "NSGAIIIDoubleDefault.txt"),
        Arguments.of("MOEAD", "Double", "MOEADDoubleDefault.txt"),
        Arguments.of("SMS-EMOA", "Double", "SMSEMOADoubleDefault.txt"),
        Arguments.of("RVEA", "Double", "RVEADoubleDefault.txt"),
        Arguments.of("AGE-MOEA", "Double", "AGEMOEADoubleDefault.txt"),
        Arguments.of("SSMOEA", "Double", "SSMOEADoubleDefault.txt"),
        Arguments.of("PAES", "Double", "PAESDoubleDefault.txt"),
        Arguments.of("PAES", "Binary", "PAESBinaryDefault.txt"),
        Arguments.of("PAES", "Permutation", "PAESPermutationDefault.txt"));
  }

  @ParameterizedTest(name = "{0} ({1})")
  @MethodSource("defaultConfigurations")
  @DisplayName("given a default configuration, when run, then it finishes and writes its fronts")
  void givenADefaultConfiguration_whenRun_thenItFinishes(
      String algorithm, String encoding, String file, @TempDir Path tempDir) throws IOException {
    // Arrange
    String problem = encoding.equals("Binary") ? "ZDT5" : encoding.equals("Permutation") ? "KroAB100TSP" : "ZDT1";
    String referenceFront =
        switch (encoding) {
          case "Binary" -> "resources/referenceFronts/ZDT5.csv";
          case "Permutation" -> "resources/referenceFrontsTSP/KroAB100TSP.csv";
          default -> "resources/referenceFronts/ZDT1.csv";
        };
    List<String> indicators =
        encoding.equals("Permutation")
            ? List.of("HypervolumeMinus")
            : List.of("Epsilon", "NormalizedHypervolume");
    boolean weightVectors = algorithm.equals("MOEAD") || algorithm.equals("RVEA");
    SolveRequest request =
        new SolveRequest(
            algorithm,
            encoding,
            100,
            algorithm.replace("-", "") + encoding + ".yaml",
            weightVectors ? Map.of("weightVectorFilesDirectory", "resources/weightVectors") : Map.of(),
            new ConfigurationFileReader("defaultConfigurations/" + file).getConfiguration(1),
            null,
            new ProblemSpec(problem),
            referenceFront,
            500,
            1,
            1L,
            indicators,
            null,
            null,
            false,
            tempDir.resolve("output").toString());
    Path statusFile = tempDir.resolve("status.yaml");

    // Act
    Path output = assertDoesNotThrow(() -> new SolveRunner().run(request, statusFile));

    // Assert
    assertTrue(Files.readString(statusFile).contains("FINISHED"), Files.readString(statusFile));
    assertTrue(Files.exists(output.resolve("run-1").resolve("FUN.csv")));
    assertEquals(1, Files.readAllLines(output.resolve("INDICATORS.csv")).size() - 1);
  }
}
