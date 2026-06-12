package org.uma.evolver.algorithm.nsgaiii;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class DoubleNSGAIII")
class NSGAIIIDoubleTest {

  private DoubleNSGAIII nsgaiiiDouble;

  @BeforeEach
  void setUp() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    nsgaiiiDouble =
        new DoubleNSGAIII(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace("NSGAIIIDouble.yaml", new DoubleParameterFactory()));
  }

  @Nested
  @DisplayName("When the class constructor is called")
  class ConstructorTestCases {

    @Test
    @DisplayName("given a valid configuration, when creating an instance, then the total number of parameters is correct")
    void givenAValidConfiguration_whenCreatingAnInstance_thenTheTotalNumberOfParametersIsCorrect() {
      // Arrange
      int numberOfFlattenedParameters = 32;

      // Act & Assert
      assertEquals(
          numberOfFlattenedParameters, nsgaiiiDouble.parameterSpace().parameters().size());
    }

    @Test
    @DisplayName("given a valid configuration, when creating an instance, then the number of top-level parameters is correct")
    void givenAValidConfiguration_whenCreatingAnInstance_thenTheNumberOfTopLevelParametersIsCorrect() {
      // Arrange
      int numberOfTopLevelParameters = 5;

      // Act & Assert
      assertEquals(
          numberOfTopLevelParameters, nsgaiiiDouble.parameterSpace().topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method")
  class ParseUnitTests {

    @Test
    @DisplayName("given the default configuration, when parsing, then the parameters are parsed correctly")
    void givenTheDefaultConfiguration_whenParsing_thenTheParametersAreParsedCorrectly() {
      // Arrange
      var parameters =
          ("--algorithmResult population "
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

      // Act
      nsgaiiiDouble.parse(parameters);
      var parameterSpace = nsgaiiiDouble.parameterSpace();

      // Assert
      assertEquals(
          "population",
          parameterSpace.get("algorithmResult").value(),
          "Algorithm result should be 'population'");
      assertEquals(
          "default",
          parameterSpace.get("createInitialSolutions").value(),
          "Initial solutions strategy should be 'default'");
      assertEquals(
          "crossoverAndMutationVariation",
          parameterSpace.get("variation").value(),
          "Variation should be 'crossoverAndMutationVariation'");
      assertEquals(
          100,
          parameterSpace.get("offspringPopulationSize").value(),
          "Offspring population size should be 100");
      assertEquals(
          "SBX", parameterSpace.get("crossover").value(), "Crossover method should be 'SBX'");
      assertEquals(
          0.9,
          parameterSpace.get("crossoverProbability").value(),
          "Crossover probability should be 0.9");
      assertEquals(
          20.0,
          parameterSpace.get("sbxDistributionIndex").value(),
          "SBX distribution index should be 20.0");
      assertEquals(
          "polynomial",
          parameterSpace.get("mutation").value(),
          "Mutation method should be 'polynomial'");
      assertEquals(
          1.0,
          parameterSpace.get("mutationProbabilityFactor").value(),
          "Mutation probability factor should be 1.0");
      assertEquals(
          "tournament",
          parameterSpace.get("selection").value(),
          "Selection method should be 'tournament'");
      assertEquals(
          2,
          parameterSpace.get("selectionTournamentSize").value(),
          "Selection tournament size should be 2");
    }

    @Test
    @DisplayName("given a configuration with an external archive, when parsing, then the archive parameters are parsed correctly")
    void givenAConfigurationWithAnExternalArchive_whenParsing_thenTheArchiveParametersAreParsedCorrectly() {
      // Arrange
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

      // Act
      nsgaiiiDouble.parse(parameters);
      var parameterSpace = nsgaiiiDouble.parameterSpace();

      // Assert
      assertEquals(
          "externalArchive",
          parameterSpace.get("algorithmResult").value(),
          "Algorithm result should be 'externalArchive'");
      assertEquals(
          20,
          parameterSpace.get("populationSizeWithArchive").value(),
          "Population size with archive should be 20");
      assertEquals(
          "unboundedArchive",
          parameterSpace.get("archiveType").value(),
          "External archive should be 'unboundedArchive'");
    }
  }
}
