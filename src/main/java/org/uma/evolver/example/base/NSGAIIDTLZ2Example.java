package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class NSGAIIDTLZ2Example {
  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 133 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9719337329527943 --crossoverRepairStrategy random --sbxDistributionIndex 133.8313543413145 --blxAlphaCrossoverAlpha 0.35592035111163245 --blxAlphaBetaCrossoverBeta 0.13149224166847348 --blxAlphaBetaCrossoverAlpha 0.6589765370173779 --laplaceCrossoverScale 0.3409664525672087 --fuzzyRecombinationCrossoverAlpha 0.05198378081562216 --pcxCrossoverZeta 0.49422123290818626 --pcxCrossoverEta 0.7719904713133385 --undcCrossoverZeta 0.42163562749351335 --undcCrossoverEta 0.2463468787851745 --mutation uniform --mutationProbabilityFactor 0.5124086272844153 --mutationRepairStrategy random --uniformMutationPerturbation 0.22680609334711863 --polynomialMutationDistributionIndex 201.2189407768613 --linkedPolynomialMutationDistributionIndex 68.36002685019898 --nonUniformMutationPerturbation 0.0964688258172639 --levyFlightMutationBeta 1.4129548157288372 --levyFlightMutationStepSize 0.2750588610075625 --powerLawMutationDelta 4.737070277615714 --selection tournament --selectionTournamentSize 5 \n")
            .split("\\s+");

    var baseNSGAII = new DoubleNSGAII(new DTLZ3(), 100, 40000, new NSGAIIDoubleParameterSpace());
    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 500, referenceFrontFileName, "F1", "F2");

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
