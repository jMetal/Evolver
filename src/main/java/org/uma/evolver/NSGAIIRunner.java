package org.uma.evolver;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIRunner {
  public static void main(String[] args) {
    String referenceFrontFileName = "ZDT1.csv" ;

    String[] parameters =
        ("--problemName org.uma.evolver.ConfigurableAlgorithmProblem "
            + "--randomGeneratorSeed 12 "
            + "--referenceFrontFileName "+ referenceFrontFileName + " "
                + "--maximumNumberOfEvaluations 5000 "
                + "--algorithmResult population "
                + "--populationSize 50 "
                + "--offspringPopulationSize 50 "
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

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parseAndCheckParameters(parameters);

    AutoNSGAII.print(autoNSGAII.fixedParameterList);
    AutoNSGAII.print(autoNSGAII.autoConfigurableParameterList);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.create() ;
    nsgaII.setEvaluation(new MultiThreadedEvaluation<>(8, nsgaII.getEvaluation().problem()));

    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 1, null);

    nsgaII.getObservable().register(evaluationObserver);
    nsgaII.getObservable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.getTotalComputingTime()); ;

    new SolutionListOutput(nsgaII.getResult())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
