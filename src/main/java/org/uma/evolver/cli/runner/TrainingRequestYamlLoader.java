package org.uma.evolver.cli.runner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link TrainingRequest} from a YAML file, following the same raw-map style used by
 * {@code YAMLParameterSpace} rather than a typed SnakeYAML bean mapping.
 *
 * <p>The YAML file has two top-level sections, mirroring the {@link BaseLevelConfig} /
 * {@link MetaSearchConfig} split: {@code baseLevel} and {@code metaSearch}. {@code metaSearch}
 * must have an {@code encoding} field ({@code "flat"} or {@code "tree"}) selecting which of
 * {@link FlatMetaSearchConfig} or {@link TreeMetaSearchConfig} to build.
 */
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
        loadBaseLevel(mapValue(data, "baseLevel")), loadMetaSearch(mapValue(data, "metaSearch")));
  }

  private static BaseLevelConfig loadBaseLevel(Map<String, Object> data) {
    return new BaseLevelConfig(
        stringValue(data, "algorithmName"),
        intValue(data, "populationSize"),
        intValue(data, "numberOfIndependentRuns"),
        stringValue(data, "yamlParameterSpaceFile"),
        stringMap(data.get("extraConfig")),
        stringList(require(data, "trainingProblemNames")),
        stringList(require(data, "trainingReferenceFrontFileNames")),
        intList(require(data, "trainingEvaluations")),
        stringList(require(data, "indicatorNames")),
        stringValue(data, "outputDirectory"));
  }

  private static MetaSearchConfig loadMetaSearch(Map<String, Object> data) {
    String encoding = stringValue(data, "encoding");
    return switch (encoding) {
      case "flat" ->
          new FlatMetaSearchConfig(
              intValue(data, "metaMaxEvaluations"),
              optionalIntValue(data, "metaPopulationSize"),
              intValue(data, "numberOfCores"),
              optionalDoubleValue(data, "mutationProbabilityFactor"),
              stringValue(data, "metaYamlParameterSpaceFile"));
      case "tree" ->
          new TreeMetaSearchConfig(
              intValue(data, "metaMaxEvaluations"),
              intValue(data, "metaPopulationSize"),
              intValue(data, "metaOffspringSize"),
              intValue(data, "numberOfCores"),
              doubleValue(data, "crossoverProbability"),
              doubleValue(data, "mutationProbability"),
              doubleValue(data, "mutationDistributionIndex"));
      default ->
          throw new JMetalException("Unknown metaSearch.encoding: " + encoding + ". Expected flat or tree");
    };
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapValue(Map<String, Object> data, String key) {
    return (Map<String, Object>) require(data, key);
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

  private static int intValue(Map<String, Object> data, String key) {
    return (Integer) require(data, key);
  }

  private static Integer optionalIntValue(Map<String, Object> data, String key) {
    return (Integer) data.get(key);
  }

  private static double doubleValue(Map<String, Object> data, String key) {
    Object value = require(data, key);
    return value instanceof Integer integer ? integer.doubleValue() : (Double) value;
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
