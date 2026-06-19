package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.PermutationPAES;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs PAES on the bi-objective KroAB100TSP (permutation encoding) using swap mutation
 * and a crowding-distance archive.
 */
public class PAESForKroAB100TSPExample {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "PAESPermutation.yaml";

    String[] parameters =
        String.join(
                " ",
                "--paesArchiveType crowdingDistanceArchive",
                "--algorithmResult externalArchive",
                "--archiveSelectionProbability 0.0",
                "--mutation swap",
                "--mutationProbability 0.01")
            .split("\\s+");

    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 20000;

    var paes =
        new PermutationPAES(
            new KroAB100TSP(),
            numberOfSolutionsToFind,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new PermutationParameterFactory()));

    paes.parse(parameters);
    EvolutionaryAlgorithm<PermutationSolution<Integer>> algorithm = paes.build();
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
