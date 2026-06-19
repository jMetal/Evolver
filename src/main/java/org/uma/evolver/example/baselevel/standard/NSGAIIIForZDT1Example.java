package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaiii.DoubleNSGAIII;
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
 * Example of running the configurable NSGA-III on ZDT1 with the default configuration.
 *
 * <p>Runs NSGA-III with SBX(p=0.9, di=20), polynomial mutation (p=1/n, di=20), a population of
 * 100 (which on a bi-objective problem yields a Das-Dennis lattice of 100 reference points), and
 * 20000 evaluations. The quality indicators are printed at the end of the run.
 */
public class NSGAIIIForZDT1Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIIDouble.yaml";
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

    var baseNSGAIII =
        new DoubleNSGAIII(
            new ZDT1(),
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseNSGAIII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaiii = baseNSGAIII.build();
    nsgaiii.run();

    JMetalLogger.logger.info("Total execution time : " + nsgaiii.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + nsgaiii.numberOfEvaluations());

    new SolutionListOutput(nsgaiii.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(nsgaiii.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
