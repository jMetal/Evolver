package org.uma.evolver.example.configuration;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.DoublePAES;
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
 * Runs PAES on ZDT1 using a crowding-distance archive as the density estimator.
 *
 * <p>Population size is fixed at 1 (non-configurable for PAES). The algorithm result is the
 * content of the PAES archive, which accumulates non-dominated solutions throughout the run.
 */
public class PAESForZDT1Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "PAESDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        """
        --paesArchiveType crowdingDistanceArchive
        --paesArchiveSize 100
        --algorithmResult paesArchive
        --createInitialSolutions default
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        """
            .split("\\s+");

    int maximumNumberOfEvaluations = 25000;

    var paes =
        new DoublePAES(
            new ZDT1(),
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    paes.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> algorithm = paes.build();
    algorithm.run();

    JMetalLogger.logger.info("Total execution time : " + algorithm.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + algorithm.numberOfEvaluations());

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(algorithm.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
