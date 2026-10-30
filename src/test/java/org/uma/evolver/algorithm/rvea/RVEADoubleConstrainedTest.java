package org.uma.evolver.algorithm.rvea;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.problem.multiobjective.cdtlz.C1_DTLZ1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for RVEA with a constraint-handling technique")
class RVEADoubleConstrainedTest {

  private static final String SPACE = "RVEADoubleConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/RVEADoubleConstrainedDefault.txt";
  private static final String WEIGHT_VECTORS = "resources/weightVectors";
  // The size of the weight vector files W2D_100.dat and W3D_100.dat
  private static final int POPULATION_SIZE = 100;
  private static final int EVALUATIONS = 5000;

  private static DoubleRVEA algorithm(Problem<DoubleSolution> problem, String space) {
    return algorithm(problem, space, EVALUATIONS);
  }

  private static DoubleRVEA algorithm(
      Problem<DoubleSolution> problem, String space, int evaluations) {
    return new DoubleRVEA(
        problem,
        POPULATION_SIZE,
        evaluations,
        WEIGHT_VECTORS,
        new YAMLParameterSpace(space, new DoubleParameterFactory()));
  }

  /** The default configuration with some of its parameters replaced. */
  private static String[] defaultConfigurationWith(String original, String replacement)
      throws IOException {
    return new ConfigurationFileReader(DEFAULT_CONFIGURATION)
        .getConfiguration(1)
        .replace(original, replacement)
        .split("\\s+");
  }

  private static List<DoubleSolution> run(DoubleRVEA rvea, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    rvea.parse(configuration);
    var evolutionaryAlgorithm = rvea.build();
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
    @DisplayName("every technique that judges a single solution returns feasible solutions")
    void givenATechnique_whenRun_thenTheResultHasFeasibleSolutions(String technique)
        throws IOException {
      // Act
      List<DoubleSolution> result =
          run(
              algorithm(new Srinivas(), SPACE),
              defaultConfigurationWith("--constraintHandling cdp", technique));

      // Assert
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("with three objectives, the default technique returns feasible solutions")
    void givenAProblemWithThreeObjectives_whenRun_thenTheResultHasFeasibleSolutions()
        throws IOException {
      // Arrange: RVEA needs about 20000 evaluations to reach the feasible region of C1-DTLZ1
      DoubleRVEA rvea = algorithm(new C1_DTLZ1(7, 3), SPACE, 20000);

      // Act
      List<DoubleSolution> result = run(rvea, defaultConfigurationWith("cdp", "cdp"));

      // Assert
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("a bounded external archive keeps only feasible solutions")
    void givenABoundedArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult population",
              "--algorithmResult externalArchive --archiveType crowdingDistanceArchive");

      // Act
      List<DoubleSolution> result = run(algorithm(new Srinivas(), SPACE), configuration);

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
      DoubleRVEA rvea = algorithm(new Srinivas(), SPACE);
      rvea.parse(defaultConfigurationWith("cdp", "cdp"));

      // Act
      var evolutionaryAlgorithm = rvea.build();

      // Assert
      assertInstanceOf(ConstraintHandlingReplacement.class, evolutionaryAlgorithm.replacement());
    }

    @Test
    @DisplayName("without constraintHandling, a problem with constraints is still rejected")
    void givenTheUnconstrainedSpace_whenBuiltForAProblemWithConstraints_thenItIsRejected()
        throws IOException {
      // Arrange
      DoubleRVEA rvea = algorithm(new Srinivas(), "RVEADouble.yaml");
      // The default configuration without the constraintHandling parameter
      rvea.parse(defaultConfigurationWith("--constraintHandling cdp ", ""));

      // Act and Assert
      assertThrows(InvalidConditionException.class, rvea::build);
    }
  }
}
