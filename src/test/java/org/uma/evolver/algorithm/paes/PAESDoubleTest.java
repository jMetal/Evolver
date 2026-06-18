package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

@DisplayName("Unit tests for class DoublePAES")
class PAESDoubleTest {

  private DoublePAES paes;

  @BeforeEach
  void setUp() {
    paes =
        new DoublePAES(
            new ZDT1(),
            100,
            20000,
            new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));
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
      assertEquals(14, totalParameters);
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
    @DisplayName("given default config when parsing then paesArchiveType is crowdingDistanceArchive")
    void givenDefaultConfig_whenParsing_thenPaesArchiveTypeIsCrowdingDistance() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("crowdingDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals("paesArchive", paes.parameterSpace().get("algorithmResult").value());
      assertEquals(0.0, paes.parameterSpace().get("archiveSelectionProbability").value());
      assertEquals("polynomial", paes.parameterSpace().get("mutation").value());
    }

    @Test
    @DisplayName("given non-zero archive selection probability when parsing then probability value is set")
    void givenNonZeroArchiveSelectionProbability_whenParsing_thenProbabilityIsSet() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.5 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("crowdingDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals(0.5, paes.parameterSpace().get("archiveSelectionProbability").value());
    }

    @Test
    @DisplayName("given build is called with default config then algorithm is not null")
    void givenDefaultConfig_whenBuildIsCalled_thenAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");
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
          + "--knnDistanceArchiveK 5 "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("knnDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals(5, paes.parameterSpace().get("paesArchiveType")
          .findConditionalParameter("knnDistanceArchiveK").value());
    }

    @Test
    @DisplayName("given angleArchive config when parsing then archive type is angleArchive")
    void givenAngleArchiveConfig_whenParsing_thenArchiveTypeIsAngleArchive() {
      // Arrange
      String[] args = ("--paesArchiveType angleArchive "
          + "--algorithmResult paesArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("angleArchive", paes.parameterSpace().get("paesArchiveType").value());
    }

    @Test
    @DisplayName("given externalArchive config when building then algorithm is not null")
    void givenExternalArchiveConfig_whenBuilding_thenAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive "
          + "--algorithmResult externalArchive "
          + "--archiveSelectionProbability 0.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");
      paes.parse(args);

      // Act
      var algorithm = paes.build();

      // Assert
      assertEquals("externalArchive", paes.parameterSpace().get("algorithmResult").value());
      assertNotNull(algorithm);
    }
  }
}
