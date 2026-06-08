package org.uma.evolver.encoding.operator;

import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Typed subtree crossover operator for derivation tree solutions.
 *
 * <p>Implements the standard Strongly Typed GP (STGP) crossover (Montana, 1995): a node is
 * selected in each parent, constrained to share the same grammar symbol (parameter name), and
 * their subtrees are swapped to produce two offspring.
 *
 * <p>This ensures that offspring are always valid configurations, since subtrees rooted at the
 * same grammar symbol are structurally compatible by construction.
 *
 * @author Antonio J. Nebro
 * @see <a href="https://doi.org/10.1162/evco.1995.3.2.199">Montana, 1995. Strongly Typed GP</a>
 */
public class SubtreeCrossover implements CrossoverOperator<DerivationTreeSolution> {

  private final double probability;
  private final JMetalRandom random;

  /**
   * Constructs a subtree crossover operator with the given probability.
   *
   * @param probability the probability of applying crossover (in [0.0, 1.0])
   */
  public SubtreeCrossover(double probability) {
    Check.probabilityIsValid(probability);
    this.probability = probability;
    this.random = JMetalRandom.getInstance();
  }

  @Override
  public List<DerivationTreeSolution> execute(List<DerivationTreeSolution> parents) {
    Check.notNull(parents);
    Check.that(parents.size() == 2, "Subtree crossover requires exactly 2 parents");

    DerivationTreeSolution child1 = (DerivationTreeSolution) parents.get(0).copy();
    DerivationTreeSolution child2 = (DerivationTreeSolution) parents.get(1).copy();

    if (random.nextDouble() < probability) {
      performCrossover(child1, child2);
    }

    return List.of(child1, child2);
  }

  @Override
  public double crossoverProbability() {
    return probability;
  }

  @Override
  public int numberOfRequiredParents() {
    return 2;
  }

  @Override
  public int numberOfGeneratedChildren() {
    return 2;
  }

  /**
   * Performs the typed subtree crossover between two offspring solutions.
   *
   * <p>Selects a random node in child1, finds a node with the same grammar symbol in child2,
   * and swaps their subtrees (value + all children).
   *
   * <p>Both node lists are obtained with a single {@code allNodes()} call each; the symbol filter
   * is applied inline to avoid the redundant traversal that {@code nodesBySymbol()} would trigger.
   */
  private void performCrossover(
      DerivationTreeSolution child1, DerivationTreeSolution child2) {
    List<TreeNode> nodes1 = child1.allNodes();
    TreeNode selected1 = nodes1.get(random.nextInt(0, nodes1.size() - 1));
    String symbol = selected1.grammarSymbol();

    List<TreeNode> nodes2 = child2.allNodes();
    List<TreeNode> candidates = nodes2.stream()
        .filter(n -> n.grammarSymbol().equals(symbol))
        .toList();
    if (candidates.isEmpty()) {
      return;
    }

    TreeNode selected2 = candidates.get(random.nextInt(0, candidates.size() - 1));

    swapSubtrees(selected1, selected2);
  }

  /**
   * Swaps the content (value, global children, conditional children) between two nodes.
   */
  private void swapSubtrees(TreeNode node1, TreeNode node2) {
    Object value1 = node1.value();
    List<TreeNode> global1 = new ArrayList<>(node1.globalChildren());
    List<TreeNode> conditional1 = new ArrayList<>(node1.conditionalChildren());

    Object value2 = node2.value();
    List<TreeNode> global2 = new ArrayList<>(node2.globalChildren());
    List<TreeNode> conditional2 = new ArrayList<>(node2.conditionalChildren());

    node1.value(value2);
    node1.replaceGlobalChildren(global2);
    node1.replaceConditionalChildren(conditional2);

    node2.value(value1);
    node2.replaceGlobalChildren(global1);
    node2.replaceConditionalChildren(conditional1);
  }
}
