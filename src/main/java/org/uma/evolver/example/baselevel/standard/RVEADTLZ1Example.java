package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
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
 * Runs RVEA on DTLZ1 with three objectives. The population size is the number of reference
 * vectors, which are read from {@code resources/weightVectors/W3D_100.dat}.
 */
public class RVEADTLZ1Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new DTLZ1();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String yamlParameterSpaceFile = "RVEADouble.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

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
        --selection random
        --replacement rvea
        --alpha 2.0
        --fr 0.1
        """
            .split("\\s+");

    var baseRVEA =
        new DoubleRVEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            weightVectorFilesDirectory,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseRVEA.parse(parameters);
    Algorithm<List<DoubleSolution>> rvea = baseRVEA.build();
    rvea.run();

    new SolutionListOutput(rvea.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(rvea.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
