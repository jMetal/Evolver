package org.uma.evolver.cli.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link MetaSearchConfig} from a meta-optimizer configuration file by name (e.g. {@code
 * MetaNSGAIIFlatConfiguration.yaml}, under {@code
 * src/main/resources/metaOptimizerConfigurations/}) — a reusable, named recipe for the
 * meta-optimizer, the same way a base-level algorithm's parameter space
 * (a {@code parameterSpaces/*.yaml} file) and its pre-tuned defaults
 * (a {@code defaultConfigurations/*.txt} file) are named, reusable resources rather than content
 * inlined in every {@link TrainingRequest}.
 *
 * <p>Public: used both by {@link TrainingRequestYamlLoader} (the {@code metaSearch} field of a
 * request YAML is one of these file names) and directly by {@code cli.training.generators}, for
 * requests built as plain Java objects. {@link #loadFromYaml(String)} is the same parsing given
 * the YAML content directly instead of a file name.
 */
public final class MetaOptimizerConfigurationReader {

  private static final String RESOURCE_DIRECTORY = "metaOptimizerConfigurations/";

  /**
   * Keys consumed directly as {@link FlatMetaSearchConfig}/{@link TreeMetaSearchConfig} fields;
   * everything else in a meta-optimizer configuration file is an operator flag (see {@link
   * FlatMetaSearchConfig#operatorFlags()}).
   */
  private static final Set<String> SCALAR_KEYS =
      Set.of("algorithm", "encoding", "metaMaxEvaluations", "metaPopulationSize", "numberOfCores");

  private MetaOptimizerConfigurationReader() {}

  public static MetaSearchConfig load(String fileName) {
    return build(loadYaml(fileName), fileName);
  }

  public static MetaSearchConfig loadFromYaml(String yamlText) {
    return build(new Yaml().load(new StringReader(yamlText)), "<inline YAML>");
  }

  private static MetaSearchConfig build(Map<String, Object> data, String label) {
    String encoding = stringValue(data, label, "encoding");
    return switch (encoding) {
      case "flat" ->
          new FlatMetaSearchConfig(
              stringValue(data, label, "algorithm"),
              intValue(data, label, "metaMaxEvaluations"),
              optionalIntValue(data, "metaPopulationSize"),
              intValue(data, label, "numberOfCores"),
              operatorFlags(data));
      case "tree" -> {
        rejectOffspringSize(data, label);
        yield new TreeMetaSearchConfig(
              stringValue(data, label, "algorithm"),
              intValue(data, label, "metaMaxEvaluations"),
            intValue(data, "metaPopulationSize", MetaAlgorithmRegistry.DEFAULT_POPULATION_SIZE),
            intValue(data, label, "numberOfCores"),
            operatorFlags(data));
      }
      default ->
          throw new JMetalException(
              "Unknown encoding in meta-optimizer configuration '"
                  + label
                  + "': "
                  + encoding
                  + ". Expected flat or tree");
    };
  }

  /**
   * The tree pipeline reads only known keys, so a leftover {@code metaOffspringSize} would be
   * silently ignored — fail instead, since the offspring size is no longer configurable.
   */
  private static void rejectOffspringSize(Map<String, Object> data, String label) {
    if (data.containsKey("metaOffspringSize")) {
      throw new JMetalException(
          "Unexpected field in meta-optimizer configuration '"
              + label
              + "': metaOffspringSize. The offspring population size always equals"
              + " metaPopulationSize");
    }
  }

  private static List<String> operatorFlags(Map<String, Object> data) {
    List<String> flags = new ArrayList<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (!SCALAR_KEYS.contains(entry.getKey())) {
        flags.add("--" + entry.getKey());
        flags.add(String.valueOf(entry.getValue()));
      }
    }
    return flags;
  }

  private static Map<String, Object> loadYaml(String fileName) {
    try (InputStream inputStream = openConfigFile(fileName)) {
      return new Yaml().load(inputStream);
    } catch (Exception e) {
      // JMetalException(String, Exception) does not call super(message), leaving getMessage()
      // null — use the single-String constructor so the message is actually preserved.
      throw new JMetalException(
          "Error reading meta-optimizer configuration file: " + fileName + ". " + e.getMessage());
    }
  }

  private static InputStream openConfigFile(String fileName) throws FileNotFoundException {
    InputStream classpathStream =
        MetaOptimizerConfigurationReader.class
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
        "Meta-optimizer configuration file not found. Tried classpath resource '"
            + RESOURCE_DIRECTORY
            + fileName
            + "', '"
            + sourceResourceFile.getPath()
            + "' and '"
            + fileName
            + "'.");
  }

  private static int intValue(Map<String, Object> data, String label, String key) {
    return (Integer) require(data, label, key);
  }

  private static int intValue(Map<String, Object> data, String key, int defaultValue) {
    Integer value = optionalIntValue(data, key);
    return value == null ? defaultValue : value;
  }

  private static Integer optionalIntValue(Map<String, Object> data, String key) {
    return (Integer) data.get(key);
  }

  private static String stringValue(Map<String, Object> data, String label, String key) {
    return (String) require(data, label, key);
  }

  private static Object require(Map<String, Object> data, String label, String key) {
    Object value = data.get(key);
    if (value == null) {
      throw new JMetalException(
          "Missing required field in meta-optimizer configuration '" + label + "': " + key);
    }
    return value;
  }
}
