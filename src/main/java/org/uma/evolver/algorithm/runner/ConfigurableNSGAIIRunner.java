package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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
        ("--algorithmResult externalArchive --populationSizeWithArchive 58 --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation crossoverAndMutationVariation --offspringPopulationSize 130 --crossover SBX --crossoverProbability 0.9421322837028916 --crossoverRepairStrategy random --sbxDistributionIndex 70.47970043926885 --blxAlphaCrossoverAlphaValue 0.2943329839650555 --mutation uniform --mutationProbabilityFactor 0.6999930028979198 --mutationRepairStrategy round --polynomialMutationDistributionIndex 66.93217044933847 --linkedPolynomialMutationDistributionIndex 72.45985785314409 --uniformMutationPerturbation 0.4171820687324503 --nonUniformMutationPerturbation 0.4377666620287536 --selection random --selectionTournamentSize 4 \n")
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
