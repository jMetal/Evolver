package org.uma.evolver.analysis.ablation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses configuration strings into a key-value map.
 * Expected format: "--key1 value1 --key2 value2"
 */
public class AblationConfigParser {

  private AblationConfigParser() {
    // Utility class
  }

  /**
   * Parses a configuration string into a map.
   *
   * @param configString The configuration string (e.g., "--popSize 100 --algo
   *                     NSGAII")
   * @return A map of parameter names to values.
   */
  public static Map<String, String> parse(String configString) {
    var config = new LinkedHashMap<String, String>();
    if (configString == null || configString.trim().isEmpty()) {
      return config;
    }

    String[] tokens = configString.trim().split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].startsWith("--")) {
        String key = tokens[i].substring(2);
        if (i + 1 < tokens.length && !tokens[i + 1].startsWith("--")) {
          config.put(key, tokens[i + 1]);
          i++; // Skip value
        } else {
          // Flag without value (boolean true implicit? or error?)
          // For Evolver parameters, usually everything has a value.
          // We'll assume empty string or handle it if needed.
          // Assuming well-formed input for now based on example.
        }
      }
    }
    return config;
  }

  /**
   * Converts a configuration map back to a string.
   */
  public static String toString(Map<String, String> config) {
    return config.entrySet().stream()
        .map(e -> "--" + e.getKey() + " " + e.getValue())
        .collect(Collectors.joining(" "));
  }
}
