package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class NSGAIIDTLZ2WithArchiveExampleV2 {
  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 131 --archiveType unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.976418940698032 --crossoverRepairStrategy round --sbxDistributionIndex 82.12550784373838 --blxAlphaCrossoverAlpha 0.8607709450249538 --blxAlphaBetaCrossoverBeta 0.5021796145439621 --blxAlphaBetaCrossoverAlpha 0.7141465092667029 --laplaceCrossoverScale 0.3176134039416645 --fuzzyRecombinationCrossoverAlpha 0.9780081931363176 --pcxCrossoverZeta 0.0604178930573782 --pcxCrossoverEta 0.5684660525202027 --mutation levyFlight --mutationProbabilityFactor 0.7424688413365836 --mutationRepairStrategy round --uniformMutationPerturbation 0.775593605827027 --polynomialMutationDistributionIndex 283.9924164821486 --linkedPolynomialMutationDistributionIndex 296.7821669154807 --nonUniformMutationPerturbation 0.6440218870731419 --levyFlightMutationBeta 1.9046527204023227 --levyFlightMutationStepSize 0.5142023233190223 --powerLawMutationDelta 9.151452382077057 --selection tournament --selectionTournamentSize 6 \n")
            .split("\\s+");

    String yamlParameterSpaceFile = "resources/parameterSpaces/NSGAIIDouble.yaml" ;

    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var evNSGAII = new NSGAIIDouble(new DTLZ3(), 100, 30000, parameterSpace);
    evNSGAII.parse(parameters);

    System.out.println(parameterSpace) ;

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = evNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

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
