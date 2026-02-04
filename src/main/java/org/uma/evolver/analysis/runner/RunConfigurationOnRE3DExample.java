package org.uma.evolver.analysis.runner;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.RWA3DTrainingSet;
import org.uma.evolver.util.TrainingSetRunner;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Example demonstrating how to run a meta-optimized configuration on all problems in a TrainingSet.
 *
 * <p>This example:
 *
 * <ol>
 *   <li>Loads an optimized NSGA-II configuration (from meta-optimization results)
 *   <li>Runs the configured algorithm on all RE3D problems
 *   <li>Saves results to CSV files for visualization
 *   <li>Computes quality indicators (Epsilon, Normalized Hypervolume)
 * </ol>
 *
 * <p>After running, use the Python visualization script:
 *
 * <pre>
 * cd analysis/scripts/visualization
 * python visualize_training_set_results.py ../../../results/RE3D --grid --save fronts.png
 * </pre>
 */
public class RunConfigurationOnRE3DExample {

  public static void main(String[] args) {
    // 1. Define the optimized configuration (from meta-optimization)
    String[] configuration =
"""
--algorithmResult externalArchive --populationSizeWithArchive 159 --archiveType unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 200 --variation crossoverAndMutationVariation --crossover UNDC --crossoverProbability 0.18164832414858043 --crossoverRepairStrategy bounds --undcCrossoverZeta 0.8195275363857764 --undcCrossoverEta 0.2947763967486281 --mutation powerLaw --mutationProbabilityFactor 1.5887199148732867 --mutationRepairStrategy bounds --powerLawMutationDelta 9.158176127618344 --selection stochasticUniversalSampling"""            .split("\\s+");
    // 2. Create the training set
    var trainingSet = new RWA3DTrainingSet();
    trainingSet.setEvaluationsToOptimize(10000);

    // 3. Create the algorithm template
    String yamlParameterSpaceFile = "NSGAIIDoubleFull.yaml";
    int populationSize = 100;

    var algorithmTemplate =
        new DoubleNSGAII(
            populationSize,
                new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())) ;

    // 4. Build and run the TrainingSetRunner
    var runner =
        new TrainingSetRunner.Builder<DoubleSolution>(trainingSet, algorithmTemplate, configuration)
            .outputDir("results/fronts/RWA3D")
            .numberOfThreads(8) // Parallel execution
            .build();

    runner.run();

    System.out.println("\nVisualize results with:");
    System.out.println("  cd analysis/scripts/visualization");
    System.out.println(
        "  python visualize_training_set_results.py ../../../results/fronts/RWA3D --grid");
  }
}
