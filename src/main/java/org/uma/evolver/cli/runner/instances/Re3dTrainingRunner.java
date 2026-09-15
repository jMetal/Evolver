package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.BaseLevelConfig;
import org.uma.evolver.cli.runner.FlatMetaSearchConfig;
import org.uma.evolver.cli.runner.TrainingRequest;
import org.uma.evolver.cli.runner.TrainingRunner;

/**
 * Runs NSGA-II as meta-optimizer to configure NSGA-II using the RE problems with three
 * objectives as the training set, through the uniform {@link TrainingRunner}. Equivalent to
 * {@code org.uma.evolver.example.training.NSGAIIOptimizingNSGAIIForBenchmarkRE3D}.
 *
 * <p>The seven problems, reference fronts and evaluation budgets below are the same ones
 * {@code org.uma.evolver.trainingset.RE3DTrainingSet} bundles under the name {@code "RE3D"} —
 * spelled out explicitly here rather than referenced by name, so this request is self-contained
 * and does not require cross-referencing {@code RE3DTrainingSet} to know what it actually runs.
 */
public class Re3dTrainingRunner {

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "NSGA-II", // algorithmName
            100, // populationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // yamlParameterSpaceFile
            null, // extraConfig
            List.of("RE31", "RE32", "RE33", "RE34", "RE35", "RE36", "RE37"), // trainingProblemNames
            List.of(
                "resources/referenceFronts/RE31.csv",
                "resources/referenceFronts/RE32.csv",
                "resources/referenceFronts/RE33.csv",
                "resources/referenceFronts/RE34.csv",
                "resources/referenceFronts/RE35.csv",
                "resources/referenceFronts/RE36.csv",
                "resources/referenceFronts/RE37.csv"), // trainingReferenceFrontFileNames
            List.of(10000, 10000, 10000, 10000, 10000, 10000, 10000), // trainingEvaluations
            List.of("Epsilon", "InvertedGenerationalDistancePlus"),
            "results/nsgaii/RE3D");

    FlatMetaSearchConfig metaSearch =
        new FlatMetaSearchConfig(
            2000, // metaMaxEvaluations
            null, // metaPopulationSize (use builder default)
            8, // numberOfCores
            null, // mutationProbabilityFactor (use builder default)
            "NSGAIIDoubleReduced.yaml");

    new TrainingRunner()
        .run(new TrainingRequest(baseLevel, metaSearch), Path.of("results/nsgaii/RE3D/status.yaml"));
  }
}
