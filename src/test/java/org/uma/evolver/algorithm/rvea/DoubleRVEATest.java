package org.uma.evolver.algorithm.rvea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.multiobjective.RVEABuilder;
import org.uma.jmetal.component.algorithm.multiobjective.RVEAStarBuilder;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.referencepoint.ReferencePointGenerator;

@DisplayName("Unit tests for class DoubleRVEA")
class DoubleRVEATest {

  private static final String OPERATORS =
      "--algorithmResult population --createInitialSolutions default "
          + "--variation crossoverAndMutationVariation --offspringPopulationSize 100 "
          + "--crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds "
          + "--sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 "
          + "--selection random --alpha 2.0 --fr 0.1 ";
  private static final String IRVEA_PARAMETERS =
      "--numberOfSubregions 40 --lateStageFraction 0.8 --epsilonKappa 0.05";

  private static ParameterSpace parameterSpace() {
    return new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory());
  }

  /** RVEA on three-objective DTLZ2 with 15 reference vectors and a small budget. */
  private static DoubleRVEA smallRVEA(String configuration) {
    List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(3, 4);
    var rvea = new DoubleRVEA(new DTLZ2(), vectors.size(), 600, parameterSpace(), vectors);
    rvea.parse(configuration.trim().split("\\s+"));
    return rvea;
  }

  @Nested
  @DisplayName("When building a variant of the RVEA family")
  class VariantTests {

    @ParameterizedTest(name = "{0} is built as {1}")
    @CsvSource({"rvea, RVEA", "rveaStar, RVEA*", "iRVEA, iRVEA"})
    @DisplayName("each value of the replacement builds its variant, which runs")
    void givenReplacementValue_whenBuilt_thenTheVariantRunsAndReturnsAFront(
        String replacement, String expectedName) {
      // Arrange
      var rvea = smallRVEA(OPERATORS + "--replacement " + replacement + " " + IRVEA_PARAMETERS);

      // Act
      Algorithm<List<DoubleSolution>> algorithm = rvea.build();
      algorithm.run();

      // Assert
      assertEquals(expectedName, algorithm.name());
      assertFalse(algorithm.result().isEmpty());
    }

    @Test
    @DisplayName("iRVEA without its specific parameters is rejected")
    void givenIRVEAWithoutItsParameters_whenParsed_thenItFails() {
      // Arrange
      List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(3, 4);
      var rvea = new DoubleRVEA(new DTLZ2(), vectors.size(), 600, parameterSpace(), vectors);
      String[] configuration = (OPERATORS + "--replacement iRVEA").trim().split("\\s+");

      // Act & Assert
      assertThrows(RuntimeException.class, () -> rvea.parse(configuration));
    }
  }

  @Nested
  @DisplayName("When using the default configurations")
  class DefaultConfigurationTests {

    @ParameterizedTest(name = "{0} builds {1}")
    @CsvSource({
      "RVEADoubleDefault.txt, RVEA",
      "RVEAStarDoubleDefault.txt, RVEA*",
      "IRVEADoubleDefault.txt, iRVEA"
    })
    @DisplayName("each default configuration file builds its variant")
    void givenDefaultConfigurationFile_whenBuilt_thenItsVariantIsBuilt(
        String fileName, String expectedName) throws IOException {
      // Arrange
      String configuration =
          new ConfigurationFileReader("defaultConfigurations/" + fileName).getConfiguration(1);

      // Act
      Algorithm<List<DoubleSolution>> algorithm = smallRVEA(configuration).build();

      // Assert
      assertEquals(expectedName, algorithm.name());
    }
  }

  @Nested
  @DisplayName("When checking the problem and the reference vectors")
  class ValidationTests {

    @Test
    @DisplayName("a constrained problem is rejected")
    void givenConstrainedProblem_whenBuilt_thenItFails() {
      // Arrange
      List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(2, 9);
      var rvea = new DoubleRVEA(new Srinivas(), vectors.size(), 600, parameterSpace(), vectors);
      rvea.parse((OPERATORS + "--replacement rvea").split("\\s+"));

      // Act & Assert
      assertThrows(RuntimeException.class, rvea::build);
    }

    @Test
    @DisplayName("a population size different from the number of reference vectors is rejected")
    void givenPopulationSizeDifferentFromTheVectors_whenCreated_thenItFails() {
      // Arrange
      List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(3, 4);

      // Act & Assert
      assertThrows(
          RuntimeException.class,
          () -> new DoubleRVEA(new DTLZ2(), vectors.size() + 1, 600, parameterSpace(), vectors));
    }

    @Test
    @DisplayName("the weight vector file of each problem is read from the directory")
    void givenWeightVectorDirectory_whenInstancesAreCreated_thenEachProblemGetsItsVectors() {
      // Arrange
      var rvea = new DoubleRVEA(100, "resources/weightVectors", parameterSpace());
      String[] configuration = (OPERATORS + "--replacement rvea").split("\\s+");

      // Act & Assert: two and three objectives, W2D_100.dat and W3D_100.dat
      for (var problem : List.of(new ZDT1(), new DTLZ2())) {
        var instance = rvea.createInstance(problem, 1000);
        instance.parse(configuration);
        Algorithm<List<DoubleSolution>> algorithm = instance.build();
        algorithm.run();
        assertFalse(algorithm.result().isEmpty());
      }
    }

    @Test
    @DisplayName("a missing weight vector file is reported")
    void givenMissingWeightVectorFile_whenBuilt_thenItFails() {
      // Arrange: there is no file of 7 vectors
      var rvea = new DoubleRVEA(new ZDT1(), 7, 1000, "resources/weightVectors", parameterSpace());
      rvea.parse((OPERATORS + "--replacement rvea").split("\\s+"));

      // Act & Assert
      assertThrows(RuntimeException.class, rvea::build);
    }
  }

  @Nested
  @DisplayName("When selecting the mating pool")
  class SelectionTests {

    @Test
    @DisplayName("tournament selection falls back to random selection on a small population")
    void givenPopulationSmallerThanTheTournament_whenSelecting_thenRandomSelectionIsUsed() {
      // Arrange
      var rvea =
          smallRVEA(
              OPERATORS.replace("--selection random", "--selection tournament --selectionTournamentSize 5")
                  + "--replacement rvea");
      var problem = new DTLZ2();
      List<DoubleSolution> population = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        population.add(problem.evaluate(problem.createSolution()));
      }

      // Act
      List<DoubleSolution> matingPool = rvea.createSelection(10).select(population);

      // Assert
      assertEquals(10, matingPool.size());
    }
  }

  @Nested
  @DisplayName("When compared with jMetal's builders")
  class EquivalenceTests {

    private List<DoubleSolution> runDoubleRVEA(String replacement, List<double[]> vectors) {
      JMetalRandom.getInstance().setSeed(1);
      var rvea = new DoubleRVEA(new ZDT1(), vectors.size(), 5000, parameterSpace(), vectors);
      rvea.parse((OPERATORS + "--replacement " + replacement).split("\\s+"));
      Algorithm<List<DoubleSolution>> algorithm = rvea.build();
      algorithm.run();
      return algorithm.result();
    }

    private static void assertSameFront(List<DoubleSolution> expected, List<DoubleSolution> actual) {
      assertEquals(expected.size(), actual.size());
      for (int i = 0; i < expected.size(); i++) {
        for (int j = 0; j < expected.get(i).objectives().length; j++) {
          assertEquals(expected.get(i).objectives()[j], actual.get(i).objectives()[j], 1e-12);
        }
      }
    }

    @Test
    @DisplayName("RVEA gives the same front as RVEABuilder with the same seed")
    void givenStandardRVEA_whenRunWithTheSameSeed_thenTheFrontIsThatOfRVEABuilder() {
      // Arrange
      List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(2, 99);
      var problem = new ZDT1();
      JMetalRandom.getInstance().setSeed(1);
      var reference =
          new RVEABuilder<>(
                  problem, vectors.size(), 5000, new SBXCrossover(0.9, 20.0),
                  new PolynomialMutation(1.0 / problem.numberOfVariables(), 20.0), 2.0, 0.1,
                  vectors)
              .build();
      reference.run();

      // Act
      List<DoubleSolution> front = runDoubleRVEA("rvea", vectors);

      // Assert
      assertSameFront(reference.result(), front);
    }

    @Test
    @DisplayName("RVEA* gives the same front as RVEAStarBuilder with the same seed")
    void givenRVEAStar_whenRunWithTheSameSeed_thenTheFrontIsThatOfRVEAStarBuilder() {
      // Arrange
      List<double[]> vectors = ReferencePointGenerator.generateSingleLayer(2, 99);
      var problem = new ZDT1();
      JMetalRandom.getInstance().setSeed(1);
      var reference =
          new RVEAStarBuilder<>(
                  problem, vectors.size(), 5000, new SBXCrossover(0.9, 20.0),
                  new PolynomialMutation(1.0 / problem.numberOfVariables(), 20.0), 2.0, 0.1,
                  vectors)
              .build();
      reference.run();

      // Act
      List<DoubleSolution> front = runDoubleRVEA("rveaStar", vectors);

      // Assert
      assertSameFront(reference.result(), front);
    }
  }
}
