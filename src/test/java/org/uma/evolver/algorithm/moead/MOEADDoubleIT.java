package org.uma.evolver.algorithm.moead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;

/**
 * Integration tests for {@link DoubleMOEAD} covering the aggregation functions added in jMetal
 * 7.4: augmentedTschebyscheff and invertedPenaltyBoundaryIntersection (IPBI), and differential
 * evolution variants (RAND_1_BIN, RAND_1_EXP, RAND_2_BIN, RAND_2_EXP).
 *
 * <p>IPBI requires the nadir point estimation, which MOEADReplacement only maintains when
 * objective normalization is enabled. These tests verify that IPBI configurations run correctly
 * even when "normalizeObjectives" is false, because normalization is enforced for that function.
 *
 * <p>DE variant tests verify that all RAND-family DE crossover operators work correctly in the
 * MOEA/D framework without producing invalid objectives.
 */
@DisplayName("Integration tests for class DoubleMOEAD")
class MOEADDoubleIT {

  private static final int POPULATION_SIZE = 100;
  private static final int MAX_EVALUATIONS = 25000;
  private static final String WEIGHT_VECTOR_FILES_DIRECTORY = "resources/weightVectors";

  private List<DoubleSolution> runMoeadOnZdt1(String aggregationSettings) {
    var moead =
        new DoubleMOEAD(
            new ZDT1(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            WEIGHT_VECTOR_FILES_DIRECTORY,
            new YAMLParameterSpace("MOEADDouble.yaml", new DoubleParameterFactory()));

    var parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + aggregationSettings
                + " --algorithmResult population "
                + "--createInitialSolutions default "
                + "--subProblemIdGenerator randomPermutationCycle "
                + "--variation crossoverAndMutationVariation "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var algorithm = moead.parse(parameters).build();
    algorithm.run();
    return algorithm.result();
  }

  private double hypervolumeOnZdt1(List<DoubleSolution> population) {
    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    return hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));
  }

  private List<DoubleSolution> runMoeadWithExternalArchiveOnZdt1(String archiveSettings) {
    var moead =
        new DoubleMOEAD(
            new ZDT1(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            WEIGHT_VECTOR_FILES_DIRECTORY,
            new YAMLParameterSpace("MOEADDouble.yaml", new DoubleParameterFactory()));

    var parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction tschebyscheff "
                + "--normalizeObjectives false "
                + "--algorithmResult externalArchive "
                + archiveSettings
                + " --createInitialSolutions default "
                + "--subProblemIdGenerator randomPermutationCycle "
                + "--variation crossoverAndMutationVariation "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var algorithm = moead.parse(parameters).build();
    algorithm.run();
    return algorithm.result();
  }

  private List<DoubleSolution> runMoeadWithDeVariantOnZdt1(String deVariant) {
    var moead =
        new DoubleMOEAD(
            new ZDT1(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            WEIGHT_VECTOR_FILES_DIRECTORY,
            new YAMLParameterSpace("MOEADDouble.yaml", new DoubleParameterFactory()));

    var parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction tschebyscheff "
                + "--normalizeObjectives false "
                + "--algorithmResult population "
                + "--createInitialSolutions default "
                + "--subProblemIdGenerator randomPermutationCycle "
                + "--variation differentialEvolutionVariation "
                + "--differentialEvolutionCrossover "
                + deVariant
                + " "
                + "--CR 0.5 "
                + "--F 0.5 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var algorithm = moead.parse(parameters).build();
    algorithm.run();
    return algorithm.result();
  }

  @Nested
  @DisplayName("When running MOEA/D with the augmentedTschebyscheff aggregation function")
  class AugmentedTschebyscheffCases {

    @Tag("integration")
    @Test
    @DisplayName(
        "given the ZDT1 problem, when running with augmentedTschebyscheff, then a minimum"
            + " hypervolume is reached")
    void givenZdt1_whenRunningWithAugmentedTschebyscheff_thenAMinimumHypervolumeIsReached() {
      // Arrange
      String aggregationSettings =
          "--aggregationFunction augmentedTschebyscheff --normalizeObjectives false";

      // Act
      List<DoubleSolution> population = runMoeadOnZdt1(aggregationSettings);

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(hypervolumeOnZdt1(population) > 0.6);
    }
  }

  @Nested
  @DisplayName(
      "When running MOEA/D with the invertedPenaltyBoundaryIntersection aggregation function")
  class InvertedPenaltyBoundaryIntersectionCases {

    @Tag("integration")
    @Test
    @DisplayName(
        "given normalization enabled, when running with IPBI, then a minimum hypervolume is"
            + " reached")
    void givenNormalizationEnabled_whenRunningWithIpbi_thenAMinimumHypervolumeIsReached() {
      // Arrange
      String aggregationSettings =
          "--aggregationFunction invertedPenaltyBoundaryIntersection --ipbiTheta 0.1 "
              + "--normalizeObjectives true --epsilonParameterForNormalization 0.0000001";

      // Act
      List<DoubleSolution> population = runMoeadOnZdt1(aggregationSettings);

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(hypervolumeOnZdt1(population) > 0.5);
    }

    @Tag("integration")
    @Test
    @DisplayName(
        "given normalization disabled, when running with IPBI, then the run completes because"
            + " normalization is enforced")
    void givenNormalizationDisabled_whenRunningWithIpbi_thenTheRunCompletes() {
      // Arrange: without the enforced normalization, this configuration would fail with a
      // NullPointerException because the nadir point would never be created
      String aggregationSettings =
          "--aggregationFunction invertedPenaltyBoundaryIntersection --ipbiTheta 0.1 "
              + "--normalizeObjectives false";

      // Act
      List<DoubleSolution> population = runMoeadOnZdt1(aggregationSettings);

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(
          population.stream()
              .allMatch(
                  solution ->
                      Double.isFinite(solution.objectives()[0])
                          && Double.isFinite(solution.objectives()[1])));
    }
  }

  @Nested
  @DisplayName("When running MOEA/D with differential evolution RAND variants")
  class DifferentialEvolutionRandVariantsCases {

    @Tag("integration")
    @Test
    @DisplayName("given RAND_1_BIN variant, when running on ZDT1, then objectives are valid")
    void givenRand1Bin_whenRunningOnZdt1_thenObjectivesAreValid() {
      // Act
      List<DoubleSolution> population = runMoeadWithDeVariantOnZdt1("RAND_1_BIN");

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(
          population.stream()
              .allMatch(
                  solution ->
                      Double.isFinite(solution.objectives()[0])
                          && Double.isFinite(solution.objectives()[1])));
    }

    @Tag("integration")
    @Test
    @DisplayName("given RAND_1_EXP variant, when running on ZDT1, then objectives are valid")
    void givenRand1Exp_whenRunningOnZdt1_thenObjectivesAreValid() {
      // Act
      List<DoubleSolution> population = runMoeadWithDeVariantOnZdt1("RAND_1_EXP");

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(
          population.stream()
              .allMatch(
                  solution ->
                      Double.isFinite(solution.objectives()[0])
                          && Double.isFinite(solution.objectives()[1])));
    }

    @Tag("integration")
    @Test
    @DisplayName("given RAND_2_BIN variant, when running on ZDT1, then objectives are valid")
    void givenRand2Bin_whenRunningOnZdt1_thenObjectivesAreValid() {
      // Act
      List<DoubleSolution> population = runMoeadWithDeVariantOnZdt1("RAND_2_BIN");

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(
          population.stream()
              .allMatch(
                  solution ->
                      Double.isFinite(solution.objectives()[0])
                          && Double.isFinite(solution.objectives()[1])));
    }

    @Tag("integration")
    @Test
    @DisplayName("given RAND_2_EXP variant, when running on ZDT1, then objectives are valid")
    void givenRand2Exp_whenRunningOnZdt1_thenObjectivesAreValid() {
      // Act
      List<DoubleSolution> population = runMoeadWithDeVariantOnZdt1("RAND_2_EXP");

      // Assert
      assertEquals(POPULATION_SIZE, population.size());
      assertTrue(
          population.stream()
              .allMatch(
                  solution ->
                      Double.isFinite(solution.objectives()[0])
                          && Double.isFinite(solution.objectives()[1])));
    }
  }

  @Nested
  @DisplayName("When running MOEA/D with alternative external archive types")
  class AlternativeExternalArchiveCases {

    @Tag("integration")
    @Test
    @DisplayName("given spatialSpreadDeviationArchive, when running on ZDT1, then result is non-empty")
    void givenSpatialSpreadDeviationArchive_whenRunningOnZdt1_thenResultIsNonEmpty() {
      // Act
      List<DoubleSolution> result =
          runMoeadWithExternalArchiveOnZdt1("--archiveType spatialSpreadDeviationArchive");

      // Assert
      assertTrue(result.size() > 0, "Result must be non-empty when using spatialSpreadDeviationArchive");
    }

    @Tag("integration")
    @Test
    @DisplayName("given knnDistanceArchive, when running on ZDT1, then result is non-empty")
    void givenKnnDistanceArchive_whenRunningOnZdt1_thenResultIsNonEmpty() {
      // Act
      List<DoubleSolution> result =
          runMoeadWithExternalArchiveOnZdt1(
              "--archiveType knnDistanceArchive --knnDistanceArchiveK 5");

      // Assert
      assertTrue(result.size() > 0, "Result must be non-empty when using knnDistanceArchive");
    }

    @Tag("integration")
    @Test
    @DisplayName("given angleArchive, when running on ZDT1, then result is non-empty")
    void givenAngleArchive_whenRunningOnZdt1_thenResultIsNonEmpty() {
      // Act
      List<DoubleSolution> result = runMoeadWithExternalArchiveOnZdt1("--archiveType angleArchive");

      // Assert
      assertTrue(result.size() > 0, "Result must be non-empty when using angleArchive");
    }
  }
}
