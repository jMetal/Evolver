package org.uma.evolver.algorithm.paes;

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
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.PAESReplacement;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for PAES with a constraint-handling technique")
class PAESDoubleConstrainedTest {

  private static final String SPACE = "PAESDoubleConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/PAESDoubleConstrainedDefault.txt";
  private static final int POPULATION_SIZE = 50;
  private static final int EVALUATIONS = 5000;

  private static DoublePAES algorithm() {
    return new DoublePAES(
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

  private static List<DoubleSolution> run(DoublePAES paes, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    paes.parse(configuration);
    var evolutionaryAlgorithm = paes.build();
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
          "--constraintHandling pushPull --pushPullChangeThreshold 0.001"
              + " --pushPullWindowFraction 0.05 --pushPullDecayFraction 0.8 --pushPullExponent 2.0"
              + " --pushPullFeasibilityThreshold 0.95 --pushPullReductionRate 0.1"
        })
    @DisplayName("every technique that compares pairs runs and returns only feasible solutions")
    void givenATechnique_whenRun_thenTheResultHasOnlyFeasibleSolutions(String technique)
        throws IOException {
      // Arrange
      DoublePAES paes = algorithm();

      // Act
      List<DoubleSolution> result =
          run(paes, defaultConfigurationWith("--constraintHandling cdp", technique));

      // Assert: the result is the PAES archive, which keeps the constrained-domination principle
      assertTrue(!result.isEmpty() && result.size() <= POPULATION_SIZE);
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("with an external archive, the result has only feasible solutions")
    void givenAnExternalArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      DoublePAES paes = algorithm();
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult paesArchive", "--algorithmResult externalArchive");

      // Act
      List<DoubleSolution> result = run(paes, configuration);

      // Assert
      assertTrue(!result.isEmpty() && result.size() <= POPULATION_SIZE);
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }
  }

  @Nested
  @DisplayName("When the parameter space decides whether the constraints are handled")
  class ParameterSpaceDecides {

    @Test
    @DisplayName("with constraintHandling, the replacement updates the technique")
    void givenTheConstrainedSpace_whenBuilt_thenTheReplacementHandlesTheConstraints()
        throws IOException {
      // Arrange
      DoublePAES paes = algorithm();
      paes.parse(defaultConfigurationWith("cdp", "cdp"));

      // Act
      var evolutionaryAlgorithm = paes.build();

      // Assert
      assertInstanceOf(ConstraintHandlingReplacement.class, evolutionaryAlgorithm.replacement());
    }

    @Test
    @DisplayName("without constraintHandling, the replacement is the one of PAES")
    void givenTheUnconstrainedSpace_whenBuilt_thenTheReplacementIsTheUsualOne() throws IOException {
      // Arrange
      DoublePAES paes =
          new DoublePAES(
              new Srinivas(),
              POPULATION_SIZE,
              EVALUATIONS,
              new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));
      // The default configuration without the constraintHandling parameter
      paes.parse(defaultConfigurationWith("--constraintHandling cdp ", ""));

      // Act
      var evolutionaryAlgorithm = paes.build();

      // Assert
      assertInstanceOf(
          PAESReplacement.class, evolutionaryAlgorithm.replacement());
    }
  }
}
