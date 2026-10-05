package org.uma.evolver.algorithm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.algorithm.ssmoea.DoubleSSMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

/**
 * The angle density estimator in the algorithms that take it as a parameter. In jMetal 7.7 it threw
 * an exception for a solution without density, which any selection that compares the population by
 * density finds, so it only worked with a random selection.
 */
@DisplayName("Unit tests of the angle density estimator in RDEMOEA and SSMOEA")
class AngleDensityEstimatorAlgorithmsTest {

  private static final int POPULATION_SIZE = 40;
  private static final int EVALUATIONS = 1500;

  private static final String COMMON =
      "--algorithmResult population --createInitialSolutions default"
          + " --densityEstimator angle --angleNeighborhoodSize 3 --ranking dominanceRanking"
          + " --variation crossoverAndMutationVariation --crossover SBX"
          + " --crossoverProbability 0.9 --crossoverRepairStrategy bounds"
          + " --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0"
          + " --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0"
          + " --selectionTournamentSize 2 --boltzmannTemperature 10.0"
          + " --replacement rankingAndDensityEstimator --removalPolicy oneShot";

  private static YAMLParameterSpace space(String file) {
    return new YAMLParameterSpace(file, new DoubleParameterFactory());
  }

  @ParameterizedTest(name = "RDEMOEA with the {0} selection")
  @ValueSource(strings = {"tournament", "random", "ranking", "stochasticUniversalSampling"})
  @DisplayName(
      "given RDEMOEA with the angle estimator, when run with any selection, then it finishes")
  void givenRdemoeaWithAngle_whenRunWithAnySelection_thenItFinishes(String selection) {
    // Arrange
    var algorithm =
        new DoubleRDEMOEA(new ZDT1(), POPULATION_SIZE, EVALUATIONS, space("RDEMOEADouble.yaml"))
            .parse(
                (COMMON + " --offspringPopulationSize 20 --selection " + selection).split("\\s+"))
            .build();

    // Act & Assert
    assertDoesNotThrow(algorithm::run);
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }

  @ParameterizedTest(name = "SSMOEA with the {0} selection")
  @ValueSource(strings = {"tournament", "random", "stochasticUniversalSampling"})
  @DisplayName(
      "given SSMOEA with the angle estimator, when run with any selection, then it finishes")
  void givenSsmoeaWithAngle_whenRunWithAnySelection_thenItFinishes(String selection) {
    // Arrange
    var algorithm =
        new DoubleSSMOEA(new ZDT1(), POPULATION_SIZE, EVALUATIONS, space("SSMOEADouble.yaml"))
            .parse((COMMON + " --gaSelection " + selection).split("\\s+"))
            .build();

    // Act & Assert
    assertDoesNotThrow(algorithm::run);
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }
}
