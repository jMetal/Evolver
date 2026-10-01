package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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
 * Runs RDEMOEA configured as NSGA-II (dominance ranking, crowding distance and one-shot removal)
 * on ZDT4, plotting the front as the run advances.
 */
public class RDEMOEANSGAIIZDT4Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new ZDT4();
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String yamlParameterSpaceFile = "RDEMOEADouble.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

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
        --ranking dominanceRanking
        --densityEstimator crowdingDistance
        --selection tournament
        --selectionTournamentSize 2
        --replacement rankingAndDensityEstimator
        --removalPolicy oneShot
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
    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseRDEMOEA.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<DoubleSolution>(
            "RDEMOEA as NSGA-II. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
            "F1",
            "F2");
    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total execution time : " + nsgaII.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + nsgaII.numberOfEvaluations());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));

    System.exit(0);
  }
}
