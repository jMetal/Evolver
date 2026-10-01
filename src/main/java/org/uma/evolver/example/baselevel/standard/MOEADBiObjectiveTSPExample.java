package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.moead.PermutationMOEAD;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.IndicatorPlotObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs MOEA/D with PMX crossover and swap mutation on the bi-objective TSP instance KroAB100,
 * plotting the front and the evolution of the epsilon and HV- indicators as the run advances.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADBiObjectiveTSPExample {

  public static void main(String[] args) throws IOException {
    PermutationProblem<PermutationSolution<Integer>> problem = new KroAB100TSP();
    String referenceFrontFileName = "resources/referenceFrontsTSP/KroAB100TSP.csv";

    String yamlParameterSpaceFile = "MOEADPermutation.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 1000000;

    String[] parameters =
        """
        --neighborhoodSize 20
        --maximumNumberOfReplacedSolutions 2
        --aggregationFunction penaltyBoundaryIntersection
        --normalizeObjectives true
        --epsilonParameterForNormalization 6
        --pbiTheta 5.0
        --algorithmResult population
        --createInitialSolutions default
        --subProblemIdGenerator randomPermutationCycle
        --variation crossoverAndMutationVariation
        --crossover PMX
        --crossoverProbability 0.9
        --mutation swap
        --mutationProbability 0.08
        --selection populationAndNeighborhoodMatingPoolSelection
        --neighborhoodSelectionProbability 0.9
        """
            .split("\\s+");

    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;
    int epsilonPlotUpdateFrequency = 100;
    int hypervolumePlotUpdateFrequency = 1000;

    var baseMOEAD =
        new PermutationMOEAD(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            weightVectorFilesDirectory,
            new YAMLParameterSpace(yamlParameterSpaceFile, new PermutationParameterFactory()));

    baseMOEAD.parse(parameters);
    EvolutionaryAlgorithm<PermutationSolution<Integer>> moead = baseMOEAD.build();

    var runTimeChartObserver =
        new RunTimeChartObserver<PermutationSolution<Integer>>(
            "MOEA/D",
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
            "F1",
            "F2");
    var epsilonPlotObserver =
        new IndicatorPlotObserver<PermutationSolution<Integer>>(
            "MOEA/D", new Epsilon(), referenceFrontFileName, epsilonPlotUpdateFrequency);
    var hypervolumePlotObserver =
        new IndicatorPlotObserver<PermutationSolution<Integer>>(
            "MOEA/D", new HypervolumeMinus(), referenceFrontFileName, hypervolumePlotUpdateFrequency);
    moead.observable().register(runTimeChartObserver);
    moead.observable().register(epsilonPlotObserver);
    moead.observable().register(hypervolumePlotObserver);

    moead.run();

    JMetalLogger.logger.info("Total execution time : " + moead.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + moead.numberOfEvaluations());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(moead.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
