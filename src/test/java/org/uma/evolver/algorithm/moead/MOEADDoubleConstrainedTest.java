package org.uma.evolver.algorithm.moead;

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
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.FeasibilityRulesCriterion;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.problem.multiobjective.cdtlz.C1_DTLZ1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for MOEA/D with a subproblem update criterion for constraints")
class MOEADDoubleConstrainedTest {

  private static final String SPACE = "MOEADDoubleConstrained.yaml";
  private static final String DEFAULT_CONFIGURATION =
      "defaultConfigurations/MOEADDoubleConstrainedDefault.txt";
  private static final String WEIGHT_VECTORS = "resources/weightVectors";
  // The size of the weight vector files W2D_100.dat and W3D_100.dat
  private static final int POPULATION_SIZE = 100;
  private static final int EVALUATIONS = 10000;

  private static DoubleMOEAD algorithm(Problem<DoubleSolution> problem, String space) {
    return new DoubleMOEAD(
        problem,
        POPULATION_SIZE,
        EVALUATIONS,
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

  private static List<DoubleSolution> run(DoubleMOEAD moead, String[] configuration) {
    JMetalRandom.getInstance().setSeed(1);
    moead.parse(configuration);
    var evolutionaryAlgorithm = moead.build();
    evolutionaryAlgorithm.run();
    return evolutionaryAlgorithm.result();
  }

  @Nested
  @DisplayName("When it solves a problem with constraints")
  class Solving {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "--subproblemUpdateCriterion feasibilityRules",
          "--subproblemUpdateCriterion improvedEpsilon --improvedEpsilonTao 0.1"
              + " --improvedEpsilonFeasibilityRatioThreshold 0.95"
              + " --improvedEpsilonDecayFraction 0.8",
          "--subproblemUpdateCriterion violationThreshold"
        })
    @DisplayName("every criterion returns feasible solutions")
    void givenACriterion_whenRun_thenTheResultHasFeasibleSolutions(String criterion)
        throws IOException {
      // Act
      List<DoubleSolution> result =
          run(
              algorithm(new Srinivas(), SPACE),
              defaultConfigurationWith("--subproblemUpdateCriterion feasibilityRules", criterion));

      // Assert
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "--subproblemUpdateCriterion feasibilityRules",
          "--subproblemUpdateCriterion improvedEpsilon --improvedEpsilonTao 0.1"
              + " --improvedEpsilonFeasibilityRatioThreshold 0.95"
              + " --improvedEpsilonDecayFraction 0.8"
        })
    @DisplayName("with three objectives, the criteria of jMetal's tests return feasible solutions")
    void givenAProblemWithThreeObjectives_whenRun_thenTheResultHasFeasibleSolutions(
        String criterion) throws IOException {
      // Act
      List<DoubleSolution> result =
          run(
              algorithm(new C1_DTLZ1(7, 3), SPACE),
              defaultConfigurationWith("--subproblemUpdateCriterion feasibilityRules", criterion));

      // Assert
      assertTrue(result.stream().anyMatch(ConstraintHandling::isFeasible));
    }

    @Test
    @DisplayName("an external archive keeps only feasible solutions")
    void givenAnExternalArchive_whenRun_thenItsSolutionsAreFeasible() throws IOException {
      // Arrange
      String[] configuration =
          defaultConfigurationWith(
              "--algorithmResult population",
              "--algorithmResult externalArchive --archiveType unboundedArchive");

      // Act
      List<DoubleSolution> result = run(algorithm(new Srinivas(), SPACE), configuration);

      // Assert
      assertTrue(!result.isEmpty());
      assertTrue(result.stream().allMatch(ConstraintHandling::isFeasible));
    }
  }

  @Nested
  @DisplayName("When the parameter space decides whether the constraints are handled")
  class ParameterSpaceDecides {

    @Test
    @DisplayName("with subproblemUpdateCriterion, the criterion of the parameter decides")
    void givenTheConstrainedSpace_whenBuilt_thenTheCriterionOfTheParameterDecides()
        throws IOException {
      // Arrange
      DoubleMOEAD moead = algorithm(new Srinivas(), SPACE);
      moead.parse(defaultConfigurationWith("feasibilityRules", "feasibilityRules"));

      // Act
      moead.build();

      // Assert
      assertTrue(moead.usingSubproblemUpdateCriterion());
      assertInstanceOf(FeasibilityRulesCriterion.class, moead.createSubproblemUpdateCriterion());
    }

    @Test
    @DisplayName("without subproblemUpdateCriterion, the aggregation values alone decide")
    void givenTheUnconstrainedSpace_whenBuilt_thenNoCriterionIsUsed() throws IOException {
      // Arrange
      DoubleMOEAD moead = algorithm(new Srinivas(), "MOEADDouble.yaml");
      // The default configuration without the subproblemUpdateCriterion parameter
      moead.parse(
          defaultConfigurationWith("--subproblemUpdateCriterion feasibilityRules ", ""));

      // Act
      moead.build();

      // Assert
      assertFalse(moead.usingSubproblemUpdateCriterion());
    }
  }
}
