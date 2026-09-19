package org.uma.evolver.cli.training;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Writes a {@link BaseLevelConfig} out to a base-level configuration file readable by
 * {@link BaseLevelConfigurationReader} — the counterpart that lets {@code cli.training.generators}
 * build a {@link BaseLevelConfig} in Java (compiler-checked field types and list sizes) and turn
 * it into the same kind of reusable, named file a hand-written one would be, instead of keeping
 * the values duplicated between Java source and a YAML fixture.
 */
public final class BaseLevelConfigurationWriter {

  private BaseLevelConfigurationWriter() {}

  public static void save(BaseLevelConfig config, Path outputFile) throws IOException {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("algorithmName", config.algorithmName());
    data.put("populationSize", config.populationSize());
    data.put("numberOfIndependentRuns", config.numberOfIndependentRuns());
    data.put("yamlParameterSpaceFile", config.yamlParameterSpaceFile());
    if (config.extraConfig() != null && !config.extraConfig().isEmpty()) {
      data.put("extraConfig", config.extraConfig());
    }
    data.put(
        "trainingProblemNames",
        config.trainingProblemNames().stream().map(BaseLevelConfigurationWriter::toYamlValue).toList());
    data.put("trainingReferenceFrontFileNames", config.trainingReferenceFrontFileNames());
    data.put("trainingEvaluations", config.trainingEvaluations());
    data.put("indicatorNames", config.indicatorNames());

    try (FileWriter writer = new FileWriter(outputFile.toFile())) {
      new Yaml().dump(data, writer);
    }
  }

  private static Object toYamlValue(ProblemSpec spec) {
    if (spec.args().isEmpty()) {
      return spec.className();
    }
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("class", spec.className());
    entry.put("args", spec.args());
    return entry;
  }
}
