package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Class configuring NSGA-II using arguments in the form (key, value) and the {@link ConfigurableNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/WFG.2D.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 111 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 50 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8898744113066657 --crossoverRepairStrategy round --sbxDistributionIndex 10.340034734550677 --blxAlphaCrossoverAlphaValue 0.8150790560730548 --mutation uniform --mutationProbabilityFactor 0.8606058307368026 --mutationRepairStrategy round --polynomialMutationDistributionIndex 363.9821559398714 --linkedPolynomialMutationDistributionIndex 135.05592356207515 --uniformMutationPerturbation 0.7273477093036995 --nonUniformMutationPerturbation 0.34415213634294417 --selection tournament --selectionTournamentSize 8 \n")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAII(new WFG9(), 100, 20000);
    configurableNSGAII.parse(parameters);

    ConfigurableNSGAII.print(configurableNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = configurableNSGAII.build();
/*
    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 500,
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

    System.exit(0);
  }
}
