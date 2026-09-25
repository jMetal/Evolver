package org.uma.evolver.cli.solving;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link SolveRequest} from a YAML file, in the same raw-map style as the training request
 * loader. Unlike a training request, a solve request is a single, self-contained file: its
 * top-level entries are the fields of {@link SolveRequest}.
 *
 * <p>The configuration is given either inline ({@code configuration}) or as a file ({@code
 * configurationFile}, e.g. {@code defaultConfigurations/NSGAIIDoubleDefault.txt}, whose first
 * configuration is used), never both. {@code indicatorNames} requires {@code
 * referenceFrontFileName}.
 */
public final class SolveRequestYamlLoader {

  /** Encoding used when the request does not give one, as in a training request. */
  public static final String DEFAULT_ENCODING = "Double";

  /** Number of independent runs used when the request does not give one. */
  public static final int DEFAULT_NUMBER_OF_INDEPENDENT_RUNS = 1;

  private SolveRequestYamlLoader() {}

  public static SolveRequest load(Path requestFile) {
    Map<String, Object> data;
    try (InputStream inputStream = new FileInputStream(requestFile.toFile())) {
      data = new Yaml().load(inputStream);
    } catch (FileNotFoundException e) {
      throw new JMetalException("Solve request file not found: " + requestFile);
    } catch (Exception e) {
      throw new JMetalException(
          "Error reading solve request file: " + requestFile + ". " + e.getMessage());
    }
    if (data == null) {
      throw new JMetalException("Empty solve request file: " + requestFile);
    }

    String configurationFile = (String) data.get("configurationFile");
    String referenceFrontFileName = (String) data.get("referenceFrontFileName");
    List<String> indicatorNames = stringList(data.get("indicatorNames"));
    if (!indicatorNames.isEmpty() && referenceFrontFileName == null) {
      throw new JMetalException(
          "indicatorNames requires referenceFrontFileName in solve request: " + requestFile);
    }

    return new SolveRequest(
        stringValue(data, "algorithmName"),
        data.get("encoding") == null ? DEFAULT_ENCODING : (String) data.get("encoding"),
        intValue(data, "populationSize"),
        stringValue(data, "yamlParameterSpaceFile"),
        stringMap(data.get("extraConfig")),
        configuration(data, configurationFile),
        configurationFile,
        ProblemSpec.fromYamlValue(require(data, "problem"), "problem in '" + requestFile + "'"),
        referenceFrontFileName,
        intValue(data, "maxEvaluations"),
        data.get("numberOfIndependentRuns") == null
            ? DEFAULT_NUMBER_OF_INDEPENDENT_RUNS
            : (Integer) data.get("numberOfIndependentRuns"),
        data.get("seed") == null ? null : ((Number) data.get("seed")).longValue(),
        indicatorNames,
        stringValue(data, "outputDirectory"));
  }

  private static String configuration(Map<String, Object> data, String configurationFile) {
    String configuration = (String) data.get("configuration");
    if ((configuration == null) == (configurationFile == null)) {
      throw new JMetalException(
          "A solve request needs exactly one of 'configuration' and 'configurationFile'");
    }
    if (configuration != null) {
      return configuration.trim();
    }
    try {
      return new ConfigurationFileReader(configurationFile).getConfiguration(1);
    } catch (IOException e) {
      throw new JMetalException(e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> stringMap(Object rawMap) {
    return rawMap == null ? Map.of() : (Map<String, String>) rawMap;
  }

  @SuppressWarnings("unchecked")
  private static List<String> stringList(Object rawList) {
    return rawList == null ? List.of() : (List<String>) rawList;
  }

  private static int intValue(Map<String, Object> data, String key) {
    return (Integer) require(data, key);
  }

  private static String stringValue(Map<String, Object> data, String key) {
    return (String) require(data, key);
  }

  private static Object require(Map<String, Object> data, String key) {
    Object value = data.get(key);
    if (value == null) {
      throw new JMetalException("Missing required field in solve request: " + key);
    }
    return value;
  }
}
