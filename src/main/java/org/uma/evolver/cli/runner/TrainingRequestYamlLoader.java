package org.uma.evolver.cli.runner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/** Loads a {@link TrainingRequest} from a YAML file, following the same raw-map style used by
 * {@code YAMLParameterSpace} rather than a typed SnakeYAML bean mapping. */
final class TrainingRequestYamlLoader {

  private TrainingRequestYamlLoader() {}

  static TrainingRequest load(Path requestFile) {
    Map<String, Object> data;
    try (InputStream inputStream = new FileInputStream(requestFile.toFile())) {
      data = new Yaml().load(inputStream);
    } catch (FileNotFoundException e) {
      throw new JMetalException("Training request file not found: " + requestFile, e);
    } catch (Exception e) {
      throw new JMetalException("Error reading training request file: " + requestFile, e);
    }

    return new TrainingRequest(
        intValue(data, "metaMaxEvaluations"),
        optionalIntValue(data, "metaPopulationSize"),
        intValue(data, "numberOfCores"),
        optionalDoubleValue(data, "mutationProbabilityFactor"),
        stringValue(data, "metaYamlParameterSpaceFile"),
        stringValue(data, "baseLevelAlgorithmName"),
        intValue(data, "baseLevelPopulationSize"),
        intValue(data, "numberOfIndependentRuns"),
        stringValue(data, "baseLevelYamlParameterSpaceFile"),
        stringMap(data.get("baseLevelExtraConfig")),
        (String) data.get("trainingSetName"),
        stringList(data.get("trainingProblemNames")),
        stringList(data.get("trainingReferenceFrontFileNames")),
        intList(data.get("trainingEvaluations")),
        stringList(require(data, "indicatorNames")),
        stringValue(data, "outputDirectory"));
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> intList(Object rawList) {
    return (List<Integer>) rawList;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> stringMap(Object rawMap) {
    return rawMap == null ? Map.of() : (Map<String, String>) rawMap;
  }

  @SuppressWarnings("unchecked")
  private static List<String> stringList(Object rawList) {
    return (List<String>) rawList;
  }

  private static int intValue(Map<String, Object> data, String key) {
    return (Integer) require(data, key);
  }

  private static Integer optionalIntValue(Map<String, Object> data, String key) {
    return (Integer) data.get(key);
  }

  private static Double optionalDoubleValue(Map<String, Object> data, String key) {
    Object value = data.get(key);
    if (value == null) {
      return null;
    }
    return value instanceof Integer integer ? integer.doubleValue() : (Double) value;
  }

  private static String stringValue(Map<String, Object> data, String key) {
    return (String) require(data, key);
  }

  private static Object require(Map<String, Object> data, String key) {
    Object value = data.get(key);
    if (value == null) {
      throw new JMetalException("Missing required field in training request: " + key);
    }
    return value;
  }
}
