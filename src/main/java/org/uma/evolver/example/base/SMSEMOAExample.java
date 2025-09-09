package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class SMSEMOAExample {
  public static void main(String[] args) {
    String yamlParameterSpaceFile = "SMSEMOADouble.yaml" ;
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection random ")
            .split("\\s+");

    var baseAlgorithm =
        new DoubleSMSEMOA(
            new DTLZ1(),
            100,
            40000,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));
    baseAlgorithm.parse(parameters);

    baseAlgorithm.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> smsemoa = baseAlgorithm.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

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
