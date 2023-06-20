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
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--algorithmResult population --populationSizeWithArchive 152 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation crossoverAndMutationVariation --offspringPopulationSize 1 --crossover SBX --crossoverProbability 0.9560795158797861 --crossoverRepairStrategy bounds --sbxDistributionIndex 12.10693145206182 --blxAlphaCrossoverAlphaValue 0.8771983328448554 --mutation linkedPolynomial --mutationProbabilityFactor 0.533596410705968 --mutationRepairStrategy round --polynomialMutationDistributionIndex 279.700702608167 --linkedPolynomialMutationDistributionIndex 34.159749312146346 --uniformMutationPerturbation 0.12900944035511847 --nonUniformMutationPerturbation 0.858699996807509 --selection tournament --selectionTournamentSize 9 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII(new ZDT4(), 100, 20000);
    autoNSGAII.parse(parameters);

    AutoNSGAII.print(autoNSGAII.configurableParameterList());

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
