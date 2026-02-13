package org.uma.evolver.base.moead;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.base.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.base.moead.PermutationMOEAD;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADPermutationParameterSpace;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

import java.io.IOException;

class MOEADTest {

  @Nested
  @DisplayName("When the class constructor is called: ")
  class ConstructorTestCases {
    private DoubleMOEAD moeadDouble;

    @BeforeEach
    void setup() {
      ZDT1 problem = new ZDT1();
      int populationSize = 100;
      int maximumNumberOfEvaluations = 25000;
      String weightVectorFilesDirectory = "resources/weightVectors";

      moeadDouble =
          new DoubleMOEAD(
              problem,
              populationSize,
              maximumNumberOfEvaluations,
              weightVectorFilesDirectory,
              new MOEADDoubleParameterSpace());
    }

    @Test
    @DisplayName("The total number of parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfParametersIsCorrect() {
      int numberOfFlattenedParameters = 35;
      assertEquals(
          numberOfFlattenedParameters, moeadDouble.parameterSpace().parameters().size());
    }

    @Test
    @DisplayName("The number of top-level parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfTopLevelParametersIsCorrect() {
      int numberOfTopLevelParameters = 8;
      assertEquals(
          numberOfTopLevelParameters, moeadDouble.parameterSpace().topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method on standard MOEA/D")
  class MOEADParseUnitTests {
    @Test
    @DisplayName(
        "The parameters are parsed correctly with the default settings for DoubleSolution problems")
    void shouldParseWorkProperlyWithTheDefaultSettingsOnMOEAD() {
      String[] parameters =
          ("--neighborhoodSize 20 "
                  + "--maximumNumberOfReplacedSolutions 2 "
                  + "--aggregationFunction penaltyBoundaryIntersection "
                  + "--normalizeObjectives true "
                  + "--epsilonParameterForNormalization 4 "
                  + "--pbiTheta 5.0 "
                  + "--algorithmResult population "
                  + "--createInitialSolutions default "
                  + "--subProblemIdGenerator randomPermutationCycle "
                  + "--variation crossoverAndMutationVariation "
                  + "--crossoverProbability 0.9 "
                  + "--crossoverRepairStrategy bounds "
                  + "--mutation polynomial "
                  + "--mutationProbabilityFactor 1.0 "
                  + "--mutationRepairStrategy bounds "
                  + "--polynomialMutationDistributionIndex 20.0 "
                  + "--crossover SBX "
                  + "--sbxDistributionIndex 20.0 "
                  + "--selection populationAndNeighborhoodMatingPoolSelection "
                  + "--neighborhoodSelectionProbability 0.9")
              .split("\\s+");

      ZDT1 problem = new ZDT1();
      int populationSize = 100;
      int maximumNumberOfEvaluations = 25000;
      String weightVectorFilesDirectory = "resources/weightVectors";

      var moeadDouble =
          new DoubleMOEAD(
              problem,
              populationSize,
              maximumNumberOfEvaluations,
              weightVectorFilesDirectory,
              new MOEADDoubleParameterSpace());

      moeadDouble.parse(parameters);
      var parameterSpace = moeadDouble.parameterSpace();

      // Validate parsed parameters
      assertEquals(20, parameterSpace.get("neighborhoodSize").value());
      assertEquals(2, parameterSpace.get("maximumNumberOfReplacedSolutions").value());
      assertEquals(
          "penaltyBoundaryIntersection", parameterSpace.get("aggregationFunction").value());
      assertEquals("true", parameterSpace.get("normalizeObjectives").value());
      assertEquals(5.0, parameterSpace.get("pbiTheta").value());
      assertEquals("population", parameterSpace.get("algorithmResult").value());
      assertEquals("default", parameterSpace.get("createInitialSolutions").value());
      assertEquals("randomPermutationCycle", parameterSpace.get("subProblemIdGenerator").value());
      assertEquals("crossoverAndMutationVariation", parameterSpace.get("variation").value());
      assertEquals(0.9, parameterSpace.get("crossoverProbability").value());
      assertEquals("bounds", parameterSpace.get("crossoverRepairStrategy").value());
      assertEquals("polynomial", parameterSpace.get("mutation").value());
      assertEquals(1.0, parameterSpace.get("mutationProbabilityFactor").value());
      assertEquals("bounds", parameterSpace.get("mutationRepairStrategy").value());
      assertEquals(20.0, parameterSpace.get("polynomialMutationDistributionIndex").value());
      assertEquals("SBX", parameterSpace.get("crossover").value());
      assertEquals(20.0, parameterSpace.get("sbxDistributionIndex").value());
      assertEquals(
          "populationAndNeighborhoodMatingPoolSelection", parameterSpace.get("selection").value());
      assertEquals(0.9, parameterSpace.get("neighborhoodSelectionProbability").value());
    }

    @Test
    @DisplayName(
        "The parameters are parsed correctly with the default settings for PermutationSolution<Integer> problems")
    void shouldParseWorkProperlyWithTheDefaultSettingsOnMOEADPermutation() throws IOException {
      String[] parameters =
          ("--neighborhoodSize 20 "
                  + "--maximumNumberOfReplacedSolutions 2 "
                  + "--aggregationFunction penaltyBoundaryIntersection "
                  + "--normalizeObjectives true "
                  + "--epsilonParameterForNormalization 4 "
                  + "--pbiTheta 5.0 "
                  + "--algorithmResult population "
                  + "--createInitialSolutions default "
                  + "--subProblemIdGenerator randomPermutationCycle "
                  + "--variation crossoverAndMutationVariation "
                  + "--crossoverProbability 0.9 "
                  + "--crossoverRepairStrategy bounds "
                  + "--mutation swap "
                  + "--mutationProbability 0.06 "
                  + "--mutationRepairStrategy bounds "
                  + "--crossover PMX "
                  + "--selection populationAndNeighborhoodMatingPoolSelection "
                  + "--neighborhoodSelectionProbability 0.9")
              .split("\\s+");

      var moead =
          new PermutationMOEAD(
                  new KroAB100TSP(),
                  100,
                  1000000,
                  "resources/weightVectors",
                  new MOEADPermutationParameterSpace())
              .parse(parameters);
      var parameterSpace = moead.parameterSpace();

      // Validate parsed parameters
      assertEquals(20, parameterSpace.get("neighborhoodSize").value());
      assertEquals(2, parameterSpace.get("maximumNumberOfReplacedSolutions").value());
      assertEquals(
          "penaltyBoundaryIntersection", parameterSpace.get("aggregationFunction").value());
      assertEquals("true", parameterSpace.get("normalizeObjectives").value());
      assertEquals(5.0, parameterSpace.get("pbiTheta").value());
      assertEquals("population", parameterSpace.get("algorithmResult").value());
      assertEquals("default", parameterSpace.get("createInitialSolutions").value());
      assertEquals("randomPermutationCycle", parameterSpace.get("subProblemIdGenerator").value());
      assertEquals("crossoverAndMutationVariation", parameterSpace.get("variation").value());
      assertEquals(0.9, parameterSpace.get("crossoverProbability").value());
      assertEquals("swap", parameterSpace.get("mutation").value());
      assertEquals(0.06, parameterSpace.get("mutationProbability").value());
      assertEquals("PMX", parameterSpace.get("crossover").value());
      assertEquals(
          "populationAndNeighborhoodMatingPoolSelection", parameterSpace.get("selection").value());
      assertEquals(0.9, parameterSpace.get("neighborhoodSelectionProbability").value());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method on MOEA/D-DE")
  class MOEADDEParseUnitTests {
    @Test
    @DisplayName("The parameters are parsed correctly with the default settings")
    void shouldParseWorkProperlyWithTheDefaultSettingsOnMOEADDE() {
      String[] parameters =
          ("--neighborhoodSize 20 "
                  + "--maximumNumberOfReplacedSolutions 2 "
                  + "--aggregationFunction tschebyscheff "
                  + "--normalizeObjectives false "
                  + "--algorithmResult population "
                  + "--createInitialSolutions default "
                  + "--variation differentialEvolutionVariation "
                  + "--subProblemIdGenerator randomPermutationCycle "
                  + "--mutation polynomial "
                  + "--mutationProbabilityFactor 1.0 "
                  + "--mutationRepairStrategy bounds "
                  + "--polynomialMutationDistributionIndex 20.0 "
                  + "--differentialEvolutionCrossover RAND_1_BIN "
                  + "--CR 1.0 "
                  + "--F 0.5 "
                  + "--selection populationAndNeighborhoodMatingPoolSelection "
                  + "--neighborhoodSelectionProbability 0.9 ")
              .split("\\s+");

      LZ09F2 problem = new LZ09F2();
      int populationSize = 100;
      int maximumNumberOfEvaluations = 25000;
      String weightVectorFilesDirectory = "resources/weightVectors";

      var moeadde =
          new DoubleMOEAD(
              problem,
              populationSize,
              maximumNumberOfEvaluations,
              weightVectorFilesDirectory,
              new MOEADDoubleParameterSpace());

      moeadde.parse(parameters);
      var parameterSpace = moeadde.parameterSpace();
      // Validate parsed parameters
      assertEquals(20, parameterSpace.get("neighborhoodSize").value());
      assertEquals(2, parameterSpace.get("maximumNumberOfReplacedSolutions").value());
      assertEquals("tschebyscheff", parameterSpace.get("aggregationFunction").value());
      assertEquals("false", parameterSpace.get("normalizeObjectives").value());
      assertEquals("population", parameterSpace.get("algorithmResult").value());
      assertEquals("default", parameterSpace.get("createInitialSolutions").value());
      assertEquals("differentialEvolutionVariation", parameterSpace.get("variation").value());
      assertEquals("randomPermutationCycle", parameterSpace.get("subProblemIdGenerator").value());
      assertEquals("polynomial", parameterSpace.get("mutation").value());
      assertEquals(1.0, parameterSpace.get("mutationProbabilityFactor").value());
      assertEquals("bounds", parameterSpace.get("mutationRepairStrategy").value());
      assertEquals(20.0, parameterSpace.get("polynomialMutationDistributionIndex").value());
      assertEquals("RAND_1_BIN", parameterSpace.get("differentialEvolutionCrossover").value());
      assertEquals(1.0, parameterSpace.get("CR").value());
      assertEquals(0.5, parameterSpace.get("F").value());
      assertEquals(
          "populationAndNeighborhoodMatingPoolSelection", parameterSpace.get("selection").value());
      assertEquals(0.9, parameterSpace.get("neighborhoodSelectionProbability").value());
    }
  }
}
