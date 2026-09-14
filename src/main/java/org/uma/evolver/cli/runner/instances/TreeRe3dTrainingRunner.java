package org.uma.evolver.cli.runner.instances;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.BaseLevelConfig;
import org.uma.evolver.cli.runner.TrainingRequest;
import org.uma.evolver.cli.runner.TrainingRunner;
import org.uma.evolver.cli.runner.TreeMetaSearchConfig;

/**
 * Runs NSGA-II with the derivation tree encoding as meta-optimizer to configure NSGA-II using
 * the RE problems with three objectives as the training set, through the uniform
 * {@link TrainingRunner}. Equivalent to
 * {@code org.uma.evolver.example.training.TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D}.
 *
 * <p>Exercises the {@link TreeMetaSearchConfig} branch: there is no meta-level YAML parameter
 * space here (unlike {@link Zdt4TrainingRunner}/{@link Re3dTrainingRunner}/
 * {@link MoeadZdt4TrainingRunner}, which all use {@code FlatMetaSearchConfig}) — the
 * meta-optimizer operates directly on derivations of the base-level algorithm's own grammar.
 */
public class TreeRe3dTrainingRunner {

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "NSGA-II", // algorithmName
            100, // populationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // yamlParameterSpaceFile
            null, // extraConfig
            "RE3D", // trainingSetName
            null, // trainingProblemNames
            null, // trainingReferenceFrontFileNames
            null, // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume"),
            "results/tree-nsgaii/RE3D");

    TreeMetaSearchConfig metaSearch =
        new TreeMetaSearchConfig(
            2000, // metaMaxEvaluations
            50, // metaPopulationSize
            50, // metaOffspringSize
            8, // numberOfCores
            0.9, // crossoverProbability
            1.0, // mutationProbability
            20.0); // mutationDistributionIndex

    new TrainingRunner()
        .run(new TrainingRequest(baseLevel, metaSearch), Path.of("results/tree-nsgaii/RE3D/status.yaml"));
  }
}
