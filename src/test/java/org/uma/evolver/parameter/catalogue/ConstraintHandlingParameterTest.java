package org.uma.evolver.parameter.catalogue;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.constrainthandling.ConstrainedDominance;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for the constraint-handling technique of an algorithm")
class ConstraintHandlingParameterTest {

  private static final String SPACE =
      "src/test/resources/parameterSpaces/ConstraintHandlingParameterSpace.yaml";

  private static ParameterSpace parsed(String arguments) {
    ParameterSpace parameterSpace = new YAMLParameterSpace(SPACE, new DoubleParameterFactory());
    parameterSpace.get(ConstraintHandlingParameter.DEFAULT_NAME).parse(arguments.split(" "));
    return parameterSpace;
  }

  @Nested
  @DisplayName("When the technique comes from a parameter space")
  class Technique {

    @Test
    @DisplayName("a space without the parameter gives the constrained-domination principle")
    void givenASpaceWithoutTheParameter_whenAsked_thenItIsConstrainedDominance() {
      // Arrange
      ParameterSpace space =
          new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());

      // Act
      ConstraintHandlingTechnique<DoubleSolution> technique =
          ConstraintHandlingParameter.technique(space);

      // Assert
      assertInstanceOf(ConstrainedDominance.class, technique);
    }

    @ParameterizedTest(name = "{0} gives {1}")
    @CsvSource({
      "'--constraintHandling cdp', ConstrainedDominance",
      "'--constraintHandling violatedConstraints', ConstrainedDominance",
      "'--constraintHandling epsilonConstrained --epsilonInitialQuantile 0.2 --epsilonExponent 5.0"
          + " --epsilonDecayFraction 0.5', EpsilonConstrained",
      "'--constraintHandling stochasticRanking --stochasticRankingProbability 0.45',"
          + " StochasticRanking",
      "'--constraintHandling pushPull --pushPullChangeThreshold 0.001 --pushPullWindowFraction 0.02"
          + " --pushPullDecayFraction 0.8 --pushPullExponent 2.0 --pushPullFeasibilityThreshold 0.95"
          + " --pushPullReductionRate 0.1', PushPullSearch",
      "'--constraintHandling selfAdaptivePenalty', SelfAdaptivePenalty"
    })
    @DisplayName("a space with the parameter gives the technique of its value")
    void givenAValue_whenAsked_thenItIsTheTechniqueOfThatValue(
        String arguments, String techniqueClass) throws ClassNotFoundException {
      // Arrange
      ParameterSpace space = parsed(arguments);

      // Act
      ConstraintHandlingTechnique<DoubleSolution> technique =
          ConstraintHandlingParameter.technique(space);

      // Assert
      assertInstanceOf(
          Class.forName("org.uma.jmetal.util.constrainthandling." + techniqueClass), technique);
    }
  }

  @Test
  @DisplayName("an unknown technique is rejected")
  void givenAnUnknownTechnique_whenCreated_thenItFails() {
    // Arrange
    var parameter = new ConstraintHandlingParameter<DoubleSolution>(java.util.List.of("unknown"));
    parameter.parse(new String[] {"--constraintHandling", "unknown"});

    // Act and Assert
    assertThrows(JMetalException.class, parameter::getConstraintHandlingTechnique);
  }
}
