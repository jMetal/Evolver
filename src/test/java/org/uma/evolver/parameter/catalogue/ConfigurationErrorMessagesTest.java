package org.uma.evolver.parameter.catalogue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.nsgaii.BinaryNSGAII;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

/**
 * Checks that an invalid configuration fails with an error that names the parameter causing it.
 * The parameter space is {@code NSGAIIDouble.yaml} with ranges wider than the operators accept
 * (test fixture {@code NSGAIIDoubleWideRanges.yaml}), so that the invalid values pass the parsing
 * of the configuration and reach the operators.
 */
@DisplayName("Unit tests of the error messages of invalid configurations")
class ConfigurationErrorMessagesTest {

  private static String defaultConfiguration() throws IOException {
    return new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
        .getConfiguration(1);
  }

  private static void runOnZDT1(String configuration) {
    var space = new YAMLParameterSpace("NSGAIIDoubleWideRanges.yaml", new DoubleParameterFactory());
    var algorithm =
        new DoubleNSGAII(new ZDT1(), 100, 300, space).parse(configuration.split("\\s+")).build();
    algorithm.run();
  }

  private static String withValue(String configuration, String parameter, String value) {
    return configuration.replaceAll("--" + parameter + " \\S+", "--" + parameter + " " + value);
  }

  private static void assertFailsMentioning(String configuration, String... fragments) {
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> runOnZDT1(configuration));
    for (String fragment : fragments) {
      assertTrue(
          exception.getMessage().contains(fragment),
          "'" + fragment + "' not in: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("When a value is invalid for the operators: ")
  class InvalidValueTestCases {

    @Test
    @DisplayName(
        "given a tournament larger than the population, when run, then the error names"
            + " selectionTournamentSize and the size of the population")
    void givenATournamentLargerThanThePopulation_whenRun_thenTheErrorNamesIt() throws IOException {
      assertFailsMentioning(
          withValue(defaultConfiguration(), "selectionTournamentSize", "150"),
          "selectionTournamentSize (150)",
          "(100 solutions)");
    }

    @Test
    @DisplayName(
        "given a crossover probability above 1, when run, then the error names"
            + " crossoverProbability")
    void givenACrossoverProbabilityAboveOne_whenRun_thenTheErrorNamesIt() throws IOException {
      assertFailsMentioning(
          withValue(defaultConfiguration(), "crossoverProbability", "1.5"),
          "crossoverProbability (1.5) is not a probability");
    }

    @Test
    @DisplayName(
        "given a mutation factor larger than the number of variables, when run, then the error"
            + " names mutationProbabilityFactor and the valid range for the problem")
    void givenAMutationFactorLargerThanTheVariables_whenRun_thenTheErrorNamesIt()
        throws IOException {
      assertFailsMentioning(
          withValue(defaultConfiguration(), "mutationProbabilityFactor", "40.0"),
          "mutationProbabilityFactor (40.0)",
          "variables of the problem (30)",
          "must be in [0, 30]");
    }

    @Test
    @DisplayName(
        "given an offspring population of 0, when run, then the error names"
            + " offspringPopulationSize and its value")
    void givenAnOffspringPopulationOfZero_whenRun_thenTheErrorNamesIt() throws IOException {
      assertFailsMentioning(
          withValue(defaultConfiguration(), "offspringPopulationSize", "0"),
          "offspringPopulationSize must be a positive integer, but it is 0");
    }
  }

  @Nested
  @DisplayName("When a value is at the limit of its range: ")
  class LimitValueTestCases {

    @Test
    @DisplayName(
        "given a binary mutation factor of 0, when run, then it runs (no mutation is a valid"
            + " setting, as in the ranges of the binary parameter spaces)")
    void givenABinaryMutationFactorOfZero_whenRun_thenItRuns() {
      String configuration =
          "--algorithmResult population --createInitialSolutions default"
              + " --offspringPopulationSize 100 --variation crossoverAndMutationVariation"
              + " --crossover HUX --crossoverProbability 0.9 --mutation bitFlip"
              + " --mutationProbabilityFactor 0.0 --selection tournament"
              + " --selectionTournamentSize 2";
      var space = new YAMLParameterSpace("NSGAIIBinary.yaml", new BinaryParameterFactory());

      assertDoesNotThrow(
          () ->
              new BinaryNSGAII(new OneZeroMax(64), 100, 300, space)
                  .parse(configuration.split("\\s+"))
                  .build()
                  .run());
    }
  }
}
