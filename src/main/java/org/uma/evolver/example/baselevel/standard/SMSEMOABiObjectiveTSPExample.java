package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.smsemoa.PermutationSMSEMOA;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs SMS-EMOA with PMX crossover and swap mutation on the bi-objective TSP instance KroAB100,
 * plotting the front as the run advances.
 */
public class SMSEMOABiObjectiveTSPExample {

  public static void main(String[] args) throws IOException {
    PermutationProblem<PermutationSolution<Integer>> problem = new KroAB100TSP();
    String referenceFrontFileName = "resources/referenceFrontsTSP/KroAB100TSP.csv";

    String yamlParameterSpaceFile = "SMSEMOAPermutation.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --variation crossoverAndMutationVariation
        --crossover PMX
        --crossoverProbability 0.9
        --mutation swap
        --mutationProbability 0.08
        --gaSelection random
        """
            .split("\\s+");

    int evaluationObserverFrequency = 100;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseSMSEMOA =
        new PermutationSMSEMOA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new PermutationParameterFactory()));

    baseSMSEMOA.parse(parameters);
    EvolutionaryAlgorithm<PermutationSolution<Integer>> smsemoa = baseSMSEMOA.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<PermutationSolution<Integer>>(
            "SMS-EMOA. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
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

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(smsemoa.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));

    System.exit(0);
  }
}
