package org.uma.evolver.meta.encoding.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.encoding.solution.TreeNode;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Encodes a configuration of a base-level algorithm (the {@code --name value} pairs that the
 * meta-optimization problems decode) as a solution of the meta-optimizer, so that a training can
 * start from known configurations (the {@code initialConfigurations} of a meta-optimizer file).
 *
 * <ul>
 *   <li>Flat encoding: the inverse of {@link ParameterManagement#decodeParameter}. A categorical
 *       value of index {@code i} among {@code n} goes to the middle of its interval, {@code (i +
 *       0.5) / n}; an integer to the middle of its interval as well; a double to its position in
 *       the range; a boolean to 0.25 or 0.75. The variables of the parameters that the
 *       configuration does not activate are random, as in any solution.
 *   <li>Tree encoding: the derivation tree of the configuration, built as {@link
 *       TreeSolutionGenerator} builds a random one, with the values of the configuration.
 * </ul>
 *
 * <p>Decoding the result gives the configuration back (doubles within rounding); the configuration
 * must be valid in the parameter space, which the caller checks by parsing it.
 */
public final class ConfigurationEncoding {

  private ConfigurationEncoding() {}

  /** The {@code --name value} pairs of a configuration, by parameter name. */
  public static Map<String, String> values(String[] configuration) {
    Map<String, String> values = new HashMap<>();
    for (int i = 0; i + 1 < configuration.length; i += 2) {
      String name =
          configuration[i].startsWith("--") ? configuration[i].substring(2) : configuration[i];
      values.put(name, configuration[i + 1]);
    }
    return values;
  }

  /**
   * The variables of the flat encoding of a configuration.
   *
   * @param configuration the {@code --name value} pairs
   * @param parameters the flattened parameters of the meta-optimization problem, in its order
   * @return a value in [0, 1] for each parameter
   */
  public static List<Double> toFlat(String[] configuration, List<Parameter<?>> parameters) {
    Map<String, String> values = values(configuration);
    JMetalRandom random = JMetalRandom.getInstance();
    List<Double> variables = new ArrayList<>(parameters.size());
    for (Parameter<?> parameter : parameters) {
      String value = values.get(parameter.name());
      variables.add(value == null ? random.nextDouble() : encode(parameter, value));
    }
    return variables;
  }

  private static double encode(Parameter<?> parameter, String value) {
    if (parameter instanceof CategoricalParameter categorical) {
      return middle(indexOf(categorical.validValues(), value, parameter), categorical.validValues().size());
    } else if (parameter instanceof CategoricalIntegerParameter categorical) {
      int index = categorical.validValues().indexOf(Integer.parseInt(value));
      return middle(index < 0 ? fail(parameter, value) : index, categorical.validValues().size());
    } else if (parameter instanceof IntegerParameter integer) {
      int v = Integer.parseInt(value);
      int min = integer.minValue();
      int max = integer.maxValue();
      if (v < min || v > max) {
        fail(parameter, value);
      }
      return middle(v - min, max - min + 1);
    } else if (parameter instanceof DoubleParameter real) {
      double v = Double.parseDouble(value);
      double min = real.minValue();
      double max = real.maxValue();
      if (v < min || v > max) {
        fail(parameter, value);
      }
      return max == min ? 0.5 : (v - min) / (max - min);
    } else if (parameter instanceof BooleanParameter) {
      return Boolean.parseBoolean(value) ? 0.75 : 0.25;
    }
    throw new JMetalException("The parameter is non-configurable: " + parameter.name());
  }

  private static double middle(int index, int size) {
    return (index + 0.5) / size;
  }

  private static int indexOf(List<String> validValues, String value, Parameter<?> parameter) {
    int index = validValues.indexOf(value);
    return index < 0 ? fail(parameter, value) : index;
  }

  private static int fail(Parameter<?> parameter, String value) {
    throw new JMetalException(
        "The value " + value + " of " + parameter.name() + " is not in its parameter space");
  }

  /**
   * The derivation tree of a configuration.
   *
   * @param configuration the {@code --name value} pairs
   * @param parameterSpace the parameter space of the base-level algorithm (its grammar)
   * @param numberOfObjectives the number of objectives of the meta-optimization problem
   * @return the tree whose decoding is the configuration
   * @throws JMetalException if a parameter that the configuration activates has no value in it
   */
  public static DerivationTreeSolution toTree(
      String[] configuration, ParameterSpace parameterSpace, int numberOfObjectives) {
    Map<String, String> values = values(configuration);
    DerivationTreeSolution solution = new DerivationTreeSolution(numberOfObjectives, 0);
    for (Parameter<?> topLevel : parameterSpace.topLevelParameters()) {
      solution.addRoot(node(topLevel, values));
    }
    return solution;
  }

  private static TreeNode node(Parameter<?> parameter, Map<String, String> values) {
    String value = values.get(parameter.name());
    if (value == null) {
      throw new JMetalException(
          "The configuration has no value for " + parameter.name() + ", which it activates");
    }
    TreeNode node;
    if (parameter instanceof CategoricalParameter categorical) {
      indexOf(categorical.validValues(), value, parameter);
      node = new TreeNode(parameter, value);
      for (Parameter<?> global : categorical.globalSubParameters()) {
        node.addGlobalChild(node(global, values));
      }
      for (ConditionalParameter<?> conditional : categorical.conditionalParameters()) {
        if (conditional.description().equals(value)) {
          node.addConditionalChild(node(conditional.parameter(), values));
        }
      }
      return node;
    } else if (parameter instanceof CategoricalIntegerParameter categorical) {
      int v = Integer.parseInt(value);
      if (!categorical.validValues().contains(v)) {
        fail(parameter, value);
      }
      node = new TreeNode(parameter, v);
      for (Parameter<?> global : categorical.globalSubParameters()) {
        node.addGlobalChild(node(global, values));
      }
      return node;
    } else if (parameter instanceof DoubleParameter) {
      encode(parameter, value);
      return new TreeNode(parameter, Double.parseDouble(value));
    } else if (parameter instanceof IntegerParameter) {
      encode(parameter, value);
      return new TreeNode(parameter, Integer.parseInt(value));
    } else if (parameter instanceof BooleanParameter) {
      return new TreeNode(parameter, Boolean.parseBoolean(value));
    }
    throw new JMetalException("Unsupported parameter type: " + parameter.getClass().getSimpleName());
  }
}
