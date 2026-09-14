package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.TrainingRequest;
import org.uma.evolver.cli.runner.TrainingRunner;

/**
 * Runs NSGA-II as meta-optimizer to configure NSGA-II using the RE problems with three
 * objectives as the training set, through the uniform {@link TrainingRunner}. Equivalent to
 * {@code org.uma.evolver.example.training.NSGAIIOptimizingNSGAIIForBenchmarkRE3D}.
 */
public class Re3dTrainingRunner {

  public static void main(String[] args) throws IOException {
    TrainingRequest request =
        new TrainingRequest(
            2000, // metaMaxEvaluations
            null, // metaPopulationSize (use builder default)
            8, // numberOfCores
            null, // mutationProbabilityFactor (use builder default)
            "NSGAIIDoubleReduced.yaml", // metaYamlParameterSpaceFile
            "NSGA-II", // baseLevelAlgorithmName
            100, // baseLevelPopulationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // baseLevelYamlParameterSpaceFile
            null, // baseLevelExtraConfig
            "RE3D", // trainingSetName
            null, // trainingProblemNames
            null, // trainingReferenceFrontFileNames
            null, // trainingEvaluations
            List.of("Epsilon", "InvertedGenerationalDistancePlus"),
            "results/nsgaii/RE3D");

    new TrainingRunner().run(request, Path.of("results/nsgaii/RE3D/status.yaml"));
  }
}
