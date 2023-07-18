package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;

import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link ConfigurableNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 24 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7706300459118733 --crossoverRepairStrategy bounds --sbxDistributionIndex 222.47374954885674 --blxAlphaCrossoverAlphaValue 0.5971239421519462 --mutation polynomial --mutationProbabilityFactor 0.9429115328245097 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 7.0602623411731695 --linkedPolynomialMutationDistributionIndex 305.5917931481647 --uniformMutationPerturbation 0.49537012520620244 --nonUniformMutationPerturbation 0.7264964475483606 --selection tournament --selectionTournamentSize 2 \n  ")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new ZDT4(), 100, 25000);
    autoNSGAII.parse(parameters);

    ConfigurableNSGAII.print(autoNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 500,
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
