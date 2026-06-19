package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

@DisplayName("Integration tests for class BinaryPAES")
class PAESBinaryIT {

  private EvolutionaryAlgorithm<BinarySolution> buildAndRun(String[] args, int maxEvals) {
    var paes =
        new BinaryPAES(
            new OneZeroMax(512),
            100,
            maxEvals,
            new YAMLParameterSpace("PAESBinary.yaml", new BinaryParameterFactory()));
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    return algorithm;
  }

  @Test
  @Tag("integration")
  @DisplayName("given default bitFlip config when running on OneZeroMax then result is non-empty")
  void givenDefaultBitFlipConfig_whenRunningOnOneZeroMax_thenResultIsNonEmpty() {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation bitFlip "
        + "--mutationProbabilityFactor 1.0").split("\\s+");

    // Act
    var result = buildAndRun(args, 10000).result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty");
  }

  @Test
  @Tag("integration")
  @DisplayName("given externalArchive config when running on OneZeroMax then result is non-empty")
  void givenExternalArchiveConfig_whenRunningOnOneZeroMax_thenResultIsNonEmpty() {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult externalArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation bitFlip "
        + "--mutationProbabilityFactor 1.0").split("\\s+");

    // Act
    var result = buildAndRun(args, 10000).result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty with externalArchive");
  }
}
