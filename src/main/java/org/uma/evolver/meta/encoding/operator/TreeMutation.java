package org.uma.evolver.meta.encoding.operator;

import java.util.List;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.encoding.solution.TreeNode.NodeType;
import org.uma.evolver.meta.encoding.solution.TreeNode;
import org.uma.evolver.meta.encoding.util.TreeSolutionGenerator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Mutation operator for derivation tree solutions combining point mutation and subtree mutation.
 *
 * <p>Implements standard GP mutation (Koza, 1992; Poli et al., 2008):
 * <ul>
 *   <li><b>Numeric nodes (point mutation):</b> the value is perturbed using polynomial mutation
 *       within the parameter's range. Integer values are mutated on {@code [lower - 0.5, upper +
 *       0.5]} and rounded, so that the bounds are as likely as the inner values; if the rounded
 *       value equals the original one, it is moved one unit in the direction of the perturbation
 *       (inwards at a bound), so that the mutation always changes the node.</li>
 *   <li><b>Categorical nodes (subtree mutation):</b> a new production is selected from the
 *       grammar alternatives, and the conditional subtree is regenerated randomly. Global
 *       children are preserved.</li>
 * </ul>
 *
 * <p>One node is selected uniformly at random from the active nodes in the tree that can change
 * (all of them except categorical nodes with a single value), and mutated according to its type. This follows the standard GP
 * convention of one mutation event per individual, and every mutation event changes the tree.
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
      List<TreeNode> nodes =
          solution.allNodes().stream().filter(TreeMutation::canChange).toList();
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
   * Whether mutating a node can change its value: categorical nodes with a single value cannot
   * (numeric ranges always have more than one value), so they are never selected.
   */
  private static boolean canChange(TreeNode node) {
    return switch (node.type()) {
      case CATEGORICAL -> node.validValues().size() > 1;
      case DOUBLE, INTEGER, BOOLEAN -> true;
    };
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
   * Polynomial mutation for integer-valued nodes: the value is perturbed on {@code [lower - 0.5,
   * upper + 0.5]} and rounded; if it does not change, it is moved one unit in the direction of the
   * perturbation (at random if there was none), or inwards at a bound.
   */
  private void mutateInteger(TreeNode node) {
    int value = ((Number) node.value()).intValue();
    int lower = (int) node.lowerBound();
    int upper = (int) node.upperBound();

    double perturbed = polynomialMutation(value, lower - 0.5, upper + 0.5);
    int mutatedValue = (int) Math.max(lower, Math.min(upper, Math.round(perturbed)));
    if (mutatedValue == value) {
      int step;
      if (perturbed > value) {
        step = 1;
      } else if (perturbed < value) {
        step = -1;
      } else {
        step = random.nextDouble() < 0.5 ? -1 : 1;
      }
      if (value + step < lower || value + step > upper) {
        step = -step;
      }
      mutatedValue = value + step;
    }
    node.value(mutatedValue);
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
