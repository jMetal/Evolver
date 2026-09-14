package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.runner.TrainingRequest;
import org.uma.evolver.cli.runner.TrainingRunner;

/**
 * Runs NSGA-II as meta-optimizer to configure MOEA/D using problem ZDT4 as the training set,
 * through the uniform {@link TrainingRunner}. Equivalent to
 * {@code org.uma.evolver.example.training.NSGAIIOptimizingMOEADForProblemZDT4}.
 *
 * <p>Exercises a base-level algorithm other than NSGA-II, which needs extra algorithm-specific
 * configuration ({@code weightVectorFilesDirectory}) carried in {@code baseLevelExtraConfig}.
 */
public class MoeadZdt4TrainingRunner {

  public static void main(String[] args) throws IOException {
    TrainingRequest request =
        new TrainingRequest(
            2000, // metaMaxEvaluations
            100, // metaPopulationSize
            8, // numberOfCores
            null, // mutationProbabilityFactor (use builder default)
            "NSGAIIDoubleReduced.yaml", // metaYamlParameterSpaceFile
            "MOEAD", // baseLevelAlgorithmName
            100, // baseLevelPopulationSize
            1, // numberOfIndependentRuns
            "MOEADDouble.yaml", // baseLevelYamlParameterSpaceFile
            Map.of("weightVectorFilesDirectory", "resources/weightVectors"),
            null, // trainingSetName
            List.of("ZDT4"), // trainingProblemNames
            List.of("resources/referenceFronts/ZDT4.csv"), // trainingReferenceFrontFileNames
            List.of(10000), // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume"),
            "results/moead/ZDT4");

    new TrainingRunner().run(request, Path.of("results/moead/ZDT4/status.yaml"));
  }
}
