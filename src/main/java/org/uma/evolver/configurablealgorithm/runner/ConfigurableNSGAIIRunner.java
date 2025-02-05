package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zcat.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form (key, value) and the {@link
 * ConfigurableNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--algorithmResult externalArchive " +
                "--populationSizeWithArchive 17 " +
                "--externalArchive crowdingDistanceArchive " +
                "--createInitialSolutions random " +
                "--variation crossoverAndMutationVariation " +
                "--offspringPopulationSize 5 " +
                "--crossover BLX_ALPHA " +
                "--crossoverProbability 0.6882344686 " +
                "--crossoverRepairStrategy bounds " +
                "--blxAlphaCrossoverAlphaValue 0.1751649236 --mutation uniform --mutationProbabilityFactor 1.2021700444 --mutationRepairStrategy bounds --uniformMutationPerturbation 0.8599724777 --selection tournament --selectionTournamentSize 5")
            .split("\\s+");

    // var configurableNSGAII = new ConfigurableNSGAII(new ZCAT3(2, 3, true, 1, false, false), 100,
    // 25000);
    var configurableNSGAII = new ConfigurableNSGAII(new ZDT4(), 100, 25000);
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
