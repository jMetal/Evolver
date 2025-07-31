package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEAD_DTLZ2 {

  public static void main(String[] args) {
    DoubleProblem problem = new DTLZ2() ;
    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    String[] parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction penaltyBoundaryIntersection "
                + "--normalizeObjectives false "
                + "--pbiTheta 5.0 "
                + "--algorithmResult population "
                + "--createInitialSolutions default "
                + "--subProblemIdGenerator randomPermutationCycle "
                + "--variation crossoverAndMutationVariation "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--crossover SBX "
                + "--sbxDistributionIndex 20.0 "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var baseMOEAD =
        new DoubleMOEAD(
            problem, 100, 40000, "resources/weightVectors", new MOEADDoubleParameterSpace());
    baseMOEAD.parse(parameters);

    baseMOEAD.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> moead = baseMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOEAD. " + problem.name(), 80, 1000,
            referenceFrontFileName, "F1", "F2");

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
