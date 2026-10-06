package org.uma.evolver.parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Derives variants of a configuration by fixing some of its parameters, keeping the result valid
 * for its parameter space: the tool of an ablation study, where a tuned configuration is compared
 * with copies of itself in which a few components are set to other values (no external archive, the
 * default crossover, ...).
 *
 * <p>Fixing a parameter can change which other parameters are active. Setting {@code crossover} from
 * {@code SBX} to {@code blxAlpha} deactivates {@code sbxDistributionIndex} and activates {@code
 * blxAlphaCrossoverAlpha}; setting {@code algorithmResult} to {@code population} deactivates the
 * archive parameters. The variant has exactly the parameters that are active for its values, in the
 * order of the parameter space: the value of each one is the one fixed, or else the one in the
 * configuration, or else the one in a fallback configuration (usually the default configuration of
 * the algorithm), which provides the parameters that the change activates.
 *
 * <pre>{@code
 * var space = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
 * String withoutArchive =
 *     ConfigurationVariants.derive(space, tuned, Map.of("algorithmResult", "population"), defaults);
 * }</pre>
 */
public final class ConfigurationVariants {

  private ConfigurationVariants() {}

  /**
   * Derives a variant of a configuration.
   *
   * @param space the parameter space of the configuration
   * @param configuration the configuration, as {@code --name value} pairs
   * @param changes the parameters to fix, by name, and their values
   * @param fallback the configuration whose values are used for the parameters that become active
   *     and are neither in {@code changes} nor in {@code configuration}; may be empty
   * @return the variant, as {@code --name value} pairs separated by spaces
   * @throws IllegalArgumentException if a parameter of {@code changes} is not in the space or is not
   *     active in the variant, if a value is not valid for its parameter, or if a parameter that
   *     becomes active has no value anywhere
   */
  public static String derive(
      ParameterSpace space, String configuration, Map<String, String> changes, String fallback) {
    Map<String, String> current = parameterValues(configuration);
    Map<String, String> defaults = parameterValues(fallback);
    for (String name : changes.keySet()) {
      if (!space.parameters().containsKey(name)) {
        throw new IllegalArgumentException(
            "Parameter " + name + " is not in the parameter space");
      }
    }

    Map<String, String> variant = new LinkedHashMap<>();
    List<String> missing = new ArrayList<>();
    for (Parameter<?> parameter : space.topLevelParameters()) {
      collect(parameter, changes, current, defaults, variant, missing);
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
          "The variant activates parameters with no value in the configuration or the fallback: "
              + String.join(", ", missing)
              + "; give them a value in the changes");
    }
    Set<String> inactive = new TreeSet<>(changes.keySet());
    inactive.removeAll(variant.keySet());
    if (!inactive.isEmpty()) {
      throw new IllegalArgumentException(
          "Parameters fixed but not active in the variant: "
              + String.join(", ", inactive)
              + " (the value of the parameter they depend on does not activate them)");
    }
    validate(space, variant);

    StringBuilder result = new StringBuilder();
    variant.forEach(
        (name, value) -> result.append(result.isEmpty() ? "" : " ").append("--").append(name)
            .append(' ').append(value));
    return result.toString();
  }

  /**
   * Splits a configuration into its {@code --name value} pairs.
   *
   * @param configuration a configuration, as {@code --name value} pairs; may be null or blank
   * @return the value of each parameter, by name, in the order of the configuration
   * @throws IllegalArgumentException if the configuration is not a list of such pairs
   */
  public static Map<String, String> parameterValues(String configuration) {
    Map<String, String> values = new LinkedHashMap<>();
    if (configuration == null || configuration.isBlank()) {
      return values;
    }
    String[] tokens = configuration.trim().split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      if (!tokens[i].startsWith("--") || i + 1 >= tokens.length) {
        throw new IllegalArgumentException(
            "A configuration is a list of --name value pairs, but it has '" + tokens[i] + "'");
      }
      values.put(tokens[i].substring(2), tokens[++i]);
    }
    return values;
  }

  private static void collect(
      Parameter<?> parameter,
      Map<String, String> changes,
      Map<String, String> current,
      Map<String, String> defaults,
      Map<String, String> variant,
      List<String> missing) {
    String name = parameter.name();
    String value =
        changes.containsKey(name)
            ? changes.get(name)
            : current.containsKey(name) ? current.get(name) : defaults.get(name);
    if (value == null) {
      missing.add(name);
      return;
    }
    variant.put(name, value);
    for (Parameter<?> global : parameter.globalSubParameters()) {
      collect(global, changes, current, defaults, variant, missing);
    }
    for (Parameter<?> conditional : parameter.findConditionalParameters(value)) {
      collect(conditional, changes, current, defaults, variant, missing);
    }
  }

  /**
   * Checks the values of the variant by parsing it with a fresh copy of the space: parsing a
   * top-level parameter also parses the sub-parameters it activates.
   */
  private static void validate(ParameterSpace space, Map<String, String> variant) {
    List<String> arguments = new ArrayList<>();
    variant.forEach(
        (name, value) -> {
          arguments.add("--" + name);
          arguments.add(value);
        });
    String[] args = arguments.toArray(String[]::new);
    for (Parameter<?> parameter : space.createInstance().topLevelParameters()) {
      try {
        parameter.parse(args);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Invalid variant: " + e.getMessage(), e);
      }
    }
  }
}
