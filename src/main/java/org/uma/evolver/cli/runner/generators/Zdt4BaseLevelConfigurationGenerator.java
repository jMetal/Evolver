package org.uma.evolver.cli.runner.generators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.cli.runner.BaseLevelConfig;
import org.uma.evolver.cli.runner.BaseLevelConfigurationWriter;

/**
 * Regenerates {@code src/main/resources/baseLevelConfigurations/Zdt4NSGAIIBaseLevel.yaml} — NSGA-II
 * tuned on problem ZDT4 — from a compiler-checked {@link BaseLevelConfig} instead of hand-editing
 * the YAML, so the field types/list sizes are validated at compile time. This file is shared by
 * {@code nsgaii-zdt4-request.yaml} and {@code async-nsgaii-zdt4-request.yaml}.
 */
public class Zdt4BaseLevelConfigurationGenerator {

  public static void main(String[] args) throws IOException {
    BaseLevelConfig baseLevel =
        new BaseLevelConfig(
            "NSGA-II", // algorithmName
            100, // populationSize
            1, // numberOfIndependentRuns
            "NSGAIIDouble.yaml", // yamlParameterSpaceFile
            null, // extraConfig
            List.of("ZDT4"), // trainingProblemNames
            List.of("resources/referenceFronts/ZDT4.csv"), // trainingReferenceFrontFileNames
            List.of(12000), // trainingEvaluations
            List.of("Epsilon", "NormalizedHypervolume")); // indicatorNames

    Path outputFile =
        Path.of("src/main/resources/baseLevelConfigurations/Zdt4NSGAIIBaseLevel.yaml");
    BaseLevelConfigurationWriter.save(baseLevel, outputFile);
    System.out.println("Wrote " + outputFile);
  }
}
