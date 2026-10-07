package org.uma.evolver.parameter.catalogue.mutationparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.example.tutorial.BiSphere;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithBoundValue;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for the Gaussian mutation of tutorial E17")
class GaussianMutationTest {

  @Test
  @DisplayName("given a probability of 1, when applied, then every variable changes and stays in its bounds")
  void givenProbabilityOne_whenApplied_thenEveryVariableChangesAndStaysInBounds() {
    // Arrange
    JMetalRandom.getInstance().setSeed(1);
    DoubleSolution solution = new BiSphere(10).createSolution();
    List<Double> before = List.copyOf(solution.variables());
    var mutation = new GaussianMutation(1.0, 0.1, new RepairDoubleSolutionWithBoundValue());

    // Act
    mutation.execute(solution);

    // Assert
    for (int i = 0; i < before.size(); i++) {
      assertTrue(solution.variables().get(i) != before.get(i).doubleValue());
      assertTrue(solution.variables().get(i) >= -5.0 && solution.variables().get(i) <= 5.0);
    }
  }

  @Test
  @DisplayName("given a probability of 0, when applied, then nothing changes")
  void givenProbabilityZero_whenApplied_thenNothingChanges() {
    // Arrange
    DoubleSolution solution = new BiSphere(10).createSolution();
    List<Double> before = List.copyOf(solution.variables());

    // Act
    new GaussianMutation(0.0, 0.1, new RepairDoubleSolutionWithBoundValue()).execute(solution);

    // Assert
    assertEquals(before, solution.variables());
  }

  @Test
  @DisplayName("given a sigma that is not positive, when built, then it fails")
  void givenANonPositiveSigma_whenBuilt_thenItFails() {
    assertThrows(
        RuntimeException.class,
        () -> new GaussianMutation(0.5, 0.0, new RepairDoubleSolutionWithBoundValue()));
  }

  @Test
  @DisplayName("given the parameter with the gaussian value, when parsed, then it builds the operator")
  void givenTheGaussianValue_whenParsed_thenItBuildsTheOperator() {
    // Arrange
    var parameter = new DoubleMutationParameter(List.of("polynomial", "gaussian"));
    parameter.addGlobalSubParameter(new DoubleParameter("mutationProbabilityFactor", 0.0, 2.0));
    parameter.addGlobalSubParameter(
        new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", List.of("bounds")));
    parameter.addConditionalParameter("gaussian", new DoubleParameter("gaussianMutationSigma", 0.001, 0.5));
    parameter.addNonConfigurableSubParameter("numberOfProblemVariables", 10);
    parameter.parse(
        "--mutation gaussian --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --gaussianMutationSigma 0.1"
            .split(" "));

    // Act
    MutationOperator<DoubleSolution> operator = parameter.getMutation();

    // Assert
    assertInstanceOf(GaussianMutation.class, operator);
    assertEquals(0.1, operator.mutationProbability(), 1e-12);
  }
}
