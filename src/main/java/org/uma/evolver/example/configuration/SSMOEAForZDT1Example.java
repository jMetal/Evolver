package org.uma.evolver.example.configuration;

import java.io.IOException;
import org.uma.evolver.algorithm.ssmoea.DoubleSSMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs the steady-state MOEA (SSMOEA) on ZDT1 using the crossover+mutation variation branch with
 * rankingAndDensityEstimator replacement.
 *
 * <p>The offspring population size is fixed at 1. The algorithm generates one candidate per
 * iteration, evaluates it, and immediately decides whether to incorporate it into the population
 * using non-dominated sorting and crowding distance.
 */
public class SSMOEAForZDT1Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "SSMOEADouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --ranking dominanceRanking
        --densityEstimator crowdingDistance
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --gaSelection tournament
        --selectionTournamentSize 2
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --replacement rankingAndDensityEstimator
        """
            .split("\\s+");

    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var ssmoea =
        new DoubleSSMOEA(
            new ZDT1(),
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    ssmoea.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> algorithm = ssmoea.build();
    algorithm.run();

    JMetalLogger.logger.info("Total execution time : " + algorithm.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + algorithm.numberOfEvaluations());

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(algorithm.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
