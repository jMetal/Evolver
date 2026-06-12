package org.uma.evolver.algorithm.nsgaiii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.SolutionListUtils;

/**
 * Integration tests for {@link DoubleNSGAIII} running the algorithm end to end with the default
 * configuration on bi-objective (ZDT1) and three-objective (DTLZ2) problems, the latter
 * exercising the reference-point-based niching with a Das-Dennis lattice.
 */
@DisplayName("Integration tests for class DoubleNSGAIII")
class NSGAIIIDoubleIT {

  private static final int POPULATION_SIZE = 100;

  private List<DoubleSolution> runNsgaiii(Problem<DoubleSolution> problem, int maxEvaluations) {
    var nsgaiii =
        new DoubleNSGAIII(
            problem,
            POPULATION_SIZE,
            maxEvaluations,
            new YAMLParameterSpace("NSGAIIIDouble.yaml", new DoubleParameterFactory()));

    var parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 100 "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split("\\s+");

    var algorithm = nsgaiii.parse(parameters).build();
    algorithm.run();
    return algorithm.result();
  }

  @Tag("integration")
  @Test
  @DisplayName("given the ZDT1 problem, when running with the default configuration, then a minimum hypervolume is reached")
  void givenZdt1_whenRunningWithTheDefaultConfiguration_thenAMinimumHypervolumeIsReached() {
    // Arrange
    var problem = new ZDT1();

    // Act
    List<DoubleSolution> population = runNsgaiii(problem, 20000);

    // Assert
    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    assertEquals(POPULATION_SIZE, population.size());
    assertTrue(hv > 0.62, "Hypervolume on ZDT1 should be greater than 0.62 but was " + hv);
  }

  @Tag("integration")
  @Test
  @DisplayName("given the three-objective DTLZ2 problem, when running with the default configuration, then a minimum hypervolume is reached")
  void givenDtlz2_whenRunningWithTheDefaultConfiguration_thenAMinimumHypervolumeIsReached() {
    // Arrange
    var problem = new DTLZ2();

    // Act
    List<DoubleSolution> population = runNsgaiii(problem, 40000);

    // Assert
    double[][] referenceFront = new double[][] {{0.0, 0.0, 1.0}, {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double[][] normalizedFront =
        NormalizeUtils.normalize(
            SolutionListUtils.getMatrixWithObjectiveValues(population),
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));
    double hv = hypervolume.compute(normalizedFront);

    assertTrue(hv > 0.40, "Hypervolume on DTLZ2 should be greater than 0.40 but was " + hv);
  }
}
