package org.uma.evolver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.EvolverAutoNSGAII;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

class ConfigurableAlgorithmProblemTest {
  @Test
  void theConstructorCreatesAValidInstanceUsingAQualityIndicator() {
    String[] parameters =
        ("--maximumNumberOfEvaluations 25000 "
            + "--algorithmResult population "
            + "--populationSize 100 "
            + "--offspringPopulationSize 100 "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--selection tournament "
            + "--selectionTournamentSize 2 "
            + "--rankingForSelection dominanceRanking "
            + "--densityEstimatorForSelection crowdingDistance "
            + "--crossover SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy bounds "
            + "--sbxDistributionIndex 20.0 "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex 20.0 ")
            .split("\\s+");

    var algorithm = new EvolverAutoNSGAII(new ZDT1(), "ZDT1.csv") ;
    algorithm.parse(parameters);

    List<QualityIndicator> indicators = List.of(new PISAHypervolume()) ;

    var problem = new ConfigurableNSGAIIProblem(indicators) ;

    assertEquals(21, problem.numberOfVariables()) ;
    assertEquals(1, problem.numberOfObjectives()) ;
    assertEquals(0, problem.numberOfConstraints()) ;
  }
}