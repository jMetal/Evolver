package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.paes.PermutationPAES;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs PAES on the bi-objective KroAB100TSP (permutation encoding) using swap mutation
 * and a crowding-distance archive.
 */
public class PAESForKroAB100TSPExample {

  public static void main(String[] args) throws IOException {
    PermutationProblem<PermutationSolution<Integer>> problem = new KroAB100TSP();
    String referenceFrontFileName = "resources/referenceFrontsTSP/KroAB100TSP.csv";

    String yamlParameterSpaceFile = "PAESPermutation.yaml";
    int numberOfSolutionsToFind = 100;
    int maximumNumberOfEvaluations = 20000;

    String[] parameters =
        """
        --paesArchiveType crowdingDistanceArchive
        --algorithmResult externalArchive
        --archiveSelectionProbability 0.0
        --mutation swap
        --mutationProbability 0.01
        """
            .split("\\s+");

    var basePAES =
        new PermutationPAES(
            problem,
            numberOfSolutionsToFind,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new PermutationParameterFactory()));

    basePAES.parse(parameters);
    EvolutionaryAlgorithm<PermutationSolution<Integer>> paes = basePAES.build();
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
