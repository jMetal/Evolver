package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOEADForSolvingInverted3DProblemRunner {

  public static void main(String[] args) {
    DoubleProblem problem = new DTLZ2Minus() ;

    String[] parameters =
        ("--neighborhoodSize 20"
            + " --maximumNumberOfReplacedSolutions 2 "
            + "--aggregationFunction penaltyBoundaryIntersection "
            + "--pbiTheta 5.0 "
            + "--normalizeObjectives False "
            + "--epsilonParameterForNormalizing 4 "
            + "--algorithmResult externalArchive "
            + "--externalArchive unboundedArchive "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy random "
            + "--polynomialMutationDistributionIndex 20.0 "
            + "--crossover  SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy random "
            + "--sbxDistributionIndex 20.0 "
            + "--selection populationAndNeighborhoodMatingPoolSelection "
            + "--neighborhoodSelectionProbability 0.9 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(problem, 100, 25000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    ConfigurableMOEAD.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> moead = autoMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOEAD." + problem.name(), 80, 100,
            null, "F1", "F2");

    moead.observable().register(evaluationObserver);
    moead.observable().register(runTimeChartObserver);

    moead.run();

    JMetalLogger.logger.info("Total computing time: " + moead.totalComputingTime());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
