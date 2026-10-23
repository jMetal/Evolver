package org.uma.evolver.algorithm.nsgaii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for the constrained variant of NSGA-II")
class ConstrainedDoubleNSGAIITest {

  private static final String SPACE = "NSGAIIDoubleConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/NSGAIIDoubleConstrainedDefault.txt";
  private static final int POPULATION_SIZE = 50;
  private static final int EVALUATIONS = 5000;

  private static ConstrainedDoubleNSGAII algorithm() {
    return new ConstrainedDoubleNSGAII(
        new Srinivas(),
        POPULATION_SIZE,
        EVALUATIONS,
        new YAMLParameterSpace(SPACE, new DoubleParameterFactory()));
  }

  /** The default configuration with some of its parameters replaced. */
  private static String[] defaultConfigurationWith(String original, String replacement)
      throws IOException {
    return new ConfigurationFileReader(DEFAULT_CONFIGURATION)
        .getConfiguration(1)
        .replace(original, replacement)
        .split("\\s+");
  }

  private static List<DoubleSolution> run(ConstrainedDoubleNSGAII nsgaII, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    nsgaII.parse(configuration);
    var evolutionaryAlgorithm = nsgaII.build();
    evolutionaryAlgorithm.run();
    return evolutionaryAlgorithm.result();
  }

  @Nested
  @DisplayName("When it solves a problem with constraints")
  class Solving {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "--constraintHandling cdp",
          "--constraintHandling violatedConstraints",
          "--constraintHandling epsilonConstrained --epsilonInitialQuantile 0.5"
              + " --epsilonExponent 2.0 --epsilonDecayFraction 0.5",
          "--constraintHandling stochasticRanking --stochasticRankingProbability 0.45",
          "--constraintHandling pushPull --pushPullChangeThreshold 0.001"
              + " --pushPullWindowFraction 0.05 --pushPullDecayFraction 0.8 --pushPullExponent 2.0"
              + " --pushPullFeasibilityThreshold 0.95 --pushPullReductionRate 0.1",
          "--constraintHandling selfAdaptivePenalty"
        })
    @DisplayName("every technique runs and returns feasible solutions")
    void givenATechnique_whenRun_thenTheResultHasFeasibleSolutions(String technique)
        throws IOException {
      // Arrange
      ConstrainedDoubleNSGAII nsgaII = algorithm();

      // Act
      List<DoubleSolution> result =
          run(nsgaII, defaultConfigurationWith("--constraintHandling cdp", technique));

      // Assert
      assertEquals(POPULATION_SIZE, result.size());
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("a bounded external archive keeps only feasible solutions")
    void givenABoundedArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      ConstrainedDoubleNSGAII nsgaII = algorithm();
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult population",
              "--algorithmResult externalArchive --populationSizeWithArchive 20"
                  + " --archiveType crowdingDistanceArchive");

      // Act
      List<DoubleSolution> result = run(nsgaII, configuration);

      // Assert
      assertTrue(!result.isEmpty() && result.size() <= POPULATION_SIZE);
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }
  }

  @Test
  @DisplayName("createInstance gives the constrained variant")
  void givenTheAlgorithm_whenAnInstanceIsCreated_thenItIsTheConstrainedVariant() {
    // Arrange
    ConstrainedDoubleNSGAII nsgaII = algorithm();

    // Act
    var instance = nsgaII.createInstance(new Srinivas(), EVALUATIONS);

    // Assert
    assertInstanceOf(ConstrainedDoubleNSGAII.class, instance);
  }
}
