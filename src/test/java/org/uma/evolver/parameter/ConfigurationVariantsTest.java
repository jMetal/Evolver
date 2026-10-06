package org.uma.evolver.parameter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class ConfigurationVariants")
class ConfigurationVariantsTest {

  private ParameterSpace space;
  private String tuned;
  private String defaults;

  @BeforeEach
  void setUp() throws IOException {
    space = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
    tuned = new ConfigurationFileReader("tunedConfigurations/NSGAIIWFG2D.txt").getConfiguration(1);
    defaults =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);
  }

  private static Map<String, String> values(String configuration) {
    return ConfigurationVariants.parameterValues(configuration);
  }

  @Nested
  @DisplayName("When a parameter is fixed: ")
  class FixingTestCases {

    @Test
    @DisplayName(
        "given a tuned configuration with an archive, when the result is set to the population,"
            + " then the archive parameters disappear and the rest is kept")
    void givenAnArchive_whenTheResultIsThePopulation_thenTheArchiveParametersDisappear() {
      // Act
      String variant =
          ConfigurationVariants.derive(
              space, tuned, Map.of("algorithmResult", "population"), defaults);

      // Assert
      Map<String, String> result = values(variant);
      assertEquals("population", result.get("algorithmResult"));
      assertFalse(result.containsKey("populationSizeWithArchive"));
      assertFalse(result.containsKey("archiveType"));
      assertEquals("levyFlight", result.get("mutation"));
      assertEquals("8", result.get("selectionTournamentSize"));
    }

    @Test
    @DisplayName(
        "given a tuned crossover, when it is set to SBX, then its sub-parameters are replaced by"
            + " those of SBX, taken from the fallback")
    void givenATunedCrossover_whenSetToSbx_thenItsSubParametersComeFromTheFallback() {
      // Act
      String variant =
          ConfigurationVariants.derive(space, tuned, Map.of("crossover", "SBX"), defaults);

      // Assert
      Map<String, String> result = values(variant);
      assertEquals("SBX", result.get("crossover"));
      assertEquals("20.0", result.get("sbxDistributionIndex"));
      assertFalse(result.containsKey("blxAlphaBetaCrossoverAlpha"));
      assertFalse(result.containsKey("blxAlphaBetaCrossoverBeta"));
      // A global sub-parameter of the crossover keeps the tuned value
      assertEquals("0.8755200112654127", result.get("crossoverProbability"));
    }

    @Test
    @DisplayName(
        "given a change that activates a parameter, when a value is given for it, then that value"
            + " is used instead of the fallback")
    void givenAnActivatedParameter_whenItsValueIsGiven_thenThatValueIsUsed() {
      // Act
      String variant =
          ConfigurationVariants.derive(
              space, tuned, Map.of("crossover", "SBX", "sbxDistributionIndex", "5.0"), defaults);

      // Assert
      assertEquals("5.0", values(variant).get("sbxDistributionIndex"));
    }

    @Test
    @DisplayName("given no changes, when derived, then the configuration is the same")
    void givenNoChanges_whenDerived_thenTheConfigurationIsTheSame() {
      // Act
      String variant = ConfigurationVariants.derive(space, tuned, Map.of(), defaults);

      // Assert
      assertEquals(values(tuned), values(variant));
    }

    @Test
    @DisplayName(
        "given a variant, when it configures NSGA-II, then the algorithm builds and runs")
    void givenAVariant_whenItConfiguresNsgaii_thenItRuns() {
      // Arrange
      String variant =
          ConfigurationVariants.derive(
              space,
              tuned,
              Map.of("algorithmResult", "population", "crossover", "SBX", "mutation", "polynomial"),
              defaults);

      // Act & Assert
      assertDoesNotThrow(
          () ->
              new DoubleNSGAII(new ZDT1(), 100, 1000, space.createInstance())
                  .parse(variant.split("\\s+"))
                  .build()
                  .run());
    }
  }

  @Nested
  @DisplayName("When a change is not valid: ")
  class InvalidChangeTestCases {

    @Test
    @DisplayName("given an unknown parameter, when derived, then it fails naming it")
    void givenAnUnknownParameter_whenDerived_thenItFailsNamingIt() {
      var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ConfigurationVariants.derive(space, tuned, Map.of("noSuchThing", "1"), defaults));
      assertTrue(exception.getMessage().contains("noSuchThing"));
    }

    @Test
    @DisplayName(
        "given a parameter that the variant does not activate, when derived, then it fails naming"
            + " it")
    void givenAnInactiveParameter_whenDerived_thenItFailsNamingIt() {
      // sbxDistributionIndex is not active with the tuned crossover (blxAlphaBeta)
      var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  ConfigurationVariants.derive(
                      space, tuned, Map.of("sbxDistributionIndex", "5.0"), defaults));
      assertTrue(exception.getMessage().contains("sbxDistributionIndex"));
    }

    @Test
    @DisplayName("given an invalid value, when derived, then it fails")
    void givenAnInvalidValue_whenDerived_thenItFails() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ConfigurationVariants.derive(space, tuned, Map.of("crossover", "noSuchCrossover"), defaults));
    }

    @Test
    @DisplayName(
        "given an activated parameter with no value in the fallback, when derived, then it fails"
            + " naming it")
    void givenAnActivatedParameterWithoutValue_whenDerived_thenItFailsNamingIt() {
      // The default configuration has no value for the Laplace crossover's scale
      var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ConfigurationVariants.derive(space, tuned, Map.of("crossover", "laplace"), defaults));
      assertTrue(exception.getMessage().contains("laplaceCrossoverScale"));
    }
  }
}
