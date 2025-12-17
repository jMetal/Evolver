package org.uma.evolver.example.runner;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.util.TrainingSetRunner;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Example demonstrating how to run a meta-optimized configuration on DTLZ 3-objective problems.
 *
 * <p>This example runs a configured NSGA-II on all DTLZ problems (DTLZ1-DTLZ7) with 3 objectives.
 * Results are saved for visualization with Python scripts.
 *
 * <p>After running, visualize with:
 * <pre>
 * cd analysis/scripts/visualization
 * python visualize_training_set_results.py ../../../results/DTLZ3D --grid
 * python visualize_training_set_results.py ../../../results/DTLZ3D --interactive
 * </pre>
 */
public class RunConfigurationOnDTLZ3DExample {

  public static void main(String[] args) {
    // 1. Define the optimized configuration (from meta-optimization on DTLZ)
    String[] configuration =
        """
        --algorithmResult externalArchive --populationSizeWithArchive 58 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 100 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.915690039341087 --crossoverRepairStrategy round --sbxDistributionIndex 86.5733443611202 --mutation uniform --mutationProbabilityFactor 0.6201602806258746 --mutationRepairStrategy bounds --uniformMutationPerturbation 0.2523551997667263 --selection tournament --selectionTournamentSize 2
        """
            .split("\\s+");

    // 2. Create the training set (can customize evaluations if needed)
    var trainingSet = new DTLZ3DTrainingSet();
    trainingSet.setEvaluationsToOptimize(40000);

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
        .outputDir("results/fronts/DTLZ3D")
        .numberOfThreads(4)
        .build();

    runner.run();

    System.out.println("\nVisualize 3D results with:");
    System.out.println("  cd analysis/scripts/visualization");
    System.out.println("  python visualize_training_set_results.py ../../../results/fronts/DTLZ3D --grid");
    System.out.println("  python visualize_training_set_results.py ../../../results/fronts/DTLZ3D --interactive");
  }
}
