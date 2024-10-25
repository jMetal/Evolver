package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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

    String referenceFrontFileName = "resources/referenceFronts/LZ09_F2.csv";
    var problem = new LZ09F2() ;

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 50 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 400 --variation differentialEvolutionVariation --mutation polynomial --mutationProbabilityFactor 0.3078378639100489 --mutationRepairStrategy round --polynomialMutationDistributionIndex 11.087446774461819 --linkedPolynomialMutationDistributionIndex 291.0607557101411 --uniformMutationPerturbation 0.3059430679532979 --nonUniformMutationPerturbation 0.046036075207529996 --differentialEvolutionCrossover RAND_1_BIN --CR 1.0 --F 0.5 \n")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAIIDE(problem, 100, 250000);
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
