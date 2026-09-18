package org.uma.evolver.cli.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link BaseLevelConfig} from a base-level configuration file by name (e.g. {@code
 * Zdt4NSGAIIBaseLevel.yaml}, under {@code src/main/resources/baseLevelConfigurations/}) — a
 * reusable, named description of what is being tuned and on what training set, the same way a
 * meta-optimizer configuration file is a reusable, named recipe (see
 * {@link MetaOptimizerConfigurationReader}). {@code outputDirectory} is deliberately not part of
 * this file: it is specific to a single run (two requests can share the exact same base-level
 * setup while writing results to different places), so it lives on {@link TrainingRequest} itself.
 *
 * <p>Public: used both by {@link TrainingRequestYamlLoader} (the {@code baseLevel} field of a
 * request YAML is one of these file names) and directly by {@code cli.training.generators}.
 * {@link #loadFromYaml(String)} is the same parsing/validation, given the YAML content directly
 * instead of a file name — e.g. for a standalone {@code org.uma.evolver.example.training} class
 * that would rather keep its configuration visible as a Java text block than in a separate file.
 */
public final class BaseLevelConfigurationReader {

  private static final String RESOURCE_DIRECTORY = "baseLevelConfigurations/";

  private BaseLevelConfigurationReader() {}

  public static BaseLevelConfig load(String fileName) {
    return build(loadYaml(fileName), fileName);
  }

  public static BaseLevelConfig loadFromYaml(String yamlText) {
    return build(new Yaml().load(new StringReader(yamlText)), "<inline YAML>");
  }

  private static BaseLevelConfig build(Map<String, Object> data, String label) {
    return new BaseLevelConfig(
        stringValue(data, label, "algorithmName"),
        intValue(data, label, "populationSize"),
        intValue(data, label, "numberOfIndependentRuns"),
        stringValue(data, label, "yamlParameterSpaceFile"),
        stringMap(data.get("extraConfig")),
        stringList(require(data, label, "trainingProblemNames")),
        stringList(require(data, label, "trainingReferenceFrontFileNames")),
        intList(require(data, label, "trainingEvaluations")),
        stringList(require(data, label, "indicatorNames")));
  }

  private static Map<String, Object> loadYaml(String fileName) {
    try (InputStream inputStream = openConfigFile(fileName)) {
      return new Yaml().load(inputStream);
    } catch (Exception e) {
      // JMetalException(String, Exception) does not call super(message), leaving getMessage()
      // null — use the single-String constructor so the message is actually preserved.
      throw new JMetalException(
          "Error reading base-level configuration file: " + fileName + ". " + e.getMessage());
    }
  }

  private static InputStream openConfigFile(String fileName) throws FileNotFoundException {
    InputStream classpathStream =
        BaseLevelConfigurationReader.class
            .getClassLoader()
            .getResourceAsStream(RESOURCE_DIRECTORY + fileName);
    if (classpathStream != null) {
      return classpathStream;
    }

    File sourceResourceFile = new File("src/main/resources/" + RESOURCE_DIRECTORY + fileName);
    if (sourceResourceFile.exists()) {
      return new FileInputStream(sourceResourceFile);
    }

    File rawFile = new File(fileName);
    if (rawFile.exists()) {
      return new FileInputStream(rawFile);
    }

    throw new FileNotFoundException(
        "Base-level configuration file not found. Tried classpath resource '"
            + RESOURCE_DIRECTORY
            + fileName
            + "', '"
            + sourceResourceFile.getPath()
            + "' and '"
            + fileName
            + "'.");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> stringMap(Object rawMap) {
    return rawMap == null ? Map.of() : (Map<String, String>) rawMap;
  }

  @SuppressWarnings("unchecked")
  private static List<String> stringList(Object rawList) {
    return (List<String>) rawList;
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> intList(Object rawList) {
    return (List<Integer>) rawList;
  }

  private static int intValue(Map<String, Object> data, String label, String key) {
    return (Integer) require(data, label, key);
  }

  private static String stringValue(Map<String, Object> data, String label, String key) {
    return (String) require(data, label, key);
  }

  private static Object require(Map<String, Object> data, String label, String key) {
    Object value = data.get(key);
    if (value == null) {
      throw new JMetalException(
          "Missing required field in base-level configuration '" + label + "': " + key);
    }
    return value;
  }
}
