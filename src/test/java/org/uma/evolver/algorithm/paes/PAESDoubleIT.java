package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;

@DisplayName("Integration tests for class DoublePAES")
class PAESDoubleIT {

  private EvolutionaryAlgorithm<DoubleSolution> buildAndRun(String[] args, int maxEvals) {
    var paes =
        new DoublePAES(
            new ZDT1(),
            100,
            maxEvals,
            new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    return algorithm;
  }

  @Test
  @Tag("integration")
  @DisplayName("given default config with crowding distance archive when running on ZDT1 then HV exceeds threshold")
  void givenDefaultConfig_whenRunningOnZDT1_thenHVExceedsThreshold() {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");
    int maxEvals = 25000;

    // Act
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, maxEvals);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    double[][] referenceFront = {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(result));
    assertTrue(hv > 0.55,
        "Expected HV > 0.55 but got: " + hv);
  }

  @Test
  @Tag("integration")
  @DisplayName("given spatialSpreadDeviationArchive config when running on ZDT1 then completes without error and result is non-empty")
  void givenSpatialSpreadDeviationArchive_whenRunningOnZDT1_thenCompletesWithoutError() {
    // Arrange
    String[] args = ("--paesArchiveType spatialSpreadDeviationArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

    // Act
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, 10000);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty when using spatialSpreadDeviationArchive");
  }

  @Test
  @Tag("integration")
  @DisplayName("given knnDistanceArchive config when running on ZDT1 then completes without error and result is non-empty")
  void givenKnnDistanceArchive_whenRunningOnZDT1_thenCompletesWithoutError() {
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
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, 10000);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty when using knnDistanceArchive");
  }

  @Test
  @Tag("integration")
  @DisplayName("given angleArchive config when running on ZDT1 then completes without error and result is non-empty")
  void givenAngleArchive_whenRunningOnZDT1_thenCompletesWithoutError() {
    // Arrange
    String[] args = ("--paesArchiveType angleArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

    // Act
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, 10000);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty when using angleArchive");
  }

  @Test
  @Tag("integration")
  @DisplayName("given externalArchive with unboundedArchive on DTLZ1 when running then result has exactly numberOfSolutionsToFind solutions")
  void givenExternalArchiveWithUnboundedArchive_whenRunningOnDTLZ1_thenResultHasExactlyNumberOfSolutionsToFindSolutions() {
    // Arrange
    int numberOfSolutionsToFind = 100;
    var paes = new DoublePAES(
        new DTLZ1(),
        numberOfSolutionsToFind,
        25000,
        new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));

    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult externalArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

    // Act
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertEquals(numberOfSolutionsToFind, result.size(),
        "External unbounded archive must return exactly numberOfSolutionsToFind solutions via distance-based subset selection");
  }
}
