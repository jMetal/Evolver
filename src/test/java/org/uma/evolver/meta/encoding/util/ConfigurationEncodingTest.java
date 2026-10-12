package org.uma.evolver.meta.encoding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class ConfigurationEncoding")
class ConfigurationEncodingTest {

  private static final Pattern DEFAULT_FILE =
      Pattern.compile("(.+)(Double|Binary|Permutation)Default\\.txt");

  /** Every default configuration whose parameter space is bundled, with that space. */
  static Stream<Arguments> defaultConfigurations() throws IOException {
    try (Stream<Path> files = Files.list(Path.of("src/main/resources/defaultConfigurations"))) {
      return files
          .map(file -> file.getFileName().toString())
          .sorted()
          .map(DEFAULT_FILE::matcher)
          .filter(Matcher::matches)
          .filter(
              m ->
                  Files.exists(
                      Path.of("src/main/resources/parameterSpaces", m.group(1) + m.group(2) + ".yaml")))
          .map(m -> Arguments.of(m.group(0), m.group(1) + m.group(2) + ".yaml", m.group(2)))
          .toList()
          .stream();
    }
  }

  private static String[] configuration(String file) throws IOException {
    return new ConfigurationFileReader("defaultConfigurations/" + file)
        .getConfiguration(1)
        .trim()
        .split("\\s+");
  }

  private static void assertSameValues(Map<String, String> expected, Map<String, String> actual) {
    for (Map.Entry<String, String> entry : actual.entrySet()) {
      String original = expected.get(entry.getKey());
      assertTrue(original != null, "decoded a parameter not in the configuration: " + entry.getKey());
      if (original.matches("-?\\d+\\.\\d+(E-?\\d+)?") || original.contains(".")) {
        try {
          assertEquals(
              Double.parseDouble(original), Double.parseDouble(entry.getValue()), 1e-9, entry.getKey());
          continue;
        } catch (NumberFormatException e) {
          // not a number: compared as text below
        }
      }
      assertEquals(original, entry.getValue(), entry.getKey());
    }
  }

  @Nested
  @DisplayName("When encoding the default configurations: ")
  class RoundTrip {

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.meta.encoding.util.ConfigurationEncodingTest#defaultConfigurations")
    @DisplayName(
        "given a default configuration, when encoded flat and decoded, then its parameters keep their"
            + " values")
    void givenDefaultConfiguration_whenEncodedFlat_thenDecodingGivesItBack(
        String file, String space, String encoding) throws IOException {
      // Arrange
      String[] configuration = configuration(file);
      YAMLParameterSpace parameterSpace = BaseAlgorithmRegistry.resolveParameterSpace(encoding, space);
      List<Parameter<?>> parameters =
          ParameterManagement.parameterFlattening(parameterSpace.topLevelParameters());

      // Act
      List<Double> variables = ConfigurationEncoding.toFlat(configuration, parameters);
      String[] decoded =
          ParameterManagement.decodeParametersToString(parameters, variables).toString().split("\\s+");

      // Assert
      Map<String, String> original = ConfigurationEncoding.values(configuration);
      Map<String, String> decodedValues = ConfigurationEncoding.values(decoded);
      decodedValues.keySet().retainAll(original.keySet());
      assertFalse(decodedValues.isEmpty());
      assertSameValues(original, decodedValues);
      assertTrue(variables.stream().allMatch(v -> v >= 0.0 && v <= 1.0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.meta.encoding.util.ConfigurationEncodingTest#defaultConfigurations")
    @DisplayName(
        "given a default configuration, when encoded as a tree and decoded, then it gives the active"
            + " parameters of the configuration")
    void givenDefaultConfiguration_whenEncodedAsTree_thenDecodingGivesItBack(
        String file, String space, String encoding) throws IOException {
      // Arrange
      String[] configuration = configuration(file);
      YAMLParameterSpace parameterSpace = BaseAlgorithmRegistry.resolveParameterSpace(encoding, space);

      // Act
      String[] decoded =
          ConfigurationEncoding.toTree(configuration, parameterSpace, 2).toParameterArray();

      // Assert
      Map<String, String> decodedValues = ConfigurationEncoding.values(decoded);
      assertFalse(decodedValues.isEmpty());
      assertSameValues(ConfigurationEncoding.values(configuration), decodedValues);
    }
  }

  @Nested
  @DisplayName("When a configuration does not fit the parameter space: ")
  class InvalidConfigurations {

    @Test
    @DisplayName("given a categorical value that the space does not have, when encoded, then it fails")
    void givenUnknownCategoricalValue_whenEncodedFlat_thenItFails() {
      // Arrange
      YAMLParameterSpace parameterSpace =
          BaseAlgorithmRegistry.resolveParameterSpace("Double", "NSGAIIDouble.yaml");
      List<Parameter<?>> parameters =
          ParameterManagement.parameterFlattening(parameterSpace.topLevelParameters());

      // Act & Assert
      assertThrows(
          JMetalException.class,
          () -> ConfigurationEncoding.toFlat(new String[] {"--crossover", "unknown"}, parameters));
    }

    @Test
    @DisplayName("given a configuration without an active parameter, when encoded as a tree, then it fails")
    void givenMissingParameter_whenEncodedAsTree_thenItFails() {
      // Arrange
      YAMLParameterSpace parameterSpace =
          BaseAlgorithmRegistry.resolveParameterSpace("Double", "NSGAIIDouble.yaml");

      // Act & Assert
      assertThrows(
          JMetalException.class,
          () ->
              ConfigurationEncoding.toTree(
                  new String[] {"--algorithmResult", "population"}, parameterSpace, 2));
    }
  }
}
