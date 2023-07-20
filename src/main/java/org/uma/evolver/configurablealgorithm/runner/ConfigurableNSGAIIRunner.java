package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;

import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
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

    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 190 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.923753535628965 --crossoverRepairStrategy round --sbxDistributionIndex 62.2630806274734 --blxAlphaCrossoverAlphaValue 0.35965955597705745 --mutation uniform --mutationProbabilityFactor 0.3514924318091968 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 15.099374045878825 --linkedPolynomialMutationDistributionIndex 313.69841419815543 --uniformMutationPerturbation 0.2587621121188072 --nonUniformMutationPerturbation 0.3514149059879736 --selection tournament --selectionTournamentSize 8 \n ")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new DTLZ1(), 100, 25000);
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
