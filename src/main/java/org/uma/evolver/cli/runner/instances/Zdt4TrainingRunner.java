package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.TrainingRequest;
import org.uma.evolver.cli.runner.TrainingRunner;

/**
 * Runs NSGA-II as meta-optimizer to configure NSGA-II using problem ZDT4 as the training set,
 * through the uniform {@link TrainingRunner}. Equivalent to
 * {@code org.uma.evolver.example.training.NSGAIIOptimizingNSGAIIForProblemZDT4}, but built as a
 * plain {@link TrainingRequest} object instead of a hardcoded pipeline.
 */
public class Zdt4TrainingRunner {

  public static void main(String[] args) throws IOException {
    TrainingRequest request =
        new TrainingRequest(
            2000, // metaMaxEvaluations
            100, // metaPopulationSize
            8, // numberOfCores
            1.5, // mutationProbabilityFactor
            "NSGAIIDoubleReduced.yaml", // metaYamlParameterSpaceFile
            "NSGA-II", // baseLevelAlgorithmName
            100, // baseLevelPopulationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // baseLevelYamlParameterSpaceFile
            null, // baseLevelExtraConfig
            null, // trainingSetName
            List.of("ZDT4"), // trainingProblemNames
            List.of("resources/referenceFronts/ZDT4.csv"), // trainingReferenceFrontFileNames
            List.of(12000), // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume"),
            "results/nsgaii/ZDT4");

    new TrainingRunner().run(request, Path.of("results/nsgaii/ZDT4/status.yaml"));
  }
}
