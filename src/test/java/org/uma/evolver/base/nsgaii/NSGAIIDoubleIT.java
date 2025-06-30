package org.uma.evolver.base.nsgaii;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.SolutionListUtils;

@DisplayName("Integration tests for class NSGAIIDouble")
class NSGAIIDoubleIT {

  /**
   * Integration test for NSGAIIDouble solving the ZDT1 problem with standard settings.
   *
   * <p>This test configures NSGA-II with a population of 100 and 20,000 evaluations, using SBX
   * crossover and polynomial mutation. It checks that the resulting population achieves a minimum
   * hypervolume of 0.62 when compared to the ZDT1 reference front.
   *
   * <p>The test ensures that the algorithm produces a high-quality Pareto front approximation under
   * typical settings.
   */
  @Tag("integration")
  @Test
  @DisplayName("NSGAIIDouble should reach a minimum hypervolume on ZDT1 with standard settings")
  void shouldTheHypervolumeHaveAMinimumValueWhenSolvingProblemZDT1UsingStandardSettings()
      throws IOException {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    var nsgaII = new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations);

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

    var algorithm = nsgaII.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};

    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);

    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    double expectedHypervolume = 0.62;
    assertTrue(hv > expectedHypervolume);
  }

  /**
   * Integration test for NSGAIIDouble solving the ZDT4 problem using an external archive.
   *
   * <p>This test configures NSGA-II with a population of 100, 25,000 evaluations, and an external
   * crowding distance archive. It uses SBX crossover and polynomial mutation, and checks that the
   * resulting population achieves a minimum hypervolume of 0.64 when compared to the ZDT4 reference
   * front.
   *
   * <p>The test ensures that the algorithm can effectively use an external archive to maintain
   * solution diversity and quality.
   */
  @Tag("integration")
  @Test
  @DisplayName("NSGAIIDouble should reach a minimum hypervolume on ZDT4 using an external archive")
  void shouldTheHypervolumeHaveAMinimumValueWhenSolvingProblemZDT4UsingAnExternalArchive() {
    var problem = new ZDT4();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var nsgaII = new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations);

    var parameters =
        ("--algorithmResult externalArchive "
                + "--externalArchive crowdingDistanceArchive "
                + "--populationSizeWithArchive 20 "
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

    var algorithm = nsgaII.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};

    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);

    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    double expectedHypervolume = 0.64;
    assertTrue(hv > expectedHypervolume);
  }

  /**
   * Integration test for NSGAIIDouble solving the ZDT4 problem using a steady-state scheme.
   *
   * <p>This test configures NSGA-II with a population of 100, 25,000 evaluations, and an offspring
   * population size of 1 (steady-state). It uses SBX crossover and polynomial mutation, and checks
   * that the resulting population achieves a minimum hypervolume of 0.64 when compared to a simple
   * ZDT4 reference front.
   *
   * <p>The test ensures that the steady-state configuration produces a high-quality Pareto front.
   */
  @Tag("integration")
  @Test
  @DisplayName(
      "NSGAIIDouble should reach a minimum hypervolume on ZDT4 using a steady-state scheme (offspring population size 1)")
  void shouldTheHypervolumeHaveAMinimumValueWhenSolvingProblemZDT4UsingASteadyStateScheme() {
    var problem = new ZDT4();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var nsgaII = new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations);

    var parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 1 "
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

    var algorithm = nsgaII.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};

    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);

    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));

    double expectedHypervolume = 0.64;
    assertTrue(hv > expectedHypervolume);
  }

  /**
   * Integration test for NSGAIIDouble solving the DTLZ2 problem using an external unbounded
   * archive.
   *
   * <p>This test configures NSGA-II with a population of 100, 40,000 evaluations, and an external
   * unbounded archive. It uses SBX crossover and polynomial mutation, and checks that the resulting
   * population achieves a minimum hypervolume of 0.40 when compared to a simple DTLZ2 reference
   * front.
   *
   * <p>The test ensures that the algorithm can effectively use an unbounded archive to maintain
   * solution diversity and quality in a three-objective scenario.
   */
  @Tag("integration")
  @Test
  @DisplayName(
      "NSGAIIDouble should reach a minimum hypervolume on DTLZ2 using an external unbounded archive")
  void
      shouldTheHypervolumeHaveAMinimumValueWhenSolvingProblemDTLZ2UsingAnExternalUnboundedArchive() {
    var problem = new DTLZ2();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    var nsgaII = new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations);

    var parameters =
        ("--algorithmResult externalArchive "
                + "--archiveType unboundedArchive "
                + "--populationSizeWithArchive 20 "
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

    var algorithm = nsgaII.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 0.0, 1.0}, {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}};

    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);

    double[][] normalizedFront =
        NormalizeUtils.normalize(
            SolutionListUtils.getMatrixWithObjectiveValues(population),
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    double hv = hypervolume.compute(normalizedFront);

    double expectedHypervolume = 0.40;
    assertTrue(hv > expectedHypervolume);
  }
}
