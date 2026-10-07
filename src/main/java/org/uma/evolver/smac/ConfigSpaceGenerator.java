package org.uma.evolver.smac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.ConfigurationVariants;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Writes a parameter space of Evolver as a configuration space of ConfigSpace, the format that
 * SMAC3 reads ({@code ConfigurationSpace.from_json}), as {@code IraceParameterDescriptionGenerator}
 * does for irace.
 *
 * <p>Every parameter of the space becomes a hyperparameter: a categorical one (the values of a
 * categorical parameter, the integers of a categorical integer one, {@code "true"} and {@code
 * "false"} for a boolean one), a {@code uniform_int} or a {@code uniform_float} one with the range
 * of the parameter. The relations between parameters become conditions: a conditional parameter is
 * active when its parent takes the value that declares it ({@code EQ}, or {@code IN} when it is
 * declared by several values), and a global sub-parameter whenever its parent is active ({@code IN}
 * with all the values of the parent). A hyperparameter whose parent is not active is not active
 * either, so the conditions of a parameter do not need to repeat those of its ancestors.
 *
 * <p>The default value of each hyperparameter is, when a default configuration is given, its value
 * in that configuration, so that SMAC3 can start from the default configuration of the algorithm.
 * Otherwise (and for the parameters that the default configuration does not activate) it is the
 * first value of a categorical parameter and the middle of the range of a numeric one.
 *
 * <pre>{@code
 * java -cp Evolver-*-jar-with-dependencies.jar org.uma.evolver.smac.ConfigSpaceGenerator \
 *     NSGAIIDouble.yaml Double defaultConfigurations/NSGAIIDoubleDefault.txt > NSGAIIDouble.json
 * }</pre>
 */
public final class ConfigSpaceGenerator {

  /** The parameter factories accepted by {@link #main}, as for irace. */
  static final List<String> FACTORY_NAMES = List.of("Double", "Binary", "Permutation", "MOPSO");

  /** The version of the JSON format of ConfigSpace that is written. */
  static final double FORMAT_VERSION = 0.4;

  private static final String USAGE =
      "Usage: ConfigSpaceGenerator <parameterSpace.yaml> <"
          + String.join("|", FACTORY_NAMES)
          + "> [defaultConfigurationFile]";

  private final Map<String, String> hyperparameters = new LinkedHashMap<>();
  private final Map<String, Condition> conditions = new LinkedHashMap<>();
  private final Map<String, String> defaults;

  /** A condition on the value of a parent: the values that activate a child. */
  private record Condition(String child, Parameter<?> parent, List<Object> values) {}

  private ConfigSpaceGenerator(Map<String, String> defaults) {
    this.defaults = defaults;
  }

  /**
   * Prints the configuration space of a parameter space, as JSON, to standard output.
   *
   * @param args the YAML file of the parameter space, the name of its parameter factory (one of
   *     {@link #FACTORY_NAMES}) and, optionally, a file with a default configuration
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 2 && args.length != 3) {
      System.err.println(USAGE);
      System.exit(1);
    }
    String defaultConfiguration =
        args.length == 3 ? new ConfigurationFileReader(args[2]).getConfiguration(1) : null;
    var space = new YAMLParameterSpace(args[0], parameterFactory(args[1]));
    System.out.println(json(space, defaultConfiguration));
  }

  /**
   * Returns the configuration space of a parameter space, in the JSON format of ConfigSpace.
   *
   * @param parameterSpace the parameter space
   * @param defaultConfiguration a configuration of the space ({@code --name value} pairs) whose
   *     values are the defaults of the hyperparameters, or null for the defaults of ConfigSpace
   * @return the configuration space, as JSON
   * @throws JMetalException if a parameter is of a type that ConfigSpace cannot represent, or is
   *     declared twice with different domains or parents
   */
  public static String json(ParameterSpace parameterSpace, String defaultConfiguration) {
    Map<String, String> defaults =
        defaultConfiguration == null
            ? Map.of()
            : ConfigurationVariants.parameterValues(defaultConfiguration);
    var generator = new ConfigSpaceGenerator(defaults);
    for (Parameter<?> parameter : parameterSpace.topLevelParameters()) {
      generator.add(parameter);
    }
    return generator.toJson();
  }

  static ParameterFactory<?> parameterFactory(String factoryName) {
    return switch (factoryName) {
      case "Double" -> new DoubleParameterFactory();
      case "Binary" -> new BinaryParameterFactory();
      case "Permutation" -> new PermutationParameterFactory();
      case "MOPSO" -> new MOPSOParameterFactory();
      default ->
          throw new JMetalException("Unknown parameter factory: " + factoryName + ". " + USAGE);
    };
  }

  private void add(Parameter<?> parameter) {
    declare(parameter);
    for (Parameter<?> global : parameter.globalSubParameters()) {
      condition(global, parameter, values(parameter));
      add(global);
    }
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      condition(
          conditional.parameter(),
          parameter,
          List.of(value(parameter, conditional.description())));
      add(conditional.parameter());
    }
  }

  private void declare(Parameter<?> parameter) {
    String hyperparameter = hyperparameter(parameter);
    String previous = hyperparameters.putIfAbsent(parameter.name(), hyperparameter);
    if (previous != null && !previous.equals(hyperparameter)) {
      throw new JMetalException(
          "Parameter " + parameter.name() + " is declared twice with different domains");
    }
  }

  private void condition(Parameter<?> child, Parameter<?> parent, List<Object> values) {
    Condition previous = conditions.get(child.name());
    if (previous == null) {
      conditions.put(child.name(), new Condition(child.name(), parent, new ArrayList<>(values)));
    } else if (previous.parent().name().equals(parent.name())) {
      for (Object value : values) {
        if (!previous.values().contains(value)) {
          previous.values().add(value);
        }
      }
    } else {
      throw new JMetalException(
          "Parameter "
              + child.name()
              + " depends on two parents, "
              + previous.parent().name()
              + " and "
              + parent.name()
              + ", which a ConfigSpace condition cannot express");
    }
  }

  /** The values of a parent that has sub-parameters, typed as in its hyperparameter. */
  private static List<Object> values(Parameter<?> parameter) {
    if (parameter instanceof CategoricalParameter categorical) {
      return new ArrayList<>(categorical.validValues());
    }
    if (parameter instanceof CategoricalIntegerParameter categorical) {
      return new ArrayList<>(categorical.validValues());
    }
    if (parameter instanceof BooleanParameter) {
      return List.of("true", "false");
    }
    throw new JMetalException(
        "Parameter " + parameter.name() + " has sub-parameters but is not categorical");
  }

  /** A value of a parent, given as text in the space, typed as in its hyperparameter. */
  private static Object value(Parameter<?> parent, String text) {
    return parent instanceof CategoricalIntegerParameter ? Integer.valueOf(text) : text;
  }

  private String hyperparameter(Parameter<?> parameter) {
    String name = quote(parameter.name());
    String fallback = defaults.get(parameter.name());
    if (parameter instanceof CategoricalParameter categorical) {
      List<String> choices = categorical.validValues();
      String value = fallback != null && choices.contains(fallback) ? fallback : choices.get(0);
      return categorical(
          name, choices.stream().map(ConfigSpaceGenerator::quote).toList(), quote(value));
    }
    if (parameter instanceof CategoricalIntegerParameter categorical) {
      List<Integer> choices = categorical.validValues();
      Integer value =
          fallback != null && choices.contains(Integer.valueOf(fallback))
              ? Integer.valueOf(fallback)
              : choices.get(0);
      return categorical(
          name, choices.stream().map(String::valueOf).toList(), String.valueOf(value));
    }
    if (parameter instanceof BooleanParameter) {
      String value = "false".equals(fallback) ? "false" : "true";
      return categorical(name, List.of(quote("true"), quote("false")), quote(value));
    }
    if (parameter instanceof IntegerParameter integer) {
      int lower = integer.minValue();
      int upper = integer.maxValue();
      int value = fallback != null ? Integer.parseInt(fallback) : (lower + upper) / 2;
      return ("{\"type\": \"uniform_int\", \"name\": %s, \"lower\": %d, \"upper\": %d,"
              + " \"default_value\": %d, \"log\": false, \"meta\": null}")
          .formatted(name, lower, upper, value);
    }
    if (parameter instanceof DoubleParameter real) {
      double lower = real.minValue();
      double upper = real.maxValue();
      double value = fallback != null ? Double.parseDouble(fallback) : (lower + upper) / 2.0;
      return ("{\"type\": \"uniform_float\", \"name\": %s, \"lower\": %s, \"upper\": %s,"
              + " \"default_value\": %s, \"log\": false, \"meta\": null}")
          .formatted(name, lower, upper, value);
    }
    throw new JMetalException(
        "Parameter "
            + parameter.name()
            + " is of a type that ConfigSpace cannot represent: "
            + parameter.getClass().getSimpleName());
  }

  private static String categorical(String name, List<String> choices, String value) {
    return ("{\"type\": \"categorical\", \"name\": %s, \"choices\": [%s], \"weights\": null,"
            + " \"default_value\": %s, \"meta\": null}")
        .formatted(name, String.join(", ", choices), value);
  }

  private String toJson() {
    List<String> conditionList = new ArrayList<>();
    for (Condition condition : conditions.values()) {
      List<String> values =
          condition.values().stream()
              .map(v -> v instanceof Integer ? v.toString() : quote((String) v))
              .toList();
      conditionList.add(
          values.size() == 1
              ? "{\"type\": \"EQ\", \"child\": %s, \"parent\": %s, \"value\": %s}"
                  .formatted(
                      quote(condition.child()), quote(condition.parent().name()), values.get(0))
              : "{\"type\": \"IN\", \"child\": %s, \"parent\": %s, \"values\": [%s]}"
                  .formatted(
                      quote(condition.child()),
                      quote(condition.parent().name()),
                      String.join(", ", values)));
    }
    return "{\n  \"name\": null,\n  \"hyperparameters\": [\n    "
        + String.join(",\n    ", hyperparameters.values())
        + "\n  ],\n  \"conditions\": [\n    "
        + String.join(",\n    ", conditionList)
        + "\n  ],\n  \"forbiddens\": [],\n  \"python_module_version\": \"1.2.0\","
        + "\n  \"format_version\": "
        + FORMAT_VERSION
        + "\n}";
  }

  /** A JSON string. */
  static String quote(String text) {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
