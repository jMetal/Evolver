package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public class NSGAIIDTLZ3Example {
  public static void main(String[] args) {
    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 80 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8992315108160078 --crossoverRepairStrategy random --sbxDistributionIndex 46.97567805077092 --blxAlphaCrossoverAlpha 0.11245872234078169 --mutation uniform --mutationProbabilityFactor 0.9357403262498191 --mutationRepairStrategy round --uniformMutationPerturbation 0.42674707842462856 --polynomialMutationDistributionIndex 239.70699011444998 --linkedPolynomialMutationDistributionIndex 149.12908658424027 --nonUniformMutationPerturbation 0.9253100426929715 --selection tournament --selectionTournamentSize 3 \n")
            .split("\\s+");

    var baseNSGAII = new DoubleNSGAII(new DTLZ3(), 100, 40000, new NSGAIIDoubleParameterSpace());
    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();
    nsgaII.run();

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
