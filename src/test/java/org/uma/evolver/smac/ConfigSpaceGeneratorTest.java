package org.uma.evolver.smac;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;

@DisplayName("Unit tests for class ConfigSpaceGenerator")
class ConfigSpaceGeneratorTest {

  private static String nsgaii(String defaultConfiguration) {
    var space = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
    return ConfigSpaceGenerator.json(space, defaultConfiguration);
  }

  private static int count(String json, String regex) {
    return (int) Pattern.compile(regex).matcher(json).results().count();
  }

  private static String hyperparameter(String json, String name) {
    return json.lines()
        .filter(line -> line.contains("\"name\": \"" + name + "\""))
        .findFirst()
        .orElseThrow();
  }

  private static String condition(String json, String child) {
    return json.lines()
        .filter(line -> line.contains("\"child\": \"" + child + "\""))
        .findFirst()
        .orElseThrow();
  }

  @Nested
  @DisplayName("When the space of NSGA-II is written: ")
  class NSGAIITestCases {

    @Test
    @DisplayName(
        "given the space, when written, then every parameter is a hyperparameter and every"
            + " sub-parameter has a condition")
    void givenTheSpace_whenWritten_thenParametersAndConditionsAreThere() {
      // Act
      String json = nsgaii(null);

      // Assert: 34 parameters, 5 of them top-level
      assertEquals(34, count(json, "\"type\": \"(categorical|uniform_int|uniform_float)\""));
      assertEquals(29, count(json, "\"type\": \"(EQ|IN)\""));
      assertTrue(json.contains("\"format_version\": 0.4"));
    }

    @Test
    @DisplayName(
        "given a conditional parameter, when written, then it is active for the value that"
            + " declares it")
    void givenAConditionalParameter_whenWritten_thenItHasAnEqualsCondition() {
      assertEquals(
          "{\"type\": \"EQ\", \"child\": \"sbxDistributionIndex\", \"parent\": \"crossover\","
              + " \"value\": \"SBX\"},",
          condition(nsgaii(null), "sbxDistributionIndex").trim());
    }

    @Test
    @DisplayName(
        "given a global sub-parameter, when written, then it is active for every value of its"
            + " parent")
    void givenAGlobalSubParameter_whenWritten_thenItHasAnInConditionWithAllTheValues() {
      String line = condition(nsgaii(null), "crossoverProbability");

      assertTrue(line.contains("\"type\": \"IN\""), line);
      assertTrue(line.contains("\"SBX\"") && line.contains("\"SDX\""), line);
    }

    @Test
    @DisplayName(
        "given a categorical integer parameter, when written, then its choices are numbers")
    void givenACategoricalIntegerParameter_whenWritten_thenItsChoicesAreNumbers() {
      assertTrue(
          hyperparameter(nsgaii(null), "offspringPopulationSize")
              .contains("\"choices\": [1, 5, 10, 20, 50, 100, 200, 400]"));
    }

    @Test
    @DisplayName(
        "given no default configuration, when written, then the defaults are the first value and"
            + " the middle of the range")
    void givenNoDefaultConfiguration_whenWritten_thenTheDefaultsAreThoseOfConfigSpace() {
      String json = nsgaii(null);

      assertTrue(hyperparameter(json, "crossover").contains("\"default_value\": \"SBX\""));
      assertTrue(hyperparameter(json, "sbxDistributionIndex").contains("\"default_value\": 202.5"));
    }

    @Test
    @DisplayName(
        "given the default configuration of NSGA-II, when written, then its values are the"
            + " defaults")
    void givenTheDefaultConfiguration_whenWritten_thenItsValuesAreTheDefaults() throws IOException {
      // Arrange
      String defaults =
          new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
              .getConfiguration(1);

      // Act
      String json = nsgaii(defaults);

      // Assert
      assertTrue(hyperparameter(json, "sbxDistributionIndex").contains("\"default_value\": 20.0"));
      assertTrue(
          hyperparameter(json, "offspringPopulationSize").contains("\"default_value\": 100"));
      assertTrue(hyperparameter(json, "selectionTournamentSize").contains("\"default_value\": 2"));
    }
  }

  static Stream<String> baseLevelSpaces() throws IOException {
    try (Stream<Path> files = Files.list(Path.of("src/main/resources/parameterSpaces"))) {
      return files
          .map(file -> file.getFileName().toString())
          .filter(name -> name.endsWith(".yaml") && !name.contains("Meta"))
          .sorted()
          .toList()
          .stream();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("baseLevelSpaces")
  @DisplayName(
      "given a bundled base-level parameter space, when written, then it can be represented")
  void givenABundledSpace_whenWritten_thenItCanBeRepresented(String file) {
    String factory =
        file.contains("Binary")
            ? "Binary"
            : file.contains("Permutation")
                ? "Permutation"
                : file.startsWith("MOPSO") ? "MOPSO" : "Double";
    var space = new YAMLParameterSpace(file, ConfigSpaceGenerator.parameterFactory(factory));

    assertDoesNotThrow(() -> ConfigSpaceGenerator.json(space, null));
  }

  @Test
  @DisplayName("given a name with quotes, when quoted, then they are escaped")
  void givenANameWithQuotes_whenQuoted_thenTheyAreEscaped() {
    assertEquals("\"a\\\"b\\\\c\"", ConfigSpaceGenerator.quote("a\"b\\c"));
    assertEquals(
        List.of("Double", "Binary", "Permutation", "MOPSO"), ConfigSpaceGenerator.FACTORY_NAMES);
  }
}
