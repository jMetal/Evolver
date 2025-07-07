package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.NSGAIIBinary;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class NSGAIIOneZeroMaxExample {  
  public static void main(String[] args) {

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 1 "
                + "--crossover singlePoint "
                + "--crossoverProbability 0.9 "
                + "--mutation bitFlip "
                + "--mutationProbabilityFactor 1.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split("\\s+");

    var baseNSGAII = new NSGAIIBinary(new OneZeroMax(), 100, 5000);
    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<BinarySolution> nsgaII = baseNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<BinarySolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, null, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
