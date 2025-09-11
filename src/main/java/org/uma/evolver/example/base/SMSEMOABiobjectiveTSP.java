package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.algorithm.base.smsemoa.PermutationSMSEMOA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOA;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.MultiObjectiveTSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

import java.io.IOException;

public class SMSEMOABiobjectiveTSP {
  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "SMSEMOABinary.yaml" ;

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
        new PermutationSMSEMOA(
            new KroAB100TSP(),
            100,
            40000,
            new YAMLParameterSpace(yamlParameterSpaceFile, new PermutationParameterFactory()));
    baseAlgorithm.parse(parameters);

    EvolutionaryAlgorithm<PermutationSolution<Integer>> smsemoa = baseAlgorithm.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
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
