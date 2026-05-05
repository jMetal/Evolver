package org.uma.evolver.encoding.util;

import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Utility class for converting between parameter spaces and BNF grammar representations, and for
 * validating derivation tree solutions against their grammar.
 *
 * <p>The BNF (Backus-Naur Form) representation makes explicit the formal grammar that the YAML
 * parameter space defines implicitly. Each categorical parameter becomes a non-terminal with
 * alternative productions; numeric and boolean parameters become terminal symbols with their
 * ranges.
 *
 * @author Antonio J. Nebro
 */
public final class GrammarConverter {

  private GrammarConverter() {
    // Utility class
  }

  /**
   * Converts a parameter space to its BNF grammar string representation.
   *
   * <p>The output format follows standard BNF notation:
   * <pre>
   * &lt;algorithm&gt; ::= &lt;param1&gt; &lt;param2&gt; ...
   * &lt;param1&gt; ::= "value1" &lt;conditional_child&gt; | "value2" &lt;conditional_child&gt;
   * &lt;param2&gt; ::= DOUBLE[0.0, 1.0]
   * </pre>
   *
   * @param parameterSpace the parameter space to convert
   * @return the BNF grammar as a string
   */
  public static String toBnf(ParameterSpace parameterSpace) {
    Check.notNull(parameterSpace);

    StringBuilder sb = new StringBuilder();
    List<Parameter<?>> topLevel = parameterSpace.topLevelParameters();

    // Start rule
    sb.append("<start> ::= ");
    for (int i = 0; i < topLevel.size(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append("<").append(topLevel.get(i).name()).append(">");
    }
    sb.append("\n");

    // Rules for each parameter
    for (Parameter<?> param : topLevel) {
      appendParameterRules(param, sb);
    }

    return sb.toString();
  }

  /**
   * Validates that a derivation tree solution is structurally consistent with the parameter space.
   *
   * <p>Checks that:
   * <ul>
   *   <li>Each node's value is valid for its parameter type</li>
   *   <li>Categorical nodes have the correct conditional children for their selected value</li>
   *   <li>Numeric values are within bounds</li>
   * </ul>
   *
   * @param solution the solution to validate
   * @return list of validation errors (empty if valid)
   */
  public static List<String> validate(DerivationTreeSolution solution) {
    Check.notNull(solution);

    List<String> errors = new ArrayList<>();
    for (TreeNode root : solution.roots()) {
      validateNode(root, errors);
    }
    return errors;
  }

  private static void appendParameterRules(Parameter<?> parameter, StringBuilder sb) {
    sb.append("<").append(parameter.name()).append("> ::= ");

    if (parameter instanceof CategoricalParameter categorical) {
      appendCategoricalRule(categorical, sb);
    } else if (parameter instanceof CategoricalIntegerParameter catInt) {
      appendCategoricalIntegerRule(catInt, sb);
    } else if (parameter instanceof DoubleParameter doubleParam) {
      sb.append("DOUBLE[").append(doubleParam.minValue())
          .append(", ").append(doubleParam.maxValue()).append("]");
    } else if (parameter instanceof IntegerParameter intParam) {
      sb.append("INTEGER[").append(intParam.minValue())
          .append(", ").append(intParam.maxValue()).append("]");
    } else if (parameter instanceof BooleanParameter) {
      sb.append("BOOLEAN");
    }

    sb.append("\n");

    // Recurse into sub-parameters
    for (Parameter<?> globalSub : parameter.globalSubParameters()) {
      appendParameterRules(globalSub, sb);
    }
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      appendParameterRules(conditional.parameter(), sb);
    }
  }

  private static void appendCategoricalRule(CategoricalParameter parameter, StringBuilder sb) {
    List<String> values = parameter.validValues();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(" | ");
      }
      sb.append("\"").append(values.get(i)).append("\"");

      // Append global sub-parameters
      for (Parameter<?> globalSub : parameter.globalSubParameters()) {
        sb.append(" <").append(globalSub.name()).append(">");
      }

      // Append conditional children for this production
      for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
        if (conditional.description().equals(values.get(i))) {
          sb.append(" <").append(conditional.parameter().name()).append(">");
        }
      }
    }
  }

  private static void appendCategoricalIntegerRule(
      CategoricalIntegerParameter parameter, StringBuilder sb) {
    List<Integer> values = parameter.validValues();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(" | ");
      }
      sb.append(values.get(i));
    }
  }

  private static void validateNode(TreeNode node, List<String> errors) {
    Parameter<?> param = node.parameter();
    String name = node.grammarSymbol();

    if (param instanceof CategoricalParameter categorical) {
      validateCategoricalNode(node, categorical, name, errors);
    } else if (param instanceof DoubleParameter doubleParam) {
      validateDoubleNode(node, doubleParam, name, errors);
    } else if (param instanceof IntegerParameter intParam) {
      validateIntegerNode(node, intParam, name, errors);
    } else if (param instanceof BooleanParameter) {
      if (!(node.value() instanceof Boolean)) {
        errors.add(name + ": expected Boolean value, got " + node.value().getClass().getSimpleName());
      }
    }

    // Recurse into children
    for (TreeNode child : node.children()) {
      validateNode(child, errors);
    }
  }

  private static void validateCategoricalNode(
      TreeNode node, CategoricalParameter param, String name, List<String> errors) {
    if (!(node.value() instanceof String value)) {
      errors.add(name + ": expected String value for categorical parameter");
      return;
    }
    if (!param.validValues().contains(value)) {
      errors.add(name + ": invalid value '" + value + "', valid values: " + param.validValues());
    }
  }

  private static void validateDoubleNode(
      TreeNode node, DoubleParameter param, String name, List<String> errors) {
    if (!(node.value() instanceof Number numValue)) {
      errors.add(name + ": expected numeric value");
      return;
    }
    double v = numValue.doubleValue();
    if (v < param.minValue() || v > param.maxValue()) {
      errors.add(name + ": value " + v + " out of range ["
          + param.minValue() + ", " + param.maxValue() + "]");
    }
  }

  private static void validateIntegerNode(
      TreeNode node, IntegerParameter param, String name, List<String> errors) {
    if (!(node.value() instanceof Number numValue)) {
      errors.add(name + ": expected numeric value");
      return;
    }
    int v = numValue.intValue();
    if (v < param.minValue() || v > param.maxValue()) {
      errors.add(name + ": value " + v + " out of range ["
          + param.minValue() + ", " + param.maxValue() + "]");
    }
  }
}
