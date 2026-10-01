package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.BinaryNSGAII;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs steady-state NSGA-II (one offspring per generation) with single-point crossover and
 * bit-flip mutation on OneZeroMax, a binary problem without a reference front, plotting the front
 * as the run advances.
 */
public class NSGAIIOneZeroMaxExample {

  public static void main(String[] args) throws IOException {
    int numberOfBits = 512;
    BinaryProblem problem = new OneZeroMax(numberOfBits);

    String yamlParameterSpaceFile = "NSGAIIBinary.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 5000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --variation crossoverAndMutationVariation
        --offspringPopulationSize 1
        --crossover singlePoint
        --crossoverProbability 0.9
        --mutation bitFlip
        --mutationProbabilityFactor 1.0
        --selection tournament
        --selectionTournamentSize 2
        """
            .split("\\s+");

    int evaluationObserverFrequency = 100;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseNSGAII =
        new BinaryNSGAII(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));

    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<BinarySolution> nsgaII = baseNSGAII.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<BinarySolution>(
            "NSGA-II. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            null,
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

    System.exit(0);
  }
}
