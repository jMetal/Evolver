package org.uma.evolver.example.baselevel.features;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;

/**
 * Example: running the configurable NSGA-II on DTLZ3 with an external archive.
 *
 * <p>This {@code features} example illustrates the external-archive capability on a 3-objective
 * problem: NSGA-II returns an unbounded external archive as the algorithm result
 * ({@code --algorithmResult externalArchive --archiveType unboundedArchive}) instead of the final
 * population. It runs a meta-optimized configuration (Latin-hypercube initialization, SBX
 * crossover, Lévy-flight mutation, binary tournament selection) on DTLZ3 with a population of 100
 * and 40000 evaluations, and registers an {@link EvaluationObserver} that logs progress every 100
 * evaluations. The parsed parameter space is printed to stdout at startup.
 *
 * <p>The parameter space is loaded from {@code NSGAIIDouble.yaml}. Results are written to
 * {@code VAR.csv}/{@code FUN.csv}, and the quality indicators against the DTLZ3 reference front
 * ({@code resources/referenceFronts/DTLZ3.3D.csv}) are printed at the end of the run.
 */
public class NSGAIIDTLZ3WithArchiveExample {

  /**
   * Runs the example.
   *
   * @param args not used; the configuration is the built-in {@code parameters} string.
   */
  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String[] parameters = """
        --algorithmResult externalArchive
        --populationSizeWithArchive 131
        --archiveType unboundedArchive
        --createInitialSolutions latinHypercubeSampling
        --offspringPopulationSize 5
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.976418940698032
        --crossoverRepairStrategy round
        --sbxDistributionIndex 82.12550784373838
        --blxAlphaCrossoverAlpha 0.8607709450249538
        --blxAlphaBetaCrossoverBeta 0.5021796145439621
        --blxAlphaBetaCrossoverAlpha 0.7141465092667029
        --laplaceCrossoverScale 0.3176134039416645
        --fuzzyRecombinationCrossoverAlpha 0.9780081931363176
        --pcxCrossoverZeta 0.0604178930573782
        --pcxCrossoverEta 0.5684660525202027
        --mutation levyFlight
        --mutationProbabilityFactor 0.7424688413365836
        --mutationRepairStrategy round
        --uniformMutationPerturbation 0.775593605827027
        --polynomialMutationDistributionIndex 283.9924164821486
        --linkedPolynomialMutationDistributionIndex 296.7821669154807
        --nonUniformMutationPerturbation 0.6440218870731419
        --levyFlightMutationBeta 1.9046527204023227
        --levyFlightMutationStepSize 0.5142023233190223
        --powerLawMutationDelta 9.151452382077057
        --selection tournament
        --selectionTournamentSize 6
        """.split("\\s+");

    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseNSGAII = new DoubleNSGAII(new DTLZ3(), 100, 40000, parameterSpace);
    baseNSGAII.parse(parameters);

    System.out.println(parameterSpace);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    nsgaII.observable().register(evaluationObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
