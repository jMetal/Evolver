package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
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
    String referenceFrontFileName = "resources/ZDT1.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 26 --externalArchive unboundedArchive --createInitialSolutions random --variation crossoverAndMutationVariation --offspringPopulationSize 8 --crossover SBX --crossoverProbability 0.9706583539471686 --crossoverRepairStrategy round --sbxDistributionIndex 55.88884235643158 --blxAlphaCrossoverAlphaValue 0.40176046031508056 --mutation uniform --mutationProbabilityFactor 0.7801233826975796 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 265.91145898670146 --linkedPolynomialMutationDistributionIndex 46.634657224307226 --uniformMutationPerturbation 0.2696672750092429 --nonUniformMutationPerturbation 0.480109066822935 --selection random --selectionTournamentSize 8 \n ")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new ZDT1(), 100, 10000);
    autoNSGAII.parse(parameters);

    AutoNSGAII.print(autoNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.create();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 100,
            referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
