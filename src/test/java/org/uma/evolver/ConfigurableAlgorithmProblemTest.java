package org.uma.evolver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

class ConfigurableAlgorithmProblemTest {
  @Test
  void theConstructorCreatesAValidInstanceUsingAQualityIndicator() {
    String[] parameters =
        ("--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT1 "
            + "--randomGeneratorSeed 12 "
            + "--referenceFrontFileName ZDT1.csv "
            + "--maximumNumberOfEvaluations 25000 "
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

    AutoConfigurableAlgorithm algorithm = new AutoNSGAII() ;
    algorithm.parseAndCheckParameters(parameters);

    List<QualityIndicator> indicators = List.of(new PISAHypervolume()) ;

    var problem = new ConfigurableAlgorithmProblem(algorithm, indicators) ;

    assertEquals(5, problem.numberOfVariables()) ;
    assertEquals(1, problem.numberOfObjectives()) ;
    assertEquals(0, problem.numberOfConstraints()) ;
  }
}