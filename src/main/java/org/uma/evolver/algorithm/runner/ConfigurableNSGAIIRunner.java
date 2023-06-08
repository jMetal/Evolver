package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
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
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 10 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation crossoverAndMutationVariation --offspringPopulationSize 2 --crossover SBX --crossoverProbability 0.7514111751343366 --crossoverRepairStrategy random --sbxDistributionIndex 337.2225782977224 --blxAlphaCrossoverAlphaValue 0.8887293665601239 --mutation uniform --mutationProbabilityFactor 0.9741869521833462 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 91.98690669410949 --linkedPolynomialMutationDistributionIndex 26.05812028188079 --uniformMutationPerturbation 0.22369500025152544 --nonUniformMutationPerturbation 0.23371884601204373 --selection tournament --selectionTournamentSize 2")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new DTLZ3(), 100, 40000);
    autoNSGAII.parse(parameters);

    AutoNSGAII.print(autoNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build();

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
