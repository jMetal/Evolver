package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

@DisplayName("Integration tests for class PermutationPAES")
class PAESPermutationIT {

  private EvolutionaryAlgorithm<PermutationSolution<Integer>> buildAndRun(
      String[] args, int maxEvals) throws IOException {
    var paes =
        new PermutationPAES(
            new KroAB100TSP(),
            100,
            maxEvals,
            new YAMLParameterSpace("PAESPermutation.yaml", new PermutationParameterFactory()));
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    return algorithm;
  }

  @Test
  @Tag("integration")
  @DisplayName("given swap mutation with paesArchive when running on KroAB100TSP then result is non-empty")
  void givenSwapMutation_whenRunningOnKroAB100TSP_thenResultIsNonEmpty() throws IOException {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation swap "
        + "--mutationProbability 0.01").split("\\s+");

    // Act
    var result = buildAndRun(args, 10000).result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty");
  }

  @Test
  @Tag("integration")
  @DisplayName("given inversion mutation with externalArchive when running on KroAB100TSP then result is non-empty")
  void givenInversionMutation_whenRunningOnKroAB100TSP_thenResultIsNonEmpty() throws IOException {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult externalArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation inversion "
        + "--mutationProbability 0.01").split("\\s+");

    // Act
    var result = buildAndRun(args, 10000).result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty with externalArchive");
  }
}
