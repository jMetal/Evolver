package org.uma.evolver.base.nsgaii;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class NSGAIIDouble")
class NSGAIIDoubleTest {

  private NSGAIIDouble nsgaIIDouble;

  @BeforeEach
  void setup() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    nsgaIIDouble =
        new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations, new NSGAIIDoubleParameterSpace());
  }

  @Nested
  @DisplayName("When the class constructor is called: ")
  class ConstructorTestCases {

    @Test
    @DisplayName("The total number of parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfParametersIsCorrect() {
      int numberOfFlattenedParameters = 29;
      assertEquals(
          numberOfFlattenedParameters,
          nsgaIIDouble.parameterSpace().parameters().size());
    }

    @Test
    @DisplayName("The number of top-level parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfTopLevelParametersIsCorrect() {
      int numberOfTopLevelParameters = 5;
      assertEquals(
          numberOfTopLevelParameters,
          nsgaIIDouble.parameterSpace().topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method")
  class ParseUnitTests {
    @Test
    @DisplayName("The parameters are parsed correctly with the default settings")
    void shouldParseWorkProperlyWithTheDefaultSettings() {
      var parameters =
          ("--algorithmResult externalArchive "
                  + "--populationSizeWithArchive 20 "
                  + "--archiveType unboundedArchive "
                  + "--createInitialSolutions default "
                  + "--variation crossoverAndMutationVariation "
                  + "--offspringPopulationSize 100 "
                  + "--crossover SBX "
                  + "--crossoverProbability 0.9 "
                  + "--crossoverRepairStrategy bounds "
                  + "--sbxDistributionIndex 20.0 "
                  + "--mutation polynomial --mutationProbabilityFactor 1.0 "
                  + "--mutationRepairStrategy bounds "
                  + "--polynomialMutationDistributionIndex 20.0 "
                  + "--selection tournament "
                  + "--selectionTournamentSize 2")
              .split("\\s+");

      nsgaIIDouble.parse(parameters);
      var parameterSpace =
          (NSGAIIDoubleParameterSpace) nsgaIIDouble.parameterSpace();

      // Validate parsed parameters
      assertEquals(
          20,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.POPULATION_SIZE_WITH_ARCHIVE)
              .value(),
          "Population size with archive should be 20");

      assertEquals(
          "unboundedArchive",
          nsgaIIDouble.parameterSpace().get(parameterSpace.ARCHIVE_TYPE).value(),
          "External archive should be 'unboundedArchive'");

      assertEquals(
          "default",
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.CREATE_INITIAL_SOLUTIONS)
              .value(),
          "Initial solutions creation method should be 'random'");

      assertEquals(
          "SBX",
          nsgaIIDouble.parameterSpace().get(parameterSpace.CROSSOVER).value(),
          "Crossover method should be 'SBX'");

      assertEquals(
          0.9,
          nsgaIIDouble.parameterSpace().get(parameterSpace.CROSSOVER_PROBABILITY).value(),
          "Crossover probability should be 0.9");

      assertEquals(
          20.0,
          nsgaIIDouble.parameterSpace().get(parameterSpace.SBX_DISTRIBUTION_INDEX).value(),
          "SBX distribution index should be 20.0");

      assertEquals(
          "polynomial",
          nsgaIIDouble.parameterSpace().get(parameterSpace.MUTATION).value(),
          "Mutation method should be 'polynomial'");

      assertEquals(
          1.0,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.MUTATION_PROBABILITY_FACTOR)
              .value(),
          "Mutation probability factor should be 1.0");

      assertEquals(
          20.0,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX)
              .value(),
          "Polynomial mutation distribution index should be 20.0");

      assertEquals(
          "tournament",
          nsgaIIDouble.parameterSpace().get(parameterSpace.SELECTION).value(),
          "Selection method should be 'tournament'");

      assertEquals(
          2,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.SELECTION_TOURNAMENT_SIZE)
              .value(),
          "Selection tournament size should be 2");
    }

    @Test
    @DisplayName(
        "The parameters are parsed correctly with a configuration including an external archive")
    void shouldParseWorkProperlyWithAConfigurationIncludingAnExternalArchive() {
      var parameters =
          ("--algorithmResult externalArchive "
                  + "--archiveType unboundedArchive "
                  + "--populationSizeWithArchive 20 "
                  + "--createInitialSolutions default "
                  + "--variation crossoverAndMutationVariation "
                  + "--offspringPopulationSize 100 "
                  + "--crossover SBX "
                  + "--crossoverProbability 0.9 "
                  + "--crossoverRepairStrategy bounds "
                  + "--sbxDistributionIndex 20.0 "
                  + "--mutation polynomial "
                  + "--mutationProbabilityFactor 1.0 "
                  + "--mutationRepairStrategy bounds "
                  + "--polynomialMutationDistributionIndex 20.0 "
                  + "--selection tournament "
                  + "--selectionTournamentSize 2")
              .split("\\s+");

      nsgaIIDouble.parse(parameters);
      var parameterSpace =
              (NSGAIIDoubleParameterSpace) nsgaIIDouble.parameterSpace();

      assertEquals(
          "externalArchive",
          nsgaIIDouble.parameterSpace().get(parameterSpace.ALGORITHM_RESULT).value(),
          "Algorithm result should be 'externalArchive'");

      assertEquals(
          20,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.POPULATION_SIZE_WITH_ARCHIVE)
              .value(),
          "Population size with archive should be 20");

      assertEquals(
          "unboundedArchive",
          nsgaIIDouble.parameterSpace().get(parameterSpace.ARCHIVE_TYPE).value(),
          "External archive should be 'unboundedArchive'");

      assertEquals(
          "crossoverAndMutationVariation",
          nsgaIIDouble.parameterSpace().get(parameterSpace.VARIATION).value(),
          "Variation should be 'crossoverAndMutationVariation'");

      assertEquals(
          100,
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.OFFSPRING_POPULATION_SIZE)
              .value(),
          "Offspring population size should be 100");

      assertEquals(
          "bounds",
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.CROSSOVER_REPAIR_STRATEGY)
              .value(),
          "Crossover repair strategy should be 'bounds'");

      assertEquals(
          "bounds",
          nsgaIIDouble
              .parameterSpace()
              .get(parameterSpace.MUTATION_REPAIR_STRATEGY)
              .value(),
          "Mutation repair strategy should be 'bounds'");
    }
  }
}
