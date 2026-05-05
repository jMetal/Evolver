package org.uma.evolver.encoding.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.type.RangeParameter;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Represents a node in a derivation tree for algorithm configuration.
 *
 * <p>Each node corresponds to a parameter in the grammar (YAML parameter space) and holds its
 * decoded value. Nodes are organized hierarchically: global children are always present, while
 * conditional children depend on the value of a categorical parent.
 *
 * <p>Node types mirror the grammar's terminal and non-terminal symbols:
 * <ul>
 *   <li>{@link NodeType#CATEGORICAL}: non-terminal with alternative productions</li>
 *   <li>{@link NodeType#DOUBLE}, {@link NodeType#INTEGER}: numeric terminals</li>
 *   <li>{@link NodeType#BOOLEAN}: boolean terminal</li>
 * </ul>
 *
 * @author Antonio J. Nebro
 */
public class TreeNode {

  /** Types of nodes in the derivation tree. */
  public enum NodeType {
    CATEGORICAL,
    DOUBLE,
    INTEGER,
    BOOLEAN
  }

  private final Parameter<?> parameter;
  private Object value;
  private final List<TreeNode> globalChildren;
  private final List<TreeNode> conditionalChildren;

  /**
   * Constructs a tree node for the given parameter with an initial value.
   *
   * @param parameter the parameter this node represents
   * @param value the decoded value of the parameter
   */
  public TreeNode(Parameter<?> parameter, Object value) {
    Check.notNull(parameter);
    this.parameter = parameter;
    this.value = value;
    this.globalChildren = new ArrayList<>();
    this.conditionalChildren = new ArrayList<>();
  }

  /**
   * Returns the parameter this node represents.
   *
   * @return the parameter
   */
  public Parameter<?> parameter() {
    return parameter;
  }

  /**
   * Returns the grammar symbol (parameter name) for type-matching in crossover.
   *
   * @return the parameter name
   */
  public String grammarSymbol() {
    return parameter.name();
  }

  /**
   * Returns the type of this node.
   *
   * @return the node type
   */
  public NodeType type() {
    if (parameter instanceof CategoricalParameter
        || parameter instanceof CategoricalIntegerParameter) {
      return NodeType.CATEGORICAL;
    } else if (parameter instanceof DoubleParameter) {
      return NodeType.DOUBLE;
    } else if (parameter instanceof IntegerParameter) {
      return NodeType.INTEGER;
    } else if (parameter instanceof BooleanParameter) {
      return NodeType.BOOLEAN;
    }
    throw new IllegalStateException(
        "Unknown parameter type: " + parameter.getClass().getSimpleName());
  }

  /**
   * Returns the current decoded value of this node.
   *
   * @return the value (String for categorical, Double/Integer/Boolean for others)
   */
  public Object value() {
    return value;
  }

  /**
   * Sets the decoded value of this node.
   *
   * @param value the new value
   */
  public void value(Object value) {
    this.value = value;
  }

  /**
   * Returns the valid values for a categorical node.
   *
   * @return list of valid values, or empty list if not categorical
   */
  public List<String> validValues() {
    if (parameter instanceof CategoricalParameter categoricalParameter) {
      return categoricalParameter.validValues();
    }
    return Collections.emptyList();
  }

  /**
   * Returns the lower bound for a numeric node.
   *
   * @return the lower bound
   * @throws IllegalStateException if the node is not numeric
   */
  public double lowerBound() {
    if (parameter instanceof RangeParameter<?> rangeParameter) {
      return rangeParameter.minValue().doubleValue();
    }
    throw new IllegalStateException("lowerBound() called on non-numeric node: " + type());
  }

  /**
   * Returns the upper bound for a numeric node.
   *
   * @return the upper bound
   * @throws IllegalStateException if the node is not numeric
   */
  public double upperBound() {
    if (parameter instanceof RangeParameter<?> rangeParameter) {
      return rangeParameter.maxValue().doubleValue();
    }
    throw new IllegalStateException("upperBound() called on non-numeric node: " + type());
  }

  /**
   * Returns the global children (always present regardless of this node's value).
   *
   * @return unmodifiable list of global children
   */
  public List<TreeNode> globalChildren() {
    return Collections.unmodifiableList(globalChildren);
  }

  /**
   * Returns the conditional children (depend on this node's categorical value).
   *
   * @return unmodifiable list of conditional children
   */
  public List<TreeNode> conditionalChildren() {
    return Collections.unmodifiableList(conditionalChildren);
  }

  /**
   * Returns all children (global + conditional).
   *
   * @return list of all children
   */
  public List<TreeNode> children() {
    List<TreeNode> all = new ArrayList<>(globalChildren);
    all.addAll(conditionalChildren);
    return all;
  }

  /**
   * Adds a global child to this node.
   *
   * @param child the child node to add
   */
  public void addGlobalChild(TreeNode child) {
    Check.notNull(child);
    globalChildren.add(child);
  }

  /**
   * Adds a conditional child to this node.
   *
   * @param child the child node to add
   */
  public void addConditionalChild(TreeNode child) {
    Check.notNull(child);
    conditionalChildren.add(child);
  }

  /**
   * Replaces all conditional children with a new list.
   *
   * @param newChildren the new conditional children
   */
  public void replaceConditionalChildren(List<TreeNode> newChildren) {
    Check.notNull(newChildren);
    conditionalChildren.clear();
    conditionalChildren.addAll(newChildren);
  }

  /**
   * Replaces all global children with a new list.
   *
   * @param newChildren the new global children
   */
  public void replaceGlobalChildren(List<TreeNode> newChildren) {
    Check.notNull(newChildren);
    globalChildren.clear();
    globalChildren.addAll(newChildren);
  }

  /**
   * Creates a deep copy of this node and its entire subtree.
   *
   * @return a new TreeNode that is a deep copy
   */
  public TreeNode deepCopy() {
    TreeNode copy = new TreeNode(parameter, value);
    for (TreeNode child : globalChildren) {
      copy.addGlobalChild(child.deepCopy());
    }
    for (TreeNode child : conditionalChildren) {
      copy.addConditionalChild(child.deepCopy());
    }
    return copy;
  }

  @Override
  public String toString() {
    return grammarSymbol() + " = " + value;
  }
}
