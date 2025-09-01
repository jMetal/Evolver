package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class NSGAIIDTLZ3Example {
  public static void main(String[] args) {
    String[] parameters =
        ("--algorithmResult externalArchive " +
                "--populationSizeWithArchive 133 " +
                "--archiveType unboundedArchive " +
                "--createInitialSolutions default " +
                "--offspringPopulationSize 2 " +
                "--variation crossoverAndMutationVariation " +
                "--crossover SBX " +
                "--crossoverProbability 0.9719337329527943 " +
                "--crossoverRepairStrategy random " +
                "--sbxDistributionIndex 133.8313543413145 " +
                "--mutation uniform " +
                "--mutationProbabilityFactor 0.5124086272844153 " +
                "--mutationRepairStrategy random " +
                "--uniformMutationPerturbation 0.22680609334711863 " +
                "--selection tournament " +
                "--selectionTournamentSize 5 \n")
            .split("\\s+");

    var baseNSGAII = new DoubleNSGAII(new DTLZ3(), 100, 40000, new NSGAIIDoubleParameterSpace());
    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();
    nsgaII.run();

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
