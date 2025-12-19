package org.uma.evolver.example.runner;

import org.uma.evolver.algorithm.base.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
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
--algorithmResult externalArchive --populationSizeWithArchive 91 --archiveType unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 200 --densityEstimator knn --knnNeighborhoodSize 4 --knnNormalizeObjectives true --ranking dominanceRanking --variation crossoverAndMutationVariation --crossover laplace --crossoverProbability 0.753296984025 --crossoverRepairStrategy bounds --laplaceCrossoverScale 0.4922345402714735 --mutation nonUniform --mutationProbabilityFactor 0.7690908717002871 --mutationRepairStrategy round --nonUniformMutationPerturbation 0.8310956709322816 --selection ranking --replacement rankingAndDensityEstimator --removalPolicy oneShot\s"""
            .split("\\s+");
    // 2. Create the training set
    var trainingSet = new RE3DTrainingSet();
    trainingSet.setEvaluationsToOptimize(10000);

    // 3. Create the algorithm template
    String yamlParameterSpaceFile = "RDEMOEADoubleFull.yaml";
    int populationSize = 100;

    var algorithmTemplate =
        new DoubleRDEMOEA(
            populationSize,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    // 4. Build and run the TrainingSetRunner
    var runner =
        new TrainingSetRunner.Builder<DoubleSolution>(trainingSet, algorithmTemplate, configuration)
            .outputDir("results/fronts/RE3D")
            .numberOfThreads(8) // Parallel execution
            .build();

    runner.run();

    System.out.println("\nVisualize results with:");
    System.out.println("  cd analysis/scripts/visualization");
    System.out.println(
        "  python visualize_training_set_results.py ../../../results/fronts/RE3D --grid");
  }
}
