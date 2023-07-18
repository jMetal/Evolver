package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring SMS-EMOA using arguments in the form <key, value> and the {@link ConfigurableSMSEMOA}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableSMSEMOARunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 83 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --model steadyState --offspringPopulationSize 100 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.19266823108855072 --crossoverRepairStrategy bounds --sbxDistributionIndex 103.69370217320794 --blxAlphaCrossoverAlphaValue 0.8961783851503412 --mutation linkedPolynomial --mutationProbabilityFactor 1.0132184971489644 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 158.39649171594044 --linkedPolynomialMutationDistributionIndex 5.565728098862394 --uniformMutationPerturbation 0.8593793632073513 --nonUniformMutationPerturbation 0.9882758308775352 --selection random --selectionTournamentSize 8 \n \n  ")
            .split("\\s+");

    var autoSMSEMOA = new ConfigurableSMSEMOA(new ZDT4(), 100, 25000);
    autoSMSEMOA.parse(parameters);

    ConfigurableNSGAII.print(autoSMSEMOA.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoSMSEMOA.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "SMS-EMOA", 80, 500,
            referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0) ;
  }
}
