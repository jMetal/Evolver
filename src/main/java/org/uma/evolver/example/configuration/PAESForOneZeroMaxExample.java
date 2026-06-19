package org.uma.evolver.example.configuration;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.BinaryPAES;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs PAES on OneZeroMax (binary encoding) using bit-flip mutation and a crowding-distance archive.
 */
public class PAESForOneZeroMaxExample {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "PAESBinary.yaml";

    String[] parameters =
        String.join(
                " ",
                "--paesArchiveType crowdingDistanceArchive",
                "--algorithmResult externalArchive",
                "--archiveSelectionProbability 0.0",
                "--mutation bitFlip",
                "--mutationProbabilityFactor 1.0")
            .split("\\s+");

    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 20000;

    var paes =
        new BinaryPAES(
            new OneZeroMax(512),
            numberOfSolutionsToFind,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));

    paes.parse(parameters);
    EvolutionaryAlgorithm<BinarySolution> algorithm = paes.build();
    algorithm.run();

    JMetalLogger.logger.info("Total execution time: " + algorithm.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + algorithm.numberOfEvaluations());
    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
