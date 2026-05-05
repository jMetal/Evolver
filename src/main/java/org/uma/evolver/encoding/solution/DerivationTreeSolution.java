package org.uma.evolver.encoding.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A solution represented as a derivation tree for algorithm configuration.
 *
 * <p>Each solution encodes a complete algorithm configuration as a tree whose structure mirrors
 * the hierarchical parameter space (grammar). Only active parameters are present in the tree,
 * eliminating the inactive variable problem of flat encodings.
 *
 * <p>This class implements jMetal's {@link Solution} interface, making it compatible with any
 * evolutionary algorithm in the framework. The {@link #variables()} method returns the tree's
 * root nodes.
 *
 * @author Antonio J. Nebro
 */
public class DerivationTreeSolution implements Solution<TreeNode> {

  private final List<TreeNode> roots;
  private final double[] objectives;
  private final double[] constraints;
  private final Map<Object, Object> attributes;

  /**
   * Constructs a derivation tree solution with the given number of objectives and constraints.
   *
   * @param numberOfObjectives the number of objectives
   * @param numberOfConstraints the number of constraints
   */
  public DerivationTreeSolution(int numberOfObjectives, int numberOfConstraints) {
    Check.that(numberOfObjectives > 0, "Number of objectives must be positive");
    Check.that(numberOfConstraints >= 0, "Number of constraints must be non-negative");

    this.roots = new ArrayList<>();
    this.objectives = new double[numberOfObjectives];
    this.constraints = new double[numberOfConstraints];
    this.attributes = new HashMap<>();
  }

  /**
   * Private constructor for deep copy.
   */
  private DerivationTreeSolution(
      List<TreeNode> roots,
      double[] objectives,
      double[] constraints,
      Map<Object, Object> attributes) {
    this.roots = roots;
    this.objectives = objectives;
    this.constraints = constraints;
    this.attributes = attributes;
  }

  /**
   * Returns the root nodes (top-level parameters) of the derivation tree.
   *
   * @return unmodifiable list of root nodes
   */
  public List<TreeNode> roots() {
    return Collections.unmodifiableList(roots);
  }

  /**
   * Adds a root node to the derivation tree.
   *
   * @param node the root node to add
   */
  public void addRoot(TreeNode node) {
    Check.notNull(node);
    roots.add(node);
  }

  /**
   * Returns all active nodes in the tree via depth-first traversal.
   *
   * @return list of all nodes in the tree
   */
  public List<TreeNode> allNodes() {
    List<TreeNode> nodes = new ArrayList<>();
    for (TreeNode root : roots) {
      collectNodes(root, nodes);
    }
    return nodes;
  }

  /**
   * Returns all nodes that match a given grammar symbol (parameter name). Used by typed subtree
   * crossover to find compatible crossover points.
   *
   * @param grammarSymbol the grammar symbol to match
   * @return list of nodes with that symbol
   */
  public List<TreeNode> nodesBySymbol(String grammarSymbol) {
    Check.notNull(grammarSymbol);
    List<TreeNode> result = new ArrayList<>();
    for (TreeNode node : allNodes()) {
      if (node.grammarSymbol().equals(grammarSymbol)) {
        result.add(node);
      }
    }
    return result;
  }

  /**
   * Converts the derivation tree to a parameter string array suitable for the base-level
   * algorithm's {@code parse()} method.
   *
   * <p>Only active nodes (those present in the tree) are included in the output.
   *
   * @return array of strings in the format ["--param1", "value1", "--param2", "value2", ...]
   */
  public String[] toParameterArray() {
    StringBuilder sb = new StringBuilder();
    for (TreeNode root : roots) {
      appendNode(root, sb);
    }
    return sb.toString().trim().split("\\s+");
  }

  @Override
  public List<TreeNode> variables() {
    return roots();
  }

  @Override
  public double[] objectives() {
    return objectives;
  }

  @Override
  public double[] constraints() {
    return constraints;
  }

  @Override
  public Map<Object, Object> attributes() {
    return attributes;
  }

  @Override
  public Solution<TreeNode> copy() {
    List<TreeNode> copiedRoots = new ArrayList<>();
    for (TreeNode root : roots) {
      copiedRoots.add(root.deepCopy());
    }

    double[] copiedObjectives = objectives.clone();
    double[] copiedConstraints = constraints.clone();
    Map<Object, Object> copiedAttributes = new HashMap<>(attributes);

    return new DerivationTreeSolution(
        copiedRoots, copiedObjectives, copiedConstraints, copiedAttributes);
  }

  private void collectNodes(TreeNode node, List<TreeNode> accumulator) {
    accumulator.add(node);
    for (TreeNode child : node.children()) {
      collectNodes(child, accumulator);
    }
  }

  private void appendNode(TreeNode node, StringBuilder sb) {
    sb.append("--").append(node.grammarSymbol()).append(" ").append(node.value()).append(" ");
    for (TreeNode child : node.children()) {
      appendNode(child, sb);
    }
  }
}
