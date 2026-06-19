package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;

@DisplayName("Unit tests for class BinaryPAES")
class PAESBinaryTest {

  private BinaryPAES paes;

  @BeforeEach
  void setUp() {
    paes =
        new BinaryPAES(
            new OneZeroMax(32),
            100,
            20000,
            new YAMLParameterSpace("PAESBinary.yaml", new BinaryParameterFactory()));
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
          + "--mutation bitFlip "
          + "--mutationProbabilityFactor 1.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("crowdingDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals("paesArchive", paes.parameterSpace().get("algorithmResult").value());
      assertEquals(0.0, paes.parameterSpace().get("archiveSelectionProbability").value());
      assertEquals("bitFlip", paes.parameterSpace().get("mutation").value());
    }

    @Test
    @DisplayName("given default config when build is called then algorithm is not null")
    void givenDefaultConfig_whenBuildIsCalled_thenAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation bitFlip "
          + "--mutationProbabilityFactor 1.0").split("\\s+");
      paes.parse(args);

      // Act
      var algorithm = paes.build();

      // Assert
      assertNotNull(algorithm);
    }

    @Test
    @DisplayName("given knnDistanceArchive config when parsing then archive type and k are set")
    void givenKnnDistanceArchiveConfig_whenParsing_thenArchiveTypeAndKAreSet() {
      // Arrange
      String[] args = ("--paesArchiveType knnDistanceArchive "
          + "--knnDistanceArchiveK 3 "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation bitFlip "
          + "--mutationProbabilityFactor 1.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("knnDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals(3, paes.parameterSpace().get("paesArchiveType")
          .findConditionalParameter("knnDistanceArchiveK").value());
    }
  }
}
