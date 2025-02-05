package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT1;
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
public class ConfigurableNSGAIIDERunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";
    var problem = new DTLZ1() ;

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 177 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation differentialEvolutionVariation --mutation polynomial --mutationProbabilityFactor 0.5226121847727929 --mutationRepairStrategy round --polynomialMutationDistributionIndex 375.50428453945534 --linkedPolynomialMutationDistributionIndex 17.138525721366843 --uniformMutationPerturbation 0.8535363298400096 --nonUniformMutationPerturbation 0.1293021691338726 --differentialEvolutionCrossover RAND_1_EXP --CR 0.6129492681665641 --F 0.40136315469564954 \n")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAIIDE(problem, 100, 40000);
    configurableNSGAII.parse(parameters);

    ConfigurableNSGAII.print(configurableNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = configurableNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II-DE. " + problem.name(), 80, 1000,
            referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
