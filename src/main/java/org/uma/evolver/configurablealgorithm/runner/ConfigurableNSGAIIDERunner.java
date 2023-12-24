package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
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

    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";
    var problem = new ZDT4() ;

    String[] parameters =
        ("--algorithmResult externalArchive "
            + "--createInitialSolutions random "
            + "--offspringPopulationSize 100 "
            + "--populationSizeWithArchive 40 "
            + "--externalArchive crowdingDistanceArchive "
            + "--variation differentialEvolutionVariation "
            + "--differentialEvolutionCrossover RAND_1_BIN "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy round "
            + "--CR 0.5 "
            + "--F 0.5 "
            + "--polynomialMutationDistributionIndex 20.0 ")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAIIDE(problem, 100, 25000);
    configurableNSGAII.parse(parameters);

    ConfigurableNSGAII.print(configurableNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = configurableNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II. " + problem.name(), 80, 1000,
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
