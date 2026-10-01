package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.BinaryPAES;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
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
    int numberOfBits = 512;
    BinaryProblem problem = new OneZeroMax(numberOfBits);

    String yamlParameterSpaceFile = "PAESBinary.yaml";
    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 20000;

    String[] parameters =
        """
        --paesArchiveType crowdingDistanceArchive
        --algorithmResult externalArchive
        --archiveSelectionProbability 0.0
        --mutation bitFlip
        --mutationProbabilityFactor 1.0
        """
            .split("\\s+");

    var basePAES =
        new BinaryPAES(
            problem,
            numberOfSolutionsToFind,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory()));

    basePAES.parse(parameters);
    EvolutionaryAlgorithm<BinarySolution> paes = basePAES.build();
    paes.run();

    JMetalLogger.logger.info("Total execution time : " + paes.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + paes.numberOfEvaluations());

    new SolutionListOutput(paes.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
  }
}
