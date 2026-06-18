package org.uma.evolver.example.configuration;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs PAES on DTLZ1 using a crowding-distance archive as the density estimator.
 *
 * <p>Population size is fixed at 1 (non-configurable for PAES). The algorithm result is the
 * content of the PAES archive, which accumulates non-dominated solutions throughout the run.
 */
public class PAESForDTLZ1Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "PAESDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String[] parameters = String.join(
            " ",
            "--paesArchiveType crowdingDistanceArchive",
            "--algorithmResult externalArchive",
            "--archiveSelectionProbability 0.9189795374643632",
            "--mutation levyFlight",
            "--mutationProbabilityFactor 1.494881899307409",
            "--mutationRepairStrategy bounds",
            "--levyFlightMutationBeta 1.6558956256498565",
            "--levyFlightMutationStepSize 0.323402020766443")
            .split("\\s+");

    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 40000;

    var paes =
        new DoublePAES(
            new DTLZ1(),
            numberOfSolutionsToFind,
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
