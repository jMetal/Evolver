package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zcat.*;
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
        ("--algorithmResult externalArchive --populationSizeWithArchive 73 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover wholeArithmetic --crossoverProbability 0.28069785841963524 --crossoverRepairStrategy bounds --sbxDistributionIndex 245.39690846527182 --blxAlphaCrossoverAlphaValue 0.01888966497350019 --mutation linkedPolynomial --mutationProbabilityFactor 1.988935485811351 --mutationRepairStrategy round --polynomialMutationDistributionIndex 260.62137030954744 --linkedPolynomialMutationDistributionIndex 9.076401102943487 --uniformMutationPerturbation 0.16515656717764357 --nonUniformMutationPerturbation 0.9619546810048647 --selection tournament --selectionTournamentSize 7 \n")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAII(new ZCAT16_2D(), 100, 50000);
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
