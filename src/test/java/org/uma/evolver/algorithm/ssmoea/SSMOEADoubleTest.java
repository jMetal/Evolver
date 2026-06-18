package org.uma.evolver.algorithm.ssmoea;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class SSMOEADouble")
class SSMOEADoubleTest {

  private DoubleSSMOEA ssmoeaDouble;

  @BeforeEach
  void setup() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    ssmoeaDouble = new DoubleSSMOEA(
        problem, populationSize, maximumNumberOfEvaluations,
        new YAMLParameterSpace("SSMOEADouble.yaml", new DoubleParameterFactory()));
  }

  @Nested
  @DisplayName("When the class constructor is called")
  class ConstructorTestCases {

    @Test
    @DisplayName("The total number of parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfParametersIsCorrect() {
      int numberOfFlattenedParameters = 43;
      assertEquals(
          numberOfFlattenedParameters,
          ssmoeaDouble.parameterSpace().parameters().size());
    }

    @Test
    @DisplayName("The number of top-level parameters is correct")
    void whenCreatingAnInstanceTheTotalNumberOfTopLevelParametersIsCorrect() {
      int numberOfTopLevelParameters = 6;
      assertEquals(
          numberOfTopLevelParameters,
          ssmoeaDouble.parameterSpace().topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When calling the parse() method")
  class ParseUnitTests {

    @Test
    @DisplayName("Parameters are parsed correctly for the crossover+mutation branch")
    void shouldParseWorkProperlyWithCrossoverAndMutationVariation() {
      var parameters = (
          "--algorithmResult population "
          + "--createInitialSolutions default "
          + "--ranking dominanceRanking "
          + "--densityEstimator crowdingDistance "
          + "--variation crossoverAndMutationVariation "
          + "--crossover SBX "
          + "--crossoverProbability 0.9 "
          + "--crossoverRepairStrategy bounds "
          + "--sbxDistributionIndex 20.0 "
          + "--gaSelection tournament "
          + "--selectionTournamentSize 2 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0 "
          + "--replacement rankingAndDensityEstimator"
      ).split("\\s+");

      ssmoeaDouble.parse(parameters);
      var ps = ssmoeaDouble.parameterSpace();

      assertEquals("population", ps.get("algorithmResult").value());
      assertEquals("crossoverAndMutationVariation", ps.get("variation").value());
      assertEquals("SBX", ps.get("crossover").value());
      assertEquals(0.9, ps.get("crossoverProbability").value());
      assertEquals(20.0, ps.get("sbxDistributionIndex").value());
      assertEquals("tournament", ps.get("gaSelection").value());
      assertEquals(2, ps.get("selectionTournamentSize").value());
      assertEquals("polynomial", ps.get("mutation").value());
      assertEquals(1.0, ps.get("mutationProbabilityFactor").value());
      assertEquals(20.0, ps.get("polynomialMutationDistributionIndex").value());
      assertEquals("rankingAndDensityEstimator", ps.get("replacement").value());
    }

    @Test
    @DisplayName("Parameters are parsed correctly for the differential evolution branch")
    void shouldParseWorkProperlyWithDifferentialEvolutionVariation() {
      var parameters = (
          "--algorithmResult population "
          + "--createInitialSolutions default "
          + "--ranking dominanceRanking "
          + "--densityEstimator crowdingDistance "
          + "--variation differentialEvolutionVariation "
          + "--differentialEvolutionCrossover RAND_1_BIN "
          + "--CR 0.5 "
          + "--F 0.5 "
          + "--sequenceGenerator randomPermutationCycle "
          + "--takeCurrentSolutionAsParent true "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0 "
          + "--replacement singleSolutionReplacement"
      ).split("\\s+");

      ssmoeaDouble.parse(parameters);
      var ps = ssmoeaDouble.parameterSpace();

      assertEquals("differentialEvolutionVariation", ps.get("variation").value());
      assertEquals("RAND_1_BIN", ps.get("differentialEvolutionCrossover").value());
      assertEquals(0.5, ps.get("CR").value());
      assertEquals(0.5, ps.get("F").value());
      assertEquals("randomPermutationCycle", ps.get("sequenceGenerator").value());
      assertEquals("true", ps.get("takeCurrentSolutionAsParent").value());
      assertEquals("polynomial", ps.get("mutation").value());
      assertEquals("singleSolutionReplacement", ps.get("replacement").value());
    }
  }
}
