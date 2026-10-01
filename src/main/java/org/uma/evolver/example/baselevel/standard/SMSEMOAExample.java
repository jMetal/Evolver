package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
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
 * Runs SMS-EMOA with SBX crossover and polynomial mutation on DTLZ1 with three objectives,
 * plotting the front as the run advances.
 */
public class SMSEMOAExample {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new DTLZ1();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String yamlParameterSpaceFile = "SMSEMOADouble.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --gaSelection random
        """
            .split("\\s+");

    int evaluationObserverFrequency = 100;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseSMSEMOA =
        new DoubleSMSEMOA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseSMSEMOA.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> smsemoa = baseSMSEMOA.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<DoubleSolution>(
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
