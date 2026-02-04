package org.uma.evolver.analysis.ablation;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Legacy wrapper for {@link ConfigurationParser}.
 *
 * @deprecated use {@link ConfigurationParser} instead.
 */
@Deprecated
public class AblationConfigParser {

  private AblationConfigParser() {
    // Utility class
  }

  /**
   * Parses a configuration string into a map.
   *
   * @param configString configuration string
   * @return map of parameter names to values
   */
  public static Map<String, String> parse(String configString) {
    Map<String, String> result;
    result = ConfigurationParser.parse(configString);
    return result;
  }

  /**
   * Converts a configuration map back to a string.
   *
   * @param config configuration map
   * @return configuration string
   */
  public static String toString(Map<String, String> config) {
    String result;
    result = config.entrySet().stream()
        .map(e -> "--" + e.getKey() + " " + e.getValue())
        .collect(Collectors.joining(" "));
    return result;
  }
}
