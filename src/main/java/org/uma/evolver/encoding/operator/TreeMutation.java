package org.uma.evolver.encoding.operator;

import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.evolver.encoding.solution.TreeNode.NodeType;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Mutation operator for derivation tree solutions combining point mutation and subtree mutation.
 *
 * <p>Implements standard GP mutation (Koza, 1992; Poli et al., 2008):
 * <ul>
 *   <li><b>Numeric nodes (point mutation):</b> the value is perturbed using polynomial mutation
 *       within the parameter's range.</li>
 *   <li><b>Categorical nodes (subtree mutation):</b> a new production is selected from the
 *       grammar alternatives, and the conditional subtree is regenerated randomly. Global
 *       children are preserved.</li>
 * </ul>
 *
 * <p>One node is selected uniformly at random from all active nodes in the tree, and mutated
 * according to its type. This follows the standard GP convention of one mutation event per
 * individual.
 *
 * @author Antonio J. Nebro
 */
public class TreeMutation implements MutationOperator<DerivationTreeSolution> {

  private final double probability;
  private final double distributionIndex;
  private final TreeSolutionGenerator generator;
  private final JMetalRandom random;

  /**
   * Constructs a tree mutation operator.
   *
   * @param probability the probability of applying mutation to an individual
   * @param distributionIndex the distribution index for polynomial mutation of numeric nodes
   * @param generator the tree solution generator for regenerating subtrees
   */
  public TreeMutation(
      double probability,
      double distributionIndex,
      TreeSolutionGenerator generator) {
    Check.probabilityIsValid(probability);
    Check.that(distributionIndex >= 0, "Distribution index must be non-negative");
    Check.notNull(generator);

    this.probability = probability;
    this.distributionIndex = distributionIndex;
    this.generator = generator;
    this.random = JMetalRandom.getInstance();
  }

  @Override
  public DerivationTreeSolution execute(DerivationTreeSolution solution) {
    Check.notNull(solution);

    if (random.nextDouble() < probability) {
      List<TreeNode> nodes = solution.allNodes();
      if (!nodes.isEmpty()) {
        TreeNode selected = nodes.get(random.nextInt(0, nodes.size() - 1));
        mutateNode(selected);
      }
    }

    return solution;
  }

  @Override
  public double mutationProbability() {
    return probability;
  }

  /**
   * Mutates a single node according to its type.
   */
  private void mutateNode(TreeNode node) {
    if (node.type() == NodeType.DOUBLE) {
      mutateDouble(node);
    } else if (node.type() == NodeType.INTEGER) {
      mutateInteger(node);
    } else if (node.type() == NodeType.CATEGORICAL) {
      mutateCategorical(node);
    } else if (node.type() == NodeType.BOOLEAN) {
      mutateBoolean(node);
    }
  }

  /**
   * Polynomial mutation for double-valued nodes.
   */
  private void mutateDouble(TreeNode node) {
    double value = ((Number) node.value()).doubleValue();
    double lower = node.lowerBound();
    double upper = node.upperBound();

    double mutatedValue = polynomialMutation(value, lower, upper);
    node.value(mutatedValue);
  }

  /**
   * Polynomial mutation for integer-valued nodes, rounded to nearest integer.
   */
  private void mutateInteger(TreeNode node) {
    int value = ((Number) node.value()).intValue();
    double lower = node.lowerBound();
    double upper = node.upperBound();

    double mutatedValue = polynomialMutation(value, lower, upper);
    node.value((int) Math.round(mutatedValue));
  }

  /**
   * Subtree mutation for categorical nodes: selects a new production and regenerates the
   * conditional branch. Global children are preserved.
   */
  private void mutateCategorical(TreeNode node) {
    List<String> validValues = node.validValues();
    if (validValues.size() <= 1) {
      return;
    }

    String currentValue = (String) node.value();
    String newValue;
    do {
      newValue = validValues.get(random.nextInt(0, validValues.size() - 1));
    } while (newValue.equals(currentValue));

    node.value(newValue);

    // Regenerate conditional children for the new production
    List<TreeNode> newConditionalChildren =
        generator.generateConditionalChildren(node.parameter(), newValue);
    node.replaceConditionalChildren(newConditionalChildren);
  }

  /**
   * Flips a boolean node.
   */
  private void mutateBoolean(TreeNode node) {
    Boolean current = (Boolean) node.value();
    node.value(!current);
  }

  /**
   * Standard polynomial mutation (Deb and Goyal, 1996).
   */
  private double polynomialMutation(
      double value, double lowerBound, double upperBound) {
    double delta1 = (value - lowerBound) / (upperBound - lowerBound);
    double delta2 = (upperBound - value) / (upperBound - lowerBound);

    double rnd = random.nextDouble();
    double mutPow = 1.0 / (distributionIndex + 1.0);
    double deltaq;

    if (rnd <= 0.5) {
      double xy = 1.0 - delta1;
      double val = 2.0 * rnd + (1.0 - 2.0 * rnd) * Math.pow(xy, distributionIndex + 1.0);
      deltaq = Math.pow(val, mutPow) - 1.0;
    } else {
      double xy = 1.0 - delta2;
      double val =
          2.0 * (1.0 - rnd) + 2.0 * (rnd - 0.5) * Math.pow(xy, distributionIndex + 1.0);
      deltaq = 1.0 - Math.pow(val, mutPow);
    }

    double result = value + deltaq * (upperBound - lowerBound);
    return Math.max(lowerBound, Math.min(upperBound, result));
  }
}
