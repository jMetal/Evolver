package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Class configuring SMS-EMOA using arguments in the form (key, value) and the {@link ConfigurableSMSEMOA}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableSMSEMOARunner {

  public static void main(String[] args) {

    //JMetalRandom.getInstance().setSeed();
    System.out.println(JMetalRandom.getInstance().getSeed()) ;
    String referenceFrontFileName = "resources/referenceFronts/ZDT2.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 25 --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.21329718643714668 --crossoverRepairStrategy round --sbxDistributionIndex 246.6486340677493 --blxAlphaCrossoverAlphaValue 0.6242369933697047 --mutation uniform --mutationProbabilityFactor 0.264570521720477 --mutationRepairStrategy random --polynomialMutationDistributionIndex 375.4916733032723 --linkedPolynomialMutationDistributionIndex 346.37753864406125 --uniformMutationPerturbation 0.9881113260902856 --nonUniformMutationPerturbation 0.9396723644616809 --selection random --selectionTournamentSize 6 \n")
            .split("\\s+");

    var autoSMSEMOA = new ConfigurableSMSEMOA(new ZDT2(), 100, 25000);
    autoSMSEMOA.parse(parameters);

    ConfigurableNSGAII.print(autoSMSEMOA.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoSMSEMOA.build();

    /*
    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "SMS-EMOA", 80, 500,
            referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);
*/
    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0) ;
  }
}
