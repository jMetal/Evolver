package org.uma.evolver.example.runner;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.WFG2DTrainingSet;
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
 * python visualize_training_set_results.py ../../../results/WFG2D --grid
 * python visualize_training_set_results.py ../../../results/WFG2D --interactive
 * </pre>
 */
public class RunConfigurationOnWFG2DExample {

  public static void main(String[] args) {
    // 1. Define the optimized configuration (from meta-optimization on WFG2D)
    String[] configuration =
        """
        --algorithmResult externalArchive --populationSizeWithArchive 73 --archiveType crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover blxAlphaBeta --crossoverProbability 0.6466458903885894 --crossoverRepairStrategy bounds --blxAlphaBetaCrossoverBeta 0.4213474801441299 --blxAlphaBetaCrossoverAlpha 0.8024887081802909 --mutation nonUniform --mutationProbabilityFactor 0.6740009648716038 --mutationRepairStrategy round --nonUniformMutationPerturbation 0.6355943954800332 --selection tournament --selectionTournamentSize 6
        """
            .split("\\s+");

    // 2. Create the training set (can customize evaluations if needed)
    var trainingSet = new WFG2DTrainingSet();
    trainingSet.setEvaluationsToOptimize(25000);

    // 3. Create the algorithm template
    String yamlParameterSpaceFile = "NSGAIIDoubleFull.yaml";
    int populationSize = 100;
    
    var algorithmTemplate = new DoubleNSGAII(
        populationSize,
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
    );

    // 4. Build and run the TrainingSetRunner
    var runner = new TrainingSetRunner.Builder<DoubleSolution>(
            trainingSet, algorithmTemplate, configuration)
        .outputDir("results/fronts/WFG2D")
        .numberOfThreads(4)
        .build();

    runner.run();

    System.out.println("\nVisualize results with:");
    System.out.println("  cd analysis/scripts/visualization");
    System.out.println("  python visualize_training_set_results.py ../../../results/fronts/WFG2D --grid");
    System.out.println("  python visualize_training_set_results.py ../../../results/fronts/WFG2D --interactive");
  }
}
