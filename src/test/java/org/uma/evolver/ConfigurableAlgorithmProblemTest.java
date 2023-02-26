package org.uma.evolver;

class ConfigurableAlgorithmProblemTest {
  /*
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

    var algorithm = new ConfigurableAlgorithmProblem(new ZDT1(), 100, 10000) ;
    algorithm.parse((configurableParameters).split("\\s+"));

    List<QualityIndicator> indicators = List.of(new PISAHypervolume()) ;

    var problem = new ConfigurableNSGAIIProblem(new ZDT1(), "resources/ZDT1.csv",
        indicators, 100, 1000) ;

    assertEquals(20, problem.numberOfVariables()) ;
    assertEquals(1, problem.numberOfObjectives()) ;
    assertEquals(0, problem.numberOfConstraints()) ;
  }

   */
}