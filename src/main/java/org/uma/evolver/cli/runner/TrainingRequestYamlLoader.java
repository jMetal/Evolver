package org.uma.evolver.cli.runner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link TrainingRequest} from a YAML file, following the same raw-map style used by
 * {@code YAMLParameterSpace} rather than a typed SnakeYAML bean mapping.
 *
 * <p>The YAML file has up to six top-level entries, none of them inline: {@code baseLevel} and
 * {@code metaSearch} are the *names* of separate, reusable configuration files (loaded via
 * {@link BaseLevelConfigurationReader}/{@link MetaOptimizerConfigurationReader}); {@code
 * outputDirectory} is a required plain string; {@code writeFrequency}/{@code statusFrequency} are
 * optional plain integers (default {@value #DEFAULT_FREQUENCY}); {@code frontPlotFrequency} is an
 * optional plain integer with no default — absent means no live plot (see {@link TrainingRequest}).
 * All five are specific to this one run, never shared with any other request, even one reusing the
 * exact same {@code baseLevel}/{@code metaSearch} files.
 */
final class TrainingRequestYamlLoader {

  private static final int DEFAULT_FREQUENCY = 100;

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
        BaseLevelConfigurationReader.load(stringValue(data, "baseLevel")),
        MetaOptimizerConfigurationReader.load(stringValue(data, "metaSearch")),
        stringValue(data, "outputDirectory"),
        optionalIntValue(data, "writeFrequency", DEFAULT_FREQUENCY),
        optionalIntValue(data, "statusFrequency", DEFAULT_FREQUENCY),
        (Integer) data.get("frontPlotFrequency"));
  }

  private static String stringValue(Map<String, Object> data, String key) {
    Object value = data.get(key);
    if (value == null) {
      throw new JMetalException("Missing required field in training request: " + key);
    }
    return (String) value;
  }

  private static int optionalIntValue(Map<String, Object> data, String key, int defaultValue) {
    Object value = data.get(key);
    return value == null ? defaultValue : (Integer) value;
  }
}
