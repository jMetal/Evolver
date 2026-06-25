package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Example of running the configurable NSGA-II on ZDT1 with its default (standard) configuration.
 *
 * <p>Runs NSGA-II with SBX crossover (probability 0.9, distribution index 20), polynomial mutation
 * (probability 1/n, distribution index 20), binary tournament selection, a population of 100, and
 * 20000 evaluations. The parameter space is loaded from {@code NSGAIIDouble.yaml}. The resulting
 * Pareto front approximation is written to {@code VAR.csv}/{@code FUN.csv}, and the quality
 * indicators against the ZDT1 reference front are printed at the end of the run.
 */
public class NSGAIIForZDT1Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --offspringPopulationSize 100
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --selection tournament
        --selectionTournamentSize 2
        """
            .split("\\s+");

    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    var baseNSGAII =
        new DoubleNSGAII(
            new ZDT1(),
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaii = baseNSGAII.build();
    nsgaii.run();

    JMetalLogger.logger.info("Total execution time : " + nsgaii.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + nsgaii.numberOfEvaluations());

    new SolutionListOutput(nsgaii.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(nsgaii.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
