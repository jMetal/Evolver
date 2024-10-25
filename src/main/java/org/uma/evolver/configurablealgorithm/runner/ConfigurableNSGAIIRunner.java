package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT1_2D;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT2_2D;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT3_2D;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT6_2D;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form (key, value) and the {@link ConfigurableNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 45 --externalArchive unboundedArchive --createInitialSolutions random --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8376231158003377 --crossoverRepairStrategy bounds --sbxDistributionIndex 175.2342006883794 --blxAlphaCrossoverAlphaValue 0.9308570666530741 --mutation linkedPolynomial --mutationProbabilityFactor 0.832562685046567 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 328.11849090807664 --linkedPolynomialMutationDistributionIndex 21.3452379400147 --uniformMutationPerturbation 0.7470633762182624 --nonUniformMutationPerturbation 0.9365446849400575 --selection tournament --selectionTournamentSize 9 \n")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAII(new ZDT4(), 100, 20000);
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
