package org.uma.evolver.algorithm;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {
    String referenceFrontFileName = "resources/DTLZ1.3D.csv";

    String[] parameters =
        ("--maximumNumberOfEvaluations 50000 --populationSize 100 --algorithmResult externalArchive --populationSizeWithArchive 26 --externalArchive unboundedArchive --createInitialSolutions random --variation crossoverAndMutationVariation --offspringPopulationSize 8 --crossover SBX --crossoverProbability 0.9706583539471686 --crossoverRepairStrategy round --sbxDistributionIndex 55.88884235643158 --blxAlphaCrossoverAlphaValue 0.40176046031508056 --mutation uniform --mutationProbabilityFactor 0.7801233826975796 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 265.91145898670146 --linkedPolynomialMutationDistributionIndex 46.634657224307226 --uniformMutationPerturbation 0.2696672750092429 --nonUniformMutationPerturbation 0.480109066822935 --selection random --selectionTournamentSize 8 \n ")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new DTLZ1());
    autoNSGAII.parse(parameters);

    AutoNSGAII.print(autoNSGAII.fixedParameterList());
    AutoNSGAII.print(autoNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.create();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 100,
            referenceFrontFileName, "F1", "F2");

    nsgaII.getObservable().register(evaluationObserver);
    nsgaII.getObservable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.getTotalComputingTime()); ;

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
