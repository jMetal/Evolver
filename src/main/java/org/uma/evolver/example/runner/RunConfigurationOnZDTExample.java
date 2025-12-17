package org.uma.evolver.example.runner;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.ZDTTrainingSet;
import org.uma.evolver.util.TrainingSetRunner;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Example demonstrating how to run a meta-optimized configuration on all problems in a TrainingSet.
 *
 * <p>This example:
 * <ol>
 *   <li>Loads an optimized NSGA-II configuration (from meta-optimization results)</li>
 *   <li>Runs the configured algorithm on all ZDT problems</li>
 *   <li>Saves results to CSV files for visualization</li>
 *   <li>Computes quality indicators (Epsilon, Normalized Hypervolume)</li>
 * </ol>
 *
 * <p>After running, use the Python visualization script:
 * <pre>
 * cd analysis/scripts/visualization
 * python visualize_training_set_results.py ../../../results/ZDT --grid --save fronts.png
 * </pre>
 */
public class RunConfigurationOnZDTExample {

  public static void main(String[] args) {
    // 1. Define the optimized configuration (from meta-optimization)
    String[] configuration =
        """
        --algorithmResult externalArchive --populationSizeWithArchive 19 --archiveType crowdingDistanceArchive --createInitialSolutions default --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6840261607155629 --crossoverRepairStrategy bounds --sbxDistributionIndex 15.680767123623234 --mutation linkedPolynomial --mutationProbabilityFactor 0.9213697790615174 --mutationRepairStrategy bounds --linkedPolynomialMutationDistributionIndex 6.433262888180897 --selection tournament --selectionTournamentSize 3\s
        """
            .split("\\s+");

    // 2. Create the training set
    var trainingSet = new ZDTTrainingSet();

    // 3. Create the algorithm template
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    int populationSize = 100;
    
    var algorithmTemplate = new DoubleNSGAII(
        populationSize,
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
    );

    // 4. Build and run the TrainingSetRunner
    var runner = new TrainingSetRunner.Builder<DoubleSolution>(
            trainingSet, algorithmTemplate, configuration)
        .outputDir("results/fronts/ZDT")
        .numberOfThreads(4)  // Parallel execution
        .build();

    runner.run();

    System.out.println("\nVisualize results with:");
    System.out.println("  cd analysis/scripts/visualization");
    System.out.println("  python visualize_training_set_results.py ../../../results/fronts/ZDT --grid");
  }
}
