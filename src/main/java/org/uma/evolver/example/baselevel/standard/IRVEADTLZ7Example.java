package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.referencepoint.ReferencePointGenerator;

/**
 * iRVEA on the disconnected three-objective front of DTLZ7, as jMetal's {@code
 * IRVEADTLZ7Example}: 91 reference vectors (a simplex lattice with 12 divisions) given as a list,
 * and the iRVEA replacement with 40 subregions (the offspring population size is 100, the value of
 * the parameter space closest to the 91 of jMetal's example). Run from the repository root.
 */
public class IRVEADTLZ7Example {
  public static void main(String[] args) throws IOException {
    String[] parameters = """
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
        """.split("\\s+");

    var problem = new DTLZ7(22, 3);
    var referenceVectors = ReferencePointGenerator.generateSingleLayer(3, 12);
    var parameterSpace = new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory());
    var rvea =
        new DoubleRVEA(problem, referenceVectors.size(), 25000, parameterSpace, referenceVectors);
    rvea.parse(parameters);

    var algorithm = rvea.build();
    algorithm.run();

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
