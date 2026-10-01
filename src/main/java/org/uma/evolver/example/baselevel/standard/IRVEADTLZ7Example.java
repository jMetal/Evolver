package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.referencepoint.ReferencePointGenerator;

/**
 * iRVEA on the disconnected three-objective front of DTLZ7, as jMetal's {@code
 * IRVEADTLZ7Example}: 91 reference vectors (a simplex lattice with 12 divisions) given as a list,
 * and the iRVEA replacement with 40 subregions (the offspring population size is 100, the value of
 * the parameter space closest to the 91 of jMetal's example). Run from the repository root.
 */
public class IRVEADTLZ7Example {

  public static void main(String[] args) throws IOException {
    int numberOfVariables = 22;
    int numberOfObjectives = 3;
    DoubleProblem problem = new DTLZ7(numberOfVariables, numberOfObjectives);
    String referenceFrontFileName = "resources/referenceFronts/DTLZ7.3D.csv";

    String yamlParameterSpaceFile = "RVEADouble.yaml";
    int referenceVectorDivisions = 12;
    int maximumNumberOfEvaluations = 25000;

    String[] parameters =
        """
        --algorithmResult population
        --createInitialSolutions default
        --offspringPopulationSize 100
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 1.0
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --selection random
        --replacement iRVEA
        --alpha 2.0
        --fr 0.1
        --numberOfSubregions 40
        --lateStageFraction 0.8
        --epsilonKappa 0.05
        """
            .split("\\s+");

    List<double[]> referenceVectors =
        ReferencePointGenerator.generateSingleLayer(numberOfObjectives, referenceVectorDivisions);

    var baseRVEA =
        new DoubleRVEA(
            problem,
            referenceVectors.size(),
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()),
            referenceVectors);

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
