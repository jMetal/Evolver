package org.uma.evolver.algorithm.mopso;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.component.catalogue.pso.localbestupdate.impl.ConstraintHandlingLocalBestUpdate;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for MOPSO with a constraint-handling technique")
class MOPSOConstrainedTest {

  private static final String SPACE = "MOPSOConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/MOPSOConstrainedDefault.txt";
  private static final int LEADER_ARCHIVE_SIZE = 50;
  private static final int EVALUATIONS = 5000;

  private static BaseMOPSO algorithm(String space) {
    return new BaseMOPSO(
        new Srinivas(),
        LEADER_ARCHIVE_SIZE,
        EVALUATIONS,
        new YAMLParameterSpace(space, new MOPSOParameterFactory()));
  }

  /** The default configuration with some of its parameters replaced. */
  private static String[] defaultConfigurationWith(String original, String replacement)
      throws IOException {
    return new ConfigurationFileReader(DEFAULT_CONFIGURATION)
        .getConfiguration(1)
        .replace(original, replacement)
        .split("\\s+");
  }

  private static List<DoubleSolution> run(BaseMOPSO mopso, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    mopso.parse(configuration);
    var algorithm = mopso.build();
    algorithm.run();
    return algorithm.result();
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
      // Act
      List<DoubleSolution> result =
          run(algorithm(SPACE), defaultConfigurationWith("--constraintHandling cdp", technique));

      // Assert: the result is the leader archive, which keeps the constrained-domination principle
      assertTrue(!result.isEmpty() && result.size() <= LEADER_ARCHIVE_SIZE);
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("with an external archive, the result has only feasible solutions")
    void givenAnExternalArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult leaderArchive",
              "--algorithmResult externalArchive --swarmSizeWithArchive 50"
                  + " --externalArchiveType crowdingDistanceArchive");

      // Act
      List<DoubleSolution> result = run(algorithm(SPACE), configuration);

      // Assert
      assertTrue(!result.isEmpty());
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }
  }

  @Nested
  @DisplayName("When the parameter space decides whether the constraints are handled")
  class ParameterSpaceDecides {

    @Test
    @DisplayName("with constraintHandling, the technique updates the local best")
    void givenTheConstrainedSpace_whenBuilt_thenTheLocalBestUpdateHandlesTheConstraints()
        throws IOException {
      // Arrange
      BaseMOPSO mopso = algorithm(SPACE);
      mopso.parse(defaultConfigurationWith("cdp", "cdp"));

      // Act
      mopso.build();

      // Assert
      assertInstanceOf(ConstraintHandlingLocalBestUpdate.class, mopso.createLocalBestUpdate());
    }

    @Test
    @DisplayName("without constraintHandling, the local best update is the usual one")
    void givenTheUnconstrainedSpace_whenBuilt_thenTheLocalBestUpdateIsTheUsualOne()
        throws IOException {
      // Arrange
      BaseMOPSO mopso = algorithm("MOPSO.yaml");
      // The default configuration without the constraintHandling parameter
      mopso.parse(defaultConfigurationWith("--constraintHandling cdp ", ""));

      // Act
      mopso.build();

      // Assert
      assertFalse(mopso.createLocalBestUpdate() instanceof ConstraintHandlingLocalBestUpdate);
    }
  }
}
