package org.uma.evolver.example.base;

import java.io.IOException;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Example: Running NSGA-II on ZDT4 Problem
 * 
 * This example demonstrates how to configure and execute the NSGA-II algorithm to solve the ZDT4
 * multi-objective optimization problem using the Evolver framework.
 * 
 * Steps to run this example:
 * 1. The algorithm will automatically load parameters from NSGAIIDoubleFull.yaml
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
  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters ;
    if (args.length > 0) parameters = args;
    else
      parameters =
          ("--algorithmResult externalArchive --populationSizeWithArchive 89 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6885278888703463 --crossoverRepairStrategy bounds --sbxDistributionIndex 32.07999211591175 --blxAlphaCrossoverAlpha 0.640303817347435 --mutation linkedPolynomial --mutationProbabilityFactor 0.6952851214888922 --mutationRepairStrategy bounds --uniformMutationPerturbation 0.14262698171788724 --polynomialMutationDistributionIndex 18.40410700737766 --linkedPolynomialMutationDistributionIndex 17.696253388022207 --nonUniformMutationPerturbation 0.9843662953835077 --selection tournament --selectionTournamentSize 8 \n")
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
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(runTimeChartObserver);

    // 6. Run the algorithm
    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    // 7. Save results to output files
    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    // 8. Print quality indicators
    QualityIndicatorUtils.printQualityIndicators(
            SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
            VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
