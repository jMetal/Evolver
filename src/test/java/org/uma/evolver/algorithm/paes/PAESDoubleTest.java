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
      assertEquals(16, totalParameters);
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
      String[] args = ("--paesArchiveType crowdingDistanceArchive --paesArchiveSize 100 "
          + "--algorithmResult paesArchive "
          + "--createInitialSolutions default "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("crowdingDistanceArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals(100, paes.parameterSpace().get("paesArchiveSize").value());
      assertEquals("paesArchive", paes.parameterSpace().get("algorithmResult").value());
      assertEquals("polynomial", paes.parameterSpace().get("mutation").value());
    }

    @Test
    @DisplayName("given hypervolumeArchive config when parsing then paesArchiveType and archive size are set")
    void givenHypervolumeArchiveConfig_whenParsing_thenArchiveTypeAndSizeAreSet() {
      // Arrange
      String[] args = ("--paesArchiveType hypervolumeArchive --paesArchiveSize 50 "
          + "--algorithmResult paesArchive "
          + "--createInitialSolutions default "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

      // Act
      paes.parse(args);

      // Assert
      assertEquals("hypervolumeArchive", paes.parameterSpace().get("paesArchiveType").value());
      assertEquals(50, paes.parameterSpace().get("paesArchiveSize").value());
    }

    @Test
    @DisplayName("given build is called with default config then algorithm is not null")
    void givenDefaultConfig_whenBuildIsCalled_thenAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive --paesArchiveSize 100 "
          + "--algorithmResult paesArchive "
          + "--createInitialSolutions default "
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
    @DisplayName("given externalArchive config when building then external archive size is parsed and algorithm is not null")
    void givenExternalArchiveConfig_whenBuilding_thenArchiveSizeIsParsedAndAlgorithmIsNotNull() {
      // Arrange
      String[] args = ("--paesArchiveType crowdingDistanceArchive --paesArchiveSize 100 "
          + "--algorithmResult externalArchive "
          + "--externalArchiveSize 50 "
          + "--archiveType crowdingDistanceArchive "
          + "--createInitialSolutions default "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0").split("\\s+");
      paes.parse(args);

      // Act
      var algorithm = paes.build();

      // Assert
      assertEquals("externalArchive", paes.parameterSpace().get("algorithmResult").value());
      assertEquals(50, paes.parameterSpace().get("externalArchiveSize").value());
      assertNotNull(algorithm);
    }
  }
}
