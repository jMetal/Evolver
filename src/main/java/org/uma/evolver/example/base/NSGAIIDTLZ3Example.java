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
        ("--algorithmResult externalArchive --populationSizeWithArchive 164 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9236653899572068 --crossoverRepairStrategy bounds --sbxDistributionIndex 54.59618759155867 --blxAlphaCrossoverAlpha 0.7757023722728449 --mutation uniform --mutationProbabilityFactor 0.9117111786450144 --mutationRepairStrategy bounds --uniformMutationPerturbation 0.26380775649649596 --polynomialMutationDistributionIndex 371.3467462072651 --linkedPolynomialMutationDistributionIndex 331.88840802885807 --nonUniformMutationPerturbation 0.8153482165609737 --selection tournament --selectionTournamentSize 9 \n")
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
