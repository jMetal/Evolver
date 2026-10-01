package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
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
    DoubleProblem problem = new ZDT1();
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String yamlParameterSpaceFile = "PAESDouble.yaml";
    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 25000;

    String[] parameters =
        """
        --paesArchiveType crowdingDistanceArchive
        --algorithmResult paesArchive
        --archiveSelectionProbability 0.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        """
            .split("\\s+");

    var basePAES =
        new DoublePAES(
            problem,
            numberOfSolutionsToFind,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    basePAES.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> paes = basePAES.build();
    paes.run();

    JMetalLogger.logger.info("Total execution time : " + paes.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + paes.numberOfEvaluations());

    new SolutionListOutput(paes.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(paes.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
