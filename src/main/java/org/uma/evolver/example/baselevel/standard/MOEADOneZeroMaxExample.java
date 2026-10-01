package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.moead.BinaryMOEAD;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs MOEA/D with single-point crossover and bit-flip mutation on OneZeroMax, a binary problem
 * without a reference front, plotting the front as the run advances.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADOneZeroMaxExample {

  public static void main(String[] args) throws IOException {
    int numberOfBits = 512;
    BinaryProblem problem = new OneZeroMax(numberOfBits);

    String yamlParameterSpaceFile = "MOEADBinary.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 10000;

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
        --crossover singlePoint
        --crossoverProbability 0.9
        --mutation bitFlip
        --mutationProbabilityFactor 1.0
        --selection populationAndNeighborhoodMatingPoolSelection
        --neighborhoodSelectionProbability 0.9
        """
            .split("\\s+");

    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseMOEAD =
        new BinaryMOEAD(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            weightVectorFilesDirectory,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));

    baseMOEAD.parse(parameters);
    EvolutionaryAlgorithm<BinarySolution> moead = baseMOEAD.build();

    var runTimeChartObserver =
        new RunTimeChartObserver<BinarySolution>(
            "MOEA/D", chartDisplayDelay, chartUpdateFrequency, null, "F1", "F2");
    moead.observable().register(runTimeChartObserver);

    moead.run();

    JMetalLogger.logger.info("Total execution time : " + moead.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + moead.numberOfEvaluations());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
  }
}
