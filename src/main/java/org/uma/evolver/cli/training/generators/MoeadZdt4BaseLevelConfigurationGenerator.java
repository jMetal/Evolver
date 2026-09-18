package org.uma.evolver.cli.training.generators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationWriter;

/**
 * Regenerates {@code src/main/resources/baseLevelConfigurations/MoeadZdt4BaseLevel.yaml} — MOEA/D
 * tuned on problem ZDT4 — from a compiler-checked {@link BaseLevelConfig}. Exercises a base-level
 * algorithm other than NSGA-II, which needs extra algorithm-specific configuration
 * ({@code weightVectorFilesDirectory}) carried in {@code extraConfig}.
 */
public class MoeadZdt4BaseLevelConfigurationGenerator {

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "MOEAD", // algorithmName
            100, // populationSize
            1, // numberOfIndependentRuns
            "MOEADDouble.yaml", // yamlParameterSpaceFile
            Map.of("weightVectorFilesDirectory", "resources/weightVectors"),
            List.of("ZDT4"), // trainingProblemNames
            List.of("resources/referenceFronts/ZDT4.csv"), // trainingReferenceFrontFileNames
            List.of(10000), // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume")); // indicatorNames

    Path outputFile =
        Path.of("src/main/resources/baseLevelConfigurations/MoeadZdt4BaseLevel.yaml");
    BaseLevelConfigurationWriter.save(baseLevel, outputFile);
    System.out.println("Wrote " + outputFile);
  }
}
