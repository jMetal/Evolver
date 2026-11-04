package org.uma.evolver.algorithm.smsemoa;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

@DisplayName("Unit tests for SMS-EMOA with the differential evolution variation")
class SMSEMOADoubleDifferentialEvolutionTest {

  private static final int POPULATION_SIZE = 10;

  private static final String CONFIGURATION =
      "--algorithmResult population "
          + "--createInitialSolutions default "
          + "--variation differentialEvolutionVariation "
          + "--differentialEvolutionCrossover RAND_1_BIN "
          + "--CR 0.5 "
          + "--F 0.5 "
          + "--sequenceGenerator cyclicIntegerSequence "
          + "--takeCurrentSolutionAsParent true "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0";

  @Nested
  @DisplayName("When the replacement is done")
  class Replacement {

    @Test
    @DisplayName("The next selection takes the next solution of the population as the current one")
    void givenADifferentialEvolutionConfigurationWhenTheReplacementIsDoneThenTheCurrentSolutionAdvances() {
      // Arrange
      var problem = new ZDT1();
      var smsemoa =
          new DoubleSMSEMOA(
              problem,
              POPULATION_SIZE,
              1000,
              new YAMLParameterSpace("SMSEMOADouble.yaml", new DoubleParameterFactory()));
      smsemoa.parse(CONFIGURATION.split("\\s+"));
      var algorithm = smsemoa.build();
      List<DoubleSolution> population = new ArrayList<>();
      for (int i = 0; i < POPULATION_SIZE; i++) {
        population.add(problem.evaluate(problem.createSolution()));
      }
      DoubleSolution offspring = problem.evaluate(problem.createSolution());

      int currentSolutionPosition = algorithm.variation().matingPoolSize() - 1;

      // Act
      List<DoubleSolution> firstMatingPool = algorithm.selection().select(population);
      algorithm.replacement().replace(new ArrayList<>(population), List.of(offspring));
      List<DoubleSolution> secondMatingPool = algorithm.selection().select(population);

      // Assert
      assertSame(population.get(0), firstMatingPool.get(currentSolutionPosition));
      assertSame(population.get(1), secondMatingPool.get(currentSolutionPosition));
    }
  }
}
