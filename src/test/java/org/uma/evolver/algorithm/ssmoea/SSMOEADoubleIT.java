package org.uma.evolver.algorithm.ssmoea;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.*;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;

@DisplayName("Integration tests for class SSMOEADouble")
class SSMOEADoubleIT {

  /**
   * Integration test for DoubleSSMOEA solving ZDT1 with the crossover+mutation variation branch
   * and ranking+density-estimator replacement (DEMO-style steady-state NSGA-II).
   *
   * <p>Uses SBX crossover, tournament selection, polynomial mutation, and
   * rankingAndDensityEstimator replacement. Checks that the hypervolume exceeds 0.55 after 25000
   * evaluations.
   */
  @Tag("integration")
  @Test
  @DisplayName("SSMOEADouble should reach a minimum hypervolume on ZDT1 using crossover+mutation variation")
  void shouldReachMinimumHypervolumeOnZDT1WithCrossoverAndMutationVariation() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var ssmoea = new DoubleSSMOEA(
        problem, populationSize, maximumNumberOfEvaluations,
        new YAMLParameterSpace("SSMOEADouble.yaml", new DoubleParameterFactory()));

    var parameters = (
        "--algorithmResult population "
        + "--createInitialSolutions default "
        + "--ranking dominanceRanking "
        + "--densityEstimator crowdingDistance "
        + "--variation crossoverAndMutationVariation "
        + "--crossover SBX "
        + "--crossoverProbability 0.9 "
        + "--crossoverRepairStrategy bounds "
        + "--sbxDistributionIndex 20.0 "
        + "--gaSelection tournament "
        + "--selectionTournamentSize 2 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0 "
        + "--replacement rankingAndDensityEstimator"
    ).split("\\s+");

    var algorithm = ssmoea.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    double expectedHypervolume = 0.55;
    assertTrue(hv > expectedHypervolume,
        "Expected HV > " + expectedHypervolume + " but got " + hv);
  }

  /**
   * Integration test for DoubleSSMOEA solving ZDT1 with the differential evolution variation branch
   * and single-solution replacement (DEMO-style one-to-one comparison).
   *
   * <p>Uses RAND_1_BIN crossover (CR=0.5, F=0.5) with polynomial mutation and singleSolutionReplacement.
   * Checks that the hypervolume exceeds 0.40 after 50000 evaluations.
   */
  @Tag("integration")
  @Test
  @DisplayName("SSMOEADouble should reach a minimum hypervolume on ZDT1 using differential evolution variation")
  void shouldReachMinimumHypervolumeOnZDT1WithDifferentialEvolutionVariation() {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 50000;

    var ssmoea = new DoubleSSMOEA(
        problem, populationSize, maximumNumberOfEvaluations,
        new YAMLParameterSpace("SSMOEADouble.yaml", new DoubleParameterFactory()));

    var parameters = (
        "--algorithmResult population "
        + "--createInitialSolutions default "
        + "--ranking dominanceRanking "
        + "--densityEstimator crowdingDistance "
        + "--variation differentialEvolutionVariation "
        + "--differentialEvolutionCrossover RAND_1_BIN "
        + "--CR 0.5 "
        + "--F 0.5 "
        + "--sequenceGenerator randomPermutationCycle "
        + "--takeCurrentSolutionAsParent true "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0 "
        + "--replacement singleSolutionReplacement"
    ).split("\\s+");

    var algorithm = ssmoea.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    double expectedHypervolume = 0.40;
    assertTrue(hv > expectedHypervolume,
        "Expected HV > " + expectedHypervolume + " but got " + hv);
  }
}
