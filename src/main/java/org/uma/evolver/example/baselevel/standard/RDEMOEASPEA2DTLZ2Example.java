package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs RDEMOEA configured as SPEA2 (strength ranking, k-nearest-neighbour density and sequential
 * removal) on DTLZ2 with three objectives, plotting the front as the run advances.
 */
public class RDEMOEASPEA2DTLZ2Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new DTLZ2();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    String yamlParameterSpaceFile = "RDEMOEADouble.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --variation crossoverAndMutationVariation
        --offspringPopulationSize 100
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --ranking strengthRanking
        --densityEstimator knn
        --knnNeighborhoodSize 1
        --knnNormalizeObjectives false
        --selection tournament
        --selectionTournamentSize 2
        --replacement rankingAndDensityEstimator
        --removalPolicy sequential
        """
            .split("\\s+");

    int evaluationObserverFrequency = 100;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseRDEMOEA =
        new DoubleRDEMOEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseRDEMOEA.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> spea2 = baseRDEMOEA.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<DoubleSolution>(
            "RDEMOEA as SPEA2. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
            "F1",
            "F2");
    spea2.observable().register(evaluationObserver);
    spea2.observable().register(runTimeChartObserver);

    spea2.run();

    JMetalLogger.logger.info("Total execution time : " + spea2.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + spea2.numberOfEvaluations());

    new SolutionListOutput(spea2.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(spea2.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));

    System.exit(0);
  }
}
