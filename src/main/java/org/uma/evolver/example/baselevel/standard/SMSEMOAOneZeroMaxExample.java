package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.smsemoa.BinarySMSEMOA;
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
 * Runs SMS-EMOA with single-point crossover and bit-flip mutation on OneZeroMax with 1000 bits, a
 * binary problem without a reference front, plotting the front as the run advances.
 */
public class SMSEMOAOneZeroMaxExample {

  public static void main(String[] args) throws IOException {
    int numberOfBits = 1000;
    BinaryProblem problem = new OneZeroMax(numberOfBits);

    String yamlParameterSpaceFile = "SMSEMOABinary.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 100000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --variation crossoverAndMutationVariation
        --crossover singlePoint
        --crossoverProbability 0.9
        --mutation bitFlip
        --mutationProbabilityFactor 1.0
        --gaSelection random
        """
            .split("\\s+");

    int evaluationObserverFrequency = 100;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseSMSEMOA =
        new BinarySMSEMOA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));

    baseSMSEMOA.parse(parameters);
    EvolutionaryAlgorithm<BinarySolution> smsemoa = baseSMSEMOA.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<BinarySolution>(
            "SMS-EMOA. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            null,
            "F1",
            "F2");
    smsemoa.observable().register(evaluationObserver);
    smsemoa.observable().register(runTimeChartObserver);

    smsemoa.run();

    JMetalLogger.logger.info("Total execution time : " + smsemoa.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + smsemoa.numberOfEvaluations());

    new SolutionListOutput(smsemoa.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    System.exit(0);
  }
}
