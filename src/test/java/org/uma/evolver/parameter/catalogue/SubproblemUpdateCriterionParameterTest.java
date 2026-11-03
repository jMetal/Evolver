package org.uma.evolver.parameter.catalogue;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.FeasibilityRulesCriterion;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.ImprovedEpsilonCriterion;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.ViolationThresholdCriterion;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class SubproblemUpdateCriterionParameter")
class SubproblemUpdateCriterionParameterTest {

  @SuppressWarnings("unchecked")
  private static SubproblemUpdateCriterionParameter<DoubleSolution> parsed(String arguments) {
    ParameterSpace space =
        new YAMLParameterSpace("MOEADDoubleConstrained.yaml", new DoubleParameterFactory());
    var parameter =
        (SubproblemUpdateCriterionParameter<DoubleSolution>)
            space.get(SubproblemUpdateCriterionParameter.DEFAULT_NAME);
    parameter.parse(arguments.split(" "));
    return parameter;
  }

  @Nested
  @DisplayName("When creating the criterion of each value")
  class Criteria {

    @Test
    @DisplayName("feasibilityRules gives jMetal's feasibility rules criterion")
    void givenFeasibilityRules_whenCreated_thenItIsTheFeasibilityRulesCriterion() {
      // Arrange
      var parameter = parsed("--subproblemUpdateCriterion feasibilityRules");

      // Act
      var criterion = parameter.getSubproblemUpdateCriterion();

      // Assert
      assertInstanceOf(FeasibilityRulesCriterion.class, criterion);
    }

    @Test
    @DisplayName("improvedEpsilon gives jMetal's improved epsilon criterion")
    void givenImprovedEpsilon_whenCreated_thenItIsTheImprovedEpsilonCriterion() {
      // Arrange
      var parameter =
          parsed(
              "--subproblemUpdateCriterion improvedEpsilon --improvedEpsilonTao 0.1"
                  + " --improvedEpsilonFeasibilityRatioThreshold 0.95"
                  + " --improvedEpsilonDecayFraction 0.8");
      parameter.addNonConfigurableSubParameter("numberOfGenerations", 250);

      // Act
      var criterion = parameter.getSubproblemUpdateCriterion();

      // Assert
      assertInstanceOf(ImprovedEpsilonCriterion.class, criterion);
    }

    @Test
    @DisplayName("improvedEpsilon needs the number of generations of the run")
    void givenImprovedEpsilonWithoutGenerations_whenCreated_thenItFails() {
      // Arrange
      var parameter =
          parsed(
              "--subproblemUpdateCriterion improvedEpsilon --improvedEpsilonTao 0.1"
                  + " --improvedEpsilonFeasibilityRatioThreshold 0.95"
                  + " --improvedEpsilonDecayFraction 0.8");

      // Act
      Executable creation = parameter::getSubproblemUpdateCriterion;

      // Assert
      assertThrows(NullParameterException.class, creation);
    }

    @Test
    @DisplayName("violationThreshold gives jMetal's violation threshold criterion")
    void givenViolationThreshold_whenCreated_thenItIsTheViolationThresholdCriterion() {
      // Arrange
      var parameter = parsed("--subproblemUpdateCriterion violationThreshold");

      // Act
      var criterion = parameter.getSubproblemUpdateCriterion();

      // Assert
      assertInstanceOf(ViolationThresholdCriterion.class, criterion);
    }
  }
}
