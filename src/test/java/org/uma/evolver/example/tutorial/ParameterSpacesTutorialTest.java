package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;

/**
 * Keeps tutorial E1 ("Parameter spaces") from rotting: its page shows the output of {@link
 * ParameterSpacesTutorial}, so these tests check the facts that output relies on.
 */
@DisplayName("Unit tests for class ParameterSpacesTutorial")
class ParameterSpacesTutorialTest {

  private static ParameterSpace nsgaiiDoubleSpace() {
    return new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
  }

  @Nested
  @DisplayName("When describing the structure of a parameter space: ")
  class DescribeTestCases {

    @Test
    @DisplayName(
        "given NSGAIIDouble.yaml, when described, then global sub-parameters are marked with * and"
            + " conditional ones with the value that activates them")
    void givenNSGAIIDoubleSpace_whenDescribed_thenRelationsAreMarked() {
      // Arrange
      ParameterSpace space = nsgaiiDoubleSpace();

      // Act
      String tree = ParameterSpacesTutorial.describe(space);

      // Assert
      assertTrue(tree.contains("    * crossoverProbability (double: [0.0, 1.0])"), tree);
      assertTrue(tree.contains("    [SBX] sbxDistributionIndex (double: [5.0, 400.0])"), tree);
      assertTrue(tree.contains("  [tournament] selectionTournamentSize (integer: [2, 10])"), tree);
    }

    @Test
    @DisplayName(
        "given NSGAIIDouble.yaml, when counting its parameters, then there are more parameters than"
            + " top-level ones")
    void givenNSGAIIDoubleSpace_whenCounted_thenSubParametersAreIncluded() {
      // Arrange
      ParameterSpace space = nsgaiiDoubleSpace();

      // Act
      int total = ParameterSpacesTutorial.numberOfParameters(space);

      // Assert
      assertEquals(5, space.topLevelParameters().size());
      assertTrue(total > space.topLevelParameters().size());
    }
  }

  @Nested
  @DisplayName("When describing the active parameters of a configuration: ")
  class DescribeActiveTestCases {

    @Test
    @DisplayName(
        "given a configuration with SBX, when described, then SBX's conditional parameter is active"
            + " and the other crossovers' are not")
    void givenSBXConfiguration_whenDescribed_thenOnlySBXParametersAreActive() {
      // Arrange
      ParameterSpace space = nsgaiiDoubleSpace();
      String[] configuration =
          ("--algorithmResult population --createInitialSolutions default"
                  + " --offspringPopulationSize 100 --variation crossoverAndMutationVariation"
                  + " --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds"
                  + " --sbxDistributionIndex 20.0 --mutation polynomial"
                  + " --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds"
                  + " --polynomialMutationDistributionIndex 20.0 --selection tournament"
                  + " --selectionTournamentSize 2")
              .split(" ");
      for (Parameter<?> parameter : space.topLevelParameters()) {
        parameter.parse(configuration);
      }

      // Act
      String active = ParameterSpacesTutorial.describeActive(space);

      // Assert
      assertTrue(active.contains("    sbxDistributionIndex = 20.0"), active);
      assertFalse(active.contains("blxAlphaCrossoverAlpha"), active);
      assertFalse(active.contains("populationSizeWithArchive"), active);
    }
  }

  @Nested
  @DisplayName("When running the tutorial: ")
  class MainTestCases {

    @Test
    @DisplayName(
        "given the tutorial, when main is run, then both wrong configurations are rejected with"
            + " the messages the tutorial page shows")
    void givenTutorial_whenMainIsRun_thenWrongConfigurationsAreRejected() {
      // Arrange
      PrintStream standardOutput = System.out;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      System.setOut(new PrintStream(output));

      // Act
      try {
        ParameterSpacesTutorial.main(new String[] {});
      } finally {
        System.setOut(standardOutput);
      }

      // Assert
      String printed = output.toString();
      assertTrue(printed.contains("Rejected: Missing parameter: --sbxDistributionIndex"), printed);
      assertTrue(printed.contains("Rejected: Parameter crossover: Invalid value: HUX"), printed);
      assertTrue(printed.contains("BinaryCrossoverParameter with values [HUX, uniform"), printed);
    }
  }
}
