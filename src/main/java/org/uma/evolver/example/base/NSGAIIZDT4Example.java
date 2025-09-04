package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Example: Running NSGA-II on ZDT4 Problem
 * 
 * This example demonstrates how to configure and execute the NSGA-II algorithm to solve the ZDT4
 * multi-objective optimization problem using the Evolver framework.
 * 
 * Steps to run this example:
 * 1. The algorithm will automatically load parameters from NSGAIIDouble.yaml
 * 2. The reference front is loaded from resources/referenceFronts/ZDT4.csv
 * 3. The algorithm will run for a maximum of 25,000 evaluations
 * 4. Results are saved to VAR.csv (variables) and FUN.csv (objectives)
 * 5. A real-time chart shows the evolution of the Pareto front (if enabled)
 */

public class NSGAIIZDT4Example {
  
  /**
   * Main execution method for the NSGA-II ZDT4 example.
   * 
   * The algorithm is configured with the following key parameters:
   * - Population size: 100
   * - Maximum evaluations: 25,000
   * - Crossover: SBX with probability 0.9
   * - Mutation: Polynomial with distribution index 20.0
   * - Selection: Binary tournament
   * 
   * @param args Command line arguments. Not used in the example, but it is possible to run the program using as arguments the same parameter string 
   * assigned to the parameters variable. In that case, just assign args to the
   * parameters variable.
   */
  public static void main(String[] args) {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters ;
    if (args.length > 0) parameters = args;
    else
      parameters =
          ("--algorithmResult externalArchive --populationSizeWithArchive 29 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 400 --variation crossoverAndMutationVariation --crossover fuzzyRecombination --crossoverProbability 0.809366562543433 --crossoverRepairStrategy round --sbxDistributionIndex 127.31165192729452 --pcxCrossoverZeta 0.2134813981122512 --pcxCrossoverEta 0.28047688996287856 --blxAlphaCrossoverAlpha 0.8317949922146415 --blxAlphaBetaCrossoverBeta 0.2345621884589518 --blxAlphaBetaCrossoverAlpha 0.7889247943665949 --laplaceCrossoverScale 0.807996467542485 --fuzzyRecombinationCrossoverAlpha 0.8924007279079302 --mutation powerLaw --mutationProbabilityFactor 1.3017685493661357 --mutationRepairStrategy random --uniformMutationPerturbation 0.27450099851705995 --nonUniformMutationPerturbation 0.7048302131243356 --polynomialMutationDistributionIndex 329.79845561187 --linkedPolynomialMutationDistributionIndex 163.91557854639427 --levyFlightMutationBeta 1.7553234174224808 --levyFlightMutationStepSize 0.6610232048553949 --powerLawMutationDelta 4.8733474437145565 --selection tournament --selectionTournamentSize 6 \n")
              .split("\\s+");

    // 2. Initialize algorithm parameters
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;
    
    // 3. Create and configure NSGA-II instance
    var baseNSGAII = new DoubleNSGAII(
        new ZDT4(),
        populationSize,
        maximumNumberOfEvaluations,
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
    );
    
    // 4. Parse parameters and build the base-level NSGA-II algorithm
    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();

    // 5. Optional: Register observers
    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    // 6. Run the algorithm
    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    // 7. Save results to output files
    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
