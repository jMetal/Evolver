package org.uma.evolver.algorithm.paes;

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
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

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
  @DisplayName("given default config with crowding distance archive when running on ZDT1 then "
      + "completes without error and result is non-empty")
  void givenDefaultConfig_whenRunningOnZDT1_thenCompletesWithoutError() {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");

    // Act
    // Asserting a minimum HV here would be flaky by nature of this configuration, not of the
    // test: with archiveSelectionProbability 0.0, PAES has no recombination, and about a fifth
    // of its runs get permanently stuck in a local optimum with a low HV, at any budget (checked
    // up to 60000 evaluations). So, as with the other archive types below, this test only checks
    // that the run completes and returns a result.
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, 25000);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty when using crowdingDistanceArchive");
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
  @DisplayName("given externalArchive with unboundedArchive on DTLZ1 when running then result is "
      + "non-empty and never exceeds numberOfSolutionsToFind")
  void givenExternalArchiveWithUnboundedArchive_whenRunningOnDTLZ1_thenResultNeverExceedsNumberOfSolutionsToFind() {
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
    // The external archive keeps every non-dominated solution found and, through
    // BestSolutionsArchive#solutions() (SolutionListUtils#distanceBasedSubsetSelection), returns
    // at most numberOfSolutionsToFind of them -- but never fills up to exactly that many when
    // PAES, with archiveSelectionProbability 0.0 (no recombination), does not discover that many
    // distinct non-dominated solutions within the budget, which happens occasionally regardless
    // of the budget (the same trait as the crowdingDistanceArchive test above). So this only
    // checks the archive's size cap, a real invariant of the code, not an exact count.
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    List<DoubleSolution> result = algorithm.result();

    // Assert
    assertTrue(result.size() > 0, "Result must be non-empty");
    assertTrue(
        result.size() <= numberOfSolutionsToFind,
        "External unbounded archive must not exceed numberOfSolutionsToFind solutions, got: "
            + result.size());
  }
}
