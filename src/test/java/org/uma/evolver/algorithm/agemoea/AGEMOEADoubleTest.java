package org.uma.evolver.algorithm.agemoea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class DoubleAGEMOEA")
class AGEMOEADoubleTest {

  private DoubleAGEMOEA agemoeaDouble;

  @BeforeEach
  void setup() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    agemoeaDouble =
        new DoubleAGEMOEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace("AGEMOEADouble.yaml", new DoubleParameterFactory()));
  }

  @Nested
  @DisplayName("When the class constructor is called: ")
  class ConstructorTestCases {

    @Test
    @DisplayName("The total number of parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfParametersIsCorrect() {
      int numberOfFlattenedParameters = 33;
      assertEquals(
          numberOfFlattenedParameters,
          agemoeaDouble.parameterSpace().parameters().size());
    }

    @Test
    @DisplayName("The number of top-level parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfTopLevelParametersIsCorrect() {
      int numberOfTopLevelParameters = 6;
      assertEquals(
          numberOfTopLevelParameters,
          agemoeaDouble.parameterSpace().topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method")
  class ParseUnitTests {
    @Test
    @DisplayName("The parameters are parsed correctly with the default AGE-MOEA-II configuration")
    void shouldParseWorkProperlyWithDefaultAgeMoea2Settings() {
      var parameters =
          ("--agemoeaVariant agemoea2 "
                  + "--algorithmResult population "
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

      agemoeaDouble.parse(parameters);
      var parameterSpace = agemoeaDouble.parameterSpace();

      assertEquals(
          "agemoea2",
          parameterSpace.get("agemoeaVariant").value(),
          "AGE-MOEA variant should be 'agemoea2'");

      assertEquals(
          "population",
          parameterSpace.get("algorithmResult").value(),
          "Algorithm result should be 'population'");

      assertEquals(
          "SBX",
          parameterSpace.get("crossover").value(),
          "Crossover method should be 'SBX'");

      assertEquals(
          0.9,
          parameterSpace.get("crossoverProbability").value(),
          "Crossover probability should be 0.9");

      assertEquals(
          "polynomial",
          parameterSpace.get("mutation").value(),
          "Mutation method should be 'polynomial'");

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
    @DisplayName("The parameters are parsed correctly with the original AGE-MOEA variant")
    void shouldParseWorkProperlyWithOriginalAgeMoeaVariant() {
      var parameters =
          ("--agemoeaVariant agemoea "
                  + "--algorithmResult population "
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

      agemoeaDouble.parse(parameters);
      var parameterSpace = agemoeaDouble.parameterSpace();

      assertEquals(
          "agemoea",
          parameterSpace.get("agemoeaVariant").value(),
          "AGE-MOEA variant should be 'agemoea'");
    }

    @Test
    @DisplayName(
        "The parameters are parsed correctly with a configuration including an external archive")
    void shouldParseWorkProperlyWithAConfigurationIncludingAnExternalArchive() {
      var parameters =
          ("--agemoeaVariant agemoea2 "
                  + "--algorithmResult externalArchive "
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

      agemoeaDouble.parse(parameters);
      var parameterSpace = agemoeaDouble.parameterSpace();

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
