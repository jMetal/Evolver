package org.uma.evolver.example.baselevel.tuned;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
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
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String[] parameters = String.join(
                    " ",
                    "--paesArchiveType knnDistanceArchive",
                    "--knnDistanceArchiveK 4",
                    "--algorithmResult externalArchive",
                    "--archiveSelectionProbability 0.013727891319631647",
                    "--mutation levyFlight",
                    "--mutationProbabilityFactor 1.5474172144174396",
                    "--mutationRepairStrategy bounds",
                    "--levyFlightMutationBeta 1.9048695481272513",
                    "--levyFlightMutationStepSize 0.26218782546309416")
            .split("\\s+");

    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 50000;

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
