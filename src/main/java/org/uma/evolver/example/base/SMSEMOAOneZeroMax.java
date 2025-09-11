package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.smsemoa.BinarySMSEMOA;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class SMSEMOAOneZeroMax {
  public static void main(String[] args) {
    String yamlParameterSpaceFile = "SMSEMOABinary.yaml" ;

    var p = new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory());

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 1 "
                + "--crossover singlePoint "
                + "--crossoverProbability 0.9 "
                + "--mutation bitFlip "
                + "--mutationProbabilityFactor 1.0 "
                + "--selection random ")
            .split("\\s+");

    var baseAlgorithm =
        new BinarySMSEMOA(
            new OneZeroMax(1000),
            100,
            100000,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));
    baseAlgorithm.parse(parameters);

    EvolutionaryAlgorithm<BinarySolution> smsemoa = baseAlgorithm.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<BinarySolution> runTimeChartObserver =
        new RunTimeChartObserver<>("SMSEMOA", 80, 1000, null, "F1", "F2");

    smsemoa.observable().register(evaluationObserver);
    smsemoa.observable().register(runTimeChartObserver);

    smsemoa.run();

    JMetalLogger.logger.info("Total computing time: " + smsemoa.totalComputingTime());

    new SolutionListOutput(smsemoa.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
