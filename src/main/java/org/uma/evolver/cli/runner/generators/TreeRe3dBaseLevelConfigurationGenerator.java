package org.uma.evolver.cli.runner.generators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.BaseLevelConfig;
import org.uma.evolver.cli.runner.BaseLevelConfigurationWriter;

/**
 * Regenerates {@code src/main/resources/baseLevelConfigurations/Re3dNSGAIITreeBaseLevel.yaml} —
 * NSGA-II tuned on the RE problems with three objectives, for the tree-encoding meta-optimizer
 * example — from a compiler-checked {@link BaseLevelConfig}.
 *
 * <p>Same training set as {@link Re3dBaseLevelConfigurationGenerator}, but with different
 * indicators (NormalizedHypervolume instead of InvertedGenerationalDistancePlus), so it writes a
 * separate file rather than sharing one.
 */
public class TreeRe3dBaseLevelConfigurationGenerator {

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
            List.of("Epsilon", "NormalizedHypervolume")); // indicatorNames

    Path outputFile =
        Path.of("src/main/resources/baseLevelConfigurations/Re3dNSGAIITreeBaseLevel.yaml");
    BaseLevelConfigurationWriter.save(baseLevel, outputFile);
    System.out.println("Wrote " + outputFile);
  }
}
