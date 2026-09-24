package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.util.List;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Crossover parameter for derivation tree solutions ({@link DerivationTreeSolution}), used by
 * meta-optimizers working on the tree encoding. The only supported operator is {@code subtree}
 * ({@link SubtreeCrossover}), which requires the global sub-parameter {@code crossoverProbability}.
 */
public class TreeCrossoverParameter extends CrossoverParameter<DerivationTreeSolution> {

  private static final List<String> VALID_OPERATORS = List.of("subtree");

  public TreeCrossoverParameter(List<String> crossoverOperators) {
    this(DEFAULT_NAME, crossoverOperators);
  }

  public TreeCrossoverParameter(String name, List<String> crossoverOperators) {
    super(name, crossoverOperators);
    crossoverOperators.stream()
        .filter(operator -> !VALID_OPERATORS.contains(operator))
        .findFirst()
        .ifPresent(
            invalidOperator -> {
              throw new JMetalException(
                  "Invalid tree crossover operator: "
                      + invalidOperator
                      + ". Supported operators are: "
                      + VALID_OPERATORS);
            });
  }

  @Override
  public CrossoverOperator<DerivationTreeSolution> getCrossover() {
    double crossoverProbability = (Double) findGlobalSubParameter("crossoverProbability").value();
    return switch (value()) {
      case "subtree" -> new SubtreeCrossover(crossoverProbability);
      default -> throw new JMetalException("Unsupported tree crossover operator: " + value());
    };
  }
}
