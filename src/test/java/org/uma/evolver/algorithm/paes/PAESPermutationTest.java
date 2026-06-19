package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;

@DisplayName("Unit tests for class PermutationPAES")
class PAESPermutationTest {

  private PermutationPAES paes;

  @BeforeEach
  void setUp() throws IOException {
    paes =
        new PermutationPAES(
            new KroAB100TSP(),
            100,
            20000,
            new YAMLParameterSpace("PAESPermutation.yaml", new PermutationParameterFactory()));
  }

  @Nested
  @DisplayName("When the class constructor is called")
  class WhenConstructorIsCalled {

    @Test
    @DisplayName("given new instance when getting parameter space then returns expected total parameter count")
    void givenNewInstance_whenGettingParameterSpace_thenReturnsExpectedTotalParameterCount() {
      // Arrange — done in setUp

      // Act
      int totalParameters = paes.parameterSpace().parameters().size();

      // Assert
      assertEquals(6, totalParameters);
    }

    @Test
    @DisplayName("given new instance when getting parameter space then returns 4 top-level parameters")
    void givenNewInstance_whenGettingParameterSpace_thenReturns4TopLevelParameters() {
      // Arrange — done in setUp

      // Act
      int topLevelCount = paes.parameterSpace().topLevelParameters().size();

      // Assert
      assertEquals(4, topLevelCount);
    }
  }

  @Nested
  @DisplayName("When calling the parse() method")
  class WhenCallingParse {

    @Test
    @DisplayName("given default config when parsing then paesArchiveType and mutation are set")
    void givenDefaultConfig_whenParsing_thenPaesArchiveTypeAndMutationAreSet() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation swap "
          + "--mutationProbability 0.01").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("crowdingDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals("paesArchive", paes.parameterSpace().get("algorithmResult").value());
      assertEquals(0.0, paes.parameterSpace().get("archiveSelectionProbability").value());
      assertEquals("swap", paes.parameterSpace().get("mutation").value());
    }

    @Test
    @DisplayName("given default config when build is called then algorithm is not null")
    void givenDefaultConfig_whenBuildIsCalled_thenAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation swap "
          + "--mutationProbability 0.01").split("\\s+");
      paes.parse(args);

      // Act
      var algorithm = paes.build();

      // Assert
      assertNotNull(algorithm);
    }

    @Test
    @DisplayName("given inversion mutation when parsing then mutation is set correctly")
    void givenInversionMutation_whenParsing_thenMutationIsSet() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation inversion "
          + "--mutationProbability 0.01").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("inversion", paes.parameterSpace().get("mutation").value());
    }
  }
}
