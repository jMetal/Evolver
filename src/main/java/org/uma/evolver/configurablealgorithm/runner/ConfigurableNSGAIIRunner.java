package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zcat.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.IndicatorPlotObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

import java.io.IOException;

/**
 * Class configuring NSGA-II using arguments in the form (key, value) and the {@link
 * ConfigurableNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) throws IOException {

    String referenceFrontFileName = "resources/referenceFronts/ZCAT20.2D.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 63 --externalArchive unboundedArchive --createInitialSolutions random --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.9981823902234037 --crossoverRepairStrategy bounds --sbxDistributionIndex 68.00790628874215 --blxAlphaCrossoverAlphaValue 0.4889180982256614 --mutation polynomial --mutationProbabilityFactor 0.00817510064962912 --mutationRepairStrategy round --polynomialMutationDistributionIndex 286.0704738452895 --linkedPolynomialMutationDistributionIndex 159.08935473756773 --uniformMutationPerturbation 0.1990471714741109 --nonUniformMutationPerturbation 0.586239154007381 --selection random --selectionTournamentSize 2 \n")
            .split("\\s+");

    var configurableNSGAII =
        new ConfigurableNSGAII(new ZCAT20(2, 30, false, 1, true, false), 100, 50000);
    // var configurableNSGAII = new ConfigurableNSGAII(new ZDT4(), 100, 25000);
    configurableNSGAII.parse(parameters);

    ConfigurableNSGAII.print(configurableNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = configurableNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    IndicatorPlotObserver<DoubleSolution> hvPlotObserver =
        new IndicatorPlotObserver<>(
            "NHV", new NormalizedHypervolume(), referenceFrontFileName, 1000);

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);
    nsgaII.observable().register(hvPlotObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
