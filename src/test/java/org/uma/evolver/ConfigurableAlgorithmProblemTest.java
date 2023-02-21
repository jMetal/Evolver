package org.uma.evolver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableNSGAIIProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

class ConfigurableAlgorithmProblemTest {
  @Test
  void theConstructorCreatesAValidInstanceUsingAQualityIndicator() {
    String configurableParameters =
        ("--algorithmResult population "
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
            + "--polynomialMutationDistributionIndex 20.0 ");

    var nonConfigurableParameterString = new StringBuilder() ;
    nonConfigurableParameterString.append("--maximumNumberOfEvaluations 10000 " ) ;
    nonConfigurableParameterString.append("--populationSize 100 " ) ;

    var algorithm = new ConfigurableNSGAII(new ZDT1()) ;
    algorithm.parse((configurableParameters+nonConfigurableParameterString).split("\\s+"));

    List<QualityIndicator> indicators = List.of(new PISAHypervolume()) ;

    var problem = new ConfigurableNSGAIIProblem(new ZDT1(), "resources/ZDT1.csv",
        indicators, nonConfigurableParameterString) ;

    assertEquals(20, problem.numberOfVariables()) ;
    assertEquals(1, problem.numberOfObjectives()) ;
    assertEquals(0, problem.numberOfConstraints()) ;
  }
}