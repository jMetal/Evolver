package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public class RVEADTLZ1Example {
  public static void main(String[] args) throws IOException {
    String[] parameters = """
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
        """.split("\\s+");

    // The reference vectors are read from resources/weightVectors/W3D_100.dat
    int populationSize = 100;

    var problem = new DTLZ1();
    var parameterSpace = new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory());
    var rvea =
        new DoubleRVEA(
            problem, populationSize, 40000, "resources/weightVectors", parameterSpace);
    rvea.parse(parameters);

    var algorithm = rvea.build();
    algorithm.run();

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
