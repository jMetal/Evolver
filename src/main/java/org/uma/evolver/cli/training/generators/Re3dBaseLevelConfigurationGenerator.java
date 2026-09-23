package org.uma.evolver.cli.training.generators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.ProblemSpec;
import org.uma.evolver.cli.training.BaseLevelConfigurationWriter;

/**
 * Regenerates {@code src/main/resources/baseLevelConfigurations/Re3dNSGAIIBaseLevel.yaml} — NSGA-II
 * tuned on the RE problems with three objectives — from a compiler-checked {@link BaseLevelConfig}.
 *
 * <p>The seven problems, reference fronts and evaluation budgets below are the same ones
 * {@code org.uma.evolver.trainingset.RE3DTrainingSet} bundles under the name {@code "RE3D"} —
 * spelled out explicitly here rather than referenced by name, so the generated file is
 * self-contained and does not require cross-referencing {@code RE3DTrainingSet}.
 */
public class Re3dBaseLevelConfigurationGenerator {

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "NSGA-II", // algorithmName
            "Double", // encoding
            100, // populationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // yamlParameterSpaceFile
            null, // extraConfig
            ProblemSpec.of("RE31", "RE32", "RE33", "RE34", "RE35", "RE36", "RE37"), // trainingProblemNames
            List.of(
                "resources/referenceFronts/RE31.csv",
                "resources/referenceFronts/RE32.csv",
                "resources/referenceFronts/RE33.csv",
                "resources/referenceFronts/RE34.csv",
                "resources/referenceFronts/RE35.csv",
                "resources/referenceFronts/RE36.csv",
                "resources/referenceFronts/RE37.csv"), // trainingReferenceFrontFileNames
            List.of(10000, 10000, 10000, 10000, 10000, 10000, 10000), // trainingEvaluations
            List.of("Epsilon", "InvertedGenerationalDistancePlus")); // indicatorNames

    Path outputFile =
        Path.of("src/main/resources/baseLevelConfigurations/Re3dNSGAIIBaseLevel.yaml");
    BaseLevelConfigurationWriter.save(baseLevel, outputFile);
    System.out.println("Wrote " + outputFile);
  }
}
