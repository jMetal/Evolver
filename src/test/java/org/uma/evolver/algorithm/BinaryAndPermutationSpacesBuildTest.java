package org.uma.evolver.algorithm;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.moead.PermutationMOEAD;
import org.uma.evolver.algorithm.smsemoa.BinarySMSEMOA;
import org.uma.evolver.algorithm.smsemoa.PermutationSMSEMOA;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.solution.Solution;

/**
 * Builds and runs, with a minimal budget, the binary and permutation variants of algorithms whose
 * parameter space and code had drifted apart (a parse alone does not detect it, since the operators
 * are only looked up when the algorithm is built).
 */
@DisplayName("Binary and permutation algorithms built from their own parameter spaces")
class BinaryAndPermutationSpacesBuildTest {

  private static final int POPULATION_SIZE = 20;
  private static final int MAX_EVALUATIONS = 200;

  private static void assertReturnsSolutions(List<? extends Solution<?>> result) {
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("Given MOEADPermutation.yaml, whose mutation is global to the variation, when built and run, then it returns solutions")
  void givenMoeadPermutationSpaceWhenBuiltAndRunThenItReturnsSolutions() throws IOException {
    // Arrange
    var moead =
        new PermutationMOEAD(
            new KroAB100TSP(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            "resources/weightVectors",
            new YAMLParameterSpace("MOEADPermutation.yaml", new PermutationParameterFactory()));
    moead.parse(
        """
        --neighborhoodSize 5 --maximumNumberOfReplacedSolutions 2
        --aggregationFunction penaltyBoundaryIntersection --normalizeObjectives false --pbiTheta 5.0
        --algorithmResult population --createInitialSolutions default
        --subProblemIdGenerator randomPermutationCycle --variation crossoverAndMutationVariation
        --crossover PMX --crossoverProbability 0.9 --mutation swap --mutationProbability 0.08
        --selection populationAndNeighborhoodMatingPoolSelection
        --neighborhoodSelectionProbability 0.9
        """
            .split("\\s+"));

    // Act
    var algorithm = moead.build();
    algorithm.run();

    // Assert
    assertReturnsSolutions(algorithm.result());
  }

  @Test
  @DisplayName("Given SMSEMOABinary.yaml, when built and run, then it returns solutions")
  void givenSmsemoaBinarySpaceWhenBuiltAndRunThenItReturnsSolutions() {
    // Arrange
    var smsemoa =
        new BinarySMSEMOA(
            new OneZeroMax(64),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            new YAMLParameterSpace("SMSEMOABinary.yaml", new BinaryParameterFactory()));
    smsemoa.parse(
        """
        --algorithmResult population --createInitialSolutions default
        --variation crossoverAndMutationVariation --crossover singlePoint
        --crossoverProbability 0.9 --mutation bitFlip --mutationProbabilityFactor 1.0
        --gaSelection random
        """
            .split("\\s+"));

    // Act
    var algorithm = smsemoa.build();
    algorithm.run();

    // Assert
    assertReturnsSolutions(algorithm.result());
  }

  @Test
  @DisplayName("Given SMSEMOAPermutation.yaml, when built and run, then it returns solutions")
  void givenSmsemoaPermutationSpaceWhenBuiltAndRunThenItReturnsSolutions() throws IOException {
    // Arrange
    var smsemoa =
        new PermutationSMSEMOA(
            new KroAB100TSP(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            new YAMLParameterSpace("SMSEMOAPermutation.yaml", new PermutationParameterFactory()));
    smsemoa.parse(
        """
        --algorithmResult population --createInitialSolutions default
        --variation crossoverAndMutationVariation --crossover PMX --crossoverProbability 0.9
        --mutation swap --mutationProbability 0.08 --gaSelection random
        """
            .split("\\s+"));

    // Act
    var algorithm = smsemoa.build();
    algorithm.run();

    // Assert
    assertReturnsSolutions(algorithm.result());
  }
}
