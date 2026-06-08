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
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Generates random derivation tree solutions from a parameter space.
 *
 * <p>This class traverses the parameter space hierarchy and produces valid derivation trees by
 * randomly selecting productions for categorical parameters and random values for numeric ones.
 * It also supports generating subtrees for a specific parameter, used by the mutation operator
 * to regenerate conditional branches.
 *
 * @author Antonio J. Nebro
 */
public class TreeSolutionGenerator {

  private final ParameterSpace parameterSpace;
  private final JMetalRandom random;

  /**
   * Constructs a generator for the given parameter space.
   *
   * @param parameterSpace the parameter space defining the grammar
   */
  public TreeSolutionGenerator(ParameterSpace parameterSpace) {
    Check.notNull(parameterSpace);
    this.parameterSpace = parameterSpace;
    this.random = JMetalRandom.getInstance();
  }

  /**
   * Generates a random derivation tree solution.
   *
   * @param numberOfObjectives the number of objectives for the solution
   * @return a new random derivation tree solution
   */
  public DerivationTreeSolution generate(int numberOfObjectives) {
    DerivationTreeSolution solution = new DerivationTreeSolution(numberOfObjectives, 0);

    for (Parameter<?> topLevel : parameterSpace.topLevelParameters()) {
      TreeNode root = generateSubtree(topLevel);
      solution.addRoot(root);
    }

    return solution;
  }

  /**
   * Generates a random subtree rooted at the given parameter.
   *
   * @param parameter the parameter to derive
   * @return a new tree node with random value and children
   */
  public TreeNode generateSubtree(Parameter<?> parameter) {
    Check.notNull(parameter);

    if (parameter instanceof CategoricalParameter categoricalParameter) {
      return generateCategoricalNode(categoricalParameter);
    } else if (parameter instanceof CategoricalIntegerParameter catIntParameter) {
      return generateCategoricalIntegerNode(catIntParameter);
    } else if (parameter instanceof DoubleParameter doubleParameter) {
      return generateDoubleNode(doubleParameter);
    } else if (parameter instanceof IntegerParameter integerParameter) {
      return generateIntegerNode(integerParameter);
    } else if (parameter instanceof BooleanParameter booleanParameter) {
      return generateBooleanNode(booleanParameter);
    }

    throw new IllegalArgumentException(
        "Unsupported parameter type: " + parameter.getClass().getSimpleName());
  }

  /**
   * Generates conditional children for a categorical parameter given a specific selected value.
   * Used by the mutation operator when changing a categorical value.
   *
   * @param parameter the categorical parameter
   * @param selectedValue the newly selected value
   * @return list of tree nodes for the conditional children of the selected production
   */
  public List<TreeNode> generateConditionalChildren(
      Parameter<?> parameter, String selectedValue) {
    Check.notNull(parameter);
    Check.notNull(selectedValue);

    List<TreeNode> children = new ArrayList<>();
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      if (conditional.description().equals(selectedValue)) {
        children.add(generateSubtree(conditional.parameter()));
      }
    }
    return children;
  }

  private TreeNode generateCategoricalNode(CategoricalParameter parameter) {
    List<String> validValues = parameter.validValues();
    String selectedValue = validValues.get(random.nextInt(0, validValues.size() - 1));

    TreeNode node = new TreeNode(parameter, selectedValue);

    // Generate global children (always present)
    for (Parameter<?> globalSub : parameter.globalSubParameters()) {
      node.addGlobalChild(generateSubtree(globalSub));
    }

    // Generate conditional children (only for the selected production)
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      if (conditional.description().equals(selectedValue)) {
        node.addConditionalChild(generateSubtree(conditional.parameter()));
      }
    }

    return node;
  }

  private TreeNode generateCategoricalIntegerNode(CategoricalIntegerParameter parameter) {
    List<Integer> validValues = parameter.validValues();
    Integer selectedValue = validValues.get(random.nextInt(0, validValues.size() - 1));

    TreeNode node = new TreeNode(parameter, selectedValue);

    for (Parameter<?> globalSub : parameter.globalSubParameters()) {
      node.addGlobalChild(generateSubtree(globalSub));
    }

    return node;
  }

  private TreeNode generateDoubleNode(DoubleParameter parameter) {
    double lower = parameter.minValue();
    double upper = parameter.maxValue();
    double value = lower + random.nextDouble() * (upper - lower);

    return new TreeNode(parameter, value);
  }

  private TreeNode generateIntegerNode(IntegerParameter parameter) {
    int lower = parameter.minValue();
    int upper = parameter.maxValue();
    int value = random.nextInt(lower, upper);

    return new TreeNode(parameter, value);
  }

  private TreeNode generateBooleanNode(BooleanParameter parameter) {
    boolean value = random.nextDouble() < 0.5;
    return new TreeNode(parameter, value);
  }
}
