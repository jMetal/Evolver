package org.uma.evolver.algorithm.agemoea;

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
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.AGEMOEAReplacement;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for AGE-MOEA with a constraint-handling technique")
class AGEMOEADoubleConstrainedTest {

  private static final String SPACE = "AGEMOEADoubleConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/AGEMOEADoubleConstrainedDefault.txt";
  private static final int POPULATION_SIZE = 50;
  private static final int EVALUATIONS = 5000;

  private static DoubleAGEMOEA algorithm() {
    return new DoubleAGEMOEA(
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

  private static List<DoubleSolution> run(DoubleAGEMOEA agemoea, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    agemoea.parse(configuration);
    var evolutionaryAlgorithm = agemoea.build();
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
      DoubleAGEMOEA agemoea = algorithm();

      // Act
      List<DoubleSolution> result =
          run(agemoea, defaultConfigurationWith("--constraintHandling cdp", technique));

      // Assert
      assertEquals(POPULATION_SIZE, result.size());
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("AGE-MOEA-II with a technique returns feasible solutions")
    void givenAGEMOEAII_whenRun_thenTheResultHasFeasibleSolutions() throws IOException {
      // Arrange
      DoubleAGEMOEA agemoea = algorithm();

      // Act
      List<DoubleSolution> result =
          run(
              agemoea,
              defaultConfigurationWith("--replacement agemoea", "--replacement agemoeaII"));

      // Assert
      assertEquals(POPULATION_SIZE, result.size());
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("a bounded external archive keeps only feasible solutions")
    void givenABoundedArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      DoubleAGEMOEA agemoea = algorithm();
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult population",
              "--algorithmResult externalArchive --populationSizeWithArchive 20"
                  + " --archiveType crowdingDistanceArchive");

      // Act
      List<DoubleSolution> result = run(agemoea, configuration);

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
      DoubleAGEMOEA agemoea = algorithm();
      agemoea.parse(defaultConfigurationWith("cdp", "cdp"));

      // Act
      var evolutionaryAlgorithm = agemoea.build();

      // Assert
      assertInstanceOf(ConstraintHandlingReplacement.class, evolutionaryAlgorithm.replacement());
    }

    @Test
    @DisplayName("without constraintHandling, the replacement is the one of AGE-MOEA")
    void givenTheUnconstrainedSpace_whenBuilt_thenTheReplacementIsTheUsualOne() throws IOException {
      // Arrange
      DoubleAGEMOEA agemoea =
          new DoubleAGEMOEA(
              new Srinivas(),
              POPULATION_SIZE,
              EVALUATIONS,
              new YAMLParameterSpace("AGEMOEADouble.yaml", new DoubleParameterFactory()));
      // The default configuration without the constraintHandling parameter
      agemoea.parse(defaultConfigurationWith("--constraintHandling cdp ", ""));

      // Act
      var evolutionaryAlgorithm = agemoea.build();

      // Assert
      assertInstanceOf(
          AGEMOEAReplacement.class, evolutionaryAlgorithm.replacement());
    }
  }
}
