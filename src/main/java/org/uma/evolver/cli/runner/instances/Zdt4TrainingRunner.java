package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.BaseLevelConfig;
import org.uma.evolver.cli.runner.FlatMetaSearchConfig;
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
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "NSGA-II", // algorithmName
            100, // populationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // yamlParameterSpaceFile
            null, // extraConfig
            null, // trainingSetName
            List.of("ZDT4"), // trainingProblemNames
            List.of("resources/referenceFronts/ZDT4.csv"), // trainingReferenceFrontFileNames
            List.of(12000), // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume"),
            "results/nsgaii/ZDT4");

    FlatMetaSearchConfig metaSearch =
        new FlatMetaSearchConfig(
            2000, // metaMaxEvaluations
            100, // metaPopulationSize
            8, // numberOfCores
            1.5, // mutationProbabilityFactor
            "NSGAIIDoubleReduced.yaml");

    new TrainingRunner()
        .run(new TrainingRequest(baseLevel, metaSearch), Path.of("results/nsgaii/ZDT4/status.yaml"));
  }
}
