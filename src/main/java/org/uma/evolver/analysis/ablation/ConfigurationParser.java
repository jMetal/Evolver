package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses configuration strings into argument maps and arrays.
 */
public final class ConfigurationParser {

  private ConfigurationParser() {
    // Utility class
  }

  /**
   * Parses a configuration string into a map of parameter names to values.
   *
   * @param configuration the configuration string
   * @return a map of parameter names to values
   * @throws IllegalArgumentException if the configuration is malformed
   */
  public static Map<String, String> parse(String configuration) {
    Map<String, String> result = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    List<String> tokens = tokenize(configuration);

    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      if (token.startsWith("--")) {
        String key;
        String value;
        int equalsIndex = token.indexOf('=');
        if (equalsIndex > 2) {
          key = token.substring(2, equalsIndex);
          value = token.substring(equalsIndex + 1);
        } else if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("--")) {
          key = token.substring(2);
          value = tokens.get(i + 1);
          i++;
        } else {
          errors.add("Missing value for parameter: " + token);
          continue;
        }

        if (key.isBlank()) {
          errors.add("Empty parameter name in token: " + token);
        } else {
          result.put(key, value);
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(System.lineSeparator(), errors));
    }

    return result;
  }

  /**
   * Converts a configuration map to a command-line argument array.
   *
   * @param configuration the configuration map
   * @return an argument array for parsing by {@code Parameter} instances
   * @throws IllegalArgumentException if configuration is null
   */
  public static String[] toArgs(Map<String, String> configuration) {
    String[] result;
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration map cannot be null");
    }

    List<String> args = new ArrayList<>();
    for (var entry : configuration.entrySet()) {
      args.add("--" + entry.getKey());
      args.add(entry.getValue());
    }
    result = args.toArray(new String[0]);
    return result;
  }

  private static List<String> tokenize(String configuration) {
    List<String> result = new ArrayList<>();
    boolean hasContent = configuration != null && !configuration.isBlank();

    if (hasContent) {
      StringBuilder current = new StringBuilder();
      boolean inSingleQuote = false;
      boolean inDoubleQuote = false;

      for (int i = 0; i < configuration.length(); i++) {
        char ch = configuration.charAt(i);
        if (ch == '\'' && !inDoubleQuote) {
          inSingleQuote = !inSingleQuote;
        } else if (ch == '"' && !inSingleQuote) {
          inDoubleQuote = !inDoubleQuote;
        } else if (Character.isWhitespace(ch) && !inSingleQuote && !inDoubleQuote) {
          if (current.length() > 0) {
            result.add(current.toString());
            current.setLength(0);
          }
        } else {
          current.append(ch);
        }
      }

      if (inSingleQuote || inDoubleQuote) {
        throw new IllegalArgumentException("Unterminated quote in configuration string");
      }

      if (current.length() > 0) {
        result.add(current.toString());
      }
    }
    return result;
  }
}
