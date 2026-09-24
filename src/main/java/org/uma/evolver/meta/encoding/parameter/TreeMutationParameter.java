package org.uma.evolver.meta.encoding.parameter;

import java.util.List;
import org.uma.evolver.meta.encoding.operator.TreeMutation;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Mutation parameter for derivation tree solutions ({@link DerivationTreeSolution}), used by
 * meta-optimizers working on the tree encoding. The only supported operator is {@code tree}
 * ({@link TreeMutation}), which requires the global sub-parameter {@code mutationProbability}, the
 * conditional sub-parameter {@code mutationDistributionIndex}, and the non-configurable
 * sub-parameter {@code treeSolutionGenerator} (the {@link TreeSolutionGenerator} used to
 * regenerate subtrees, set by the algorithm from the problem being solved).
 */
public class TreeMutationParameter extends MutationParameter<DerivationTreeSolution> {

  private static final List<String> VALID_OPERATORS = List.of("tree");

  public TreeMutationParameter(List<String> mutationOperators) {
    this(DEFAULT_NAME, mutationOperators);
  }

  public TreeMutationParameter(String name, List<String> mutationOperators) {
    super(name, mutationOperators);
    mutationOperators.stream()
        .filter(operator -> !VALID_OPERATORS.contains(operator))
        .findFirst()
        .ifPresent(
            invalidOperator -> {
              throw new JMetalException(
                  "Invalid tree mutation operator: "
                      + invalidOperator
                      + ". Supported operators are: "
                      + VALID_OPERATORS);
            });
  }

  @Override
  public MutationOperator<DerivationTreeSolution> getMutation() {
    double mutationProbability = (Double) findGlobalSubParameter("mutationProbability").value();
    var generator =
        (TreeSolutionGenerator) nonConfigurableSubParameters().get("treeSolutionGenerator");
    if (generator == null) {
      throw new JMetalException("treeSolutionGenerator non-configurable sub-parameter not set");
    }

    return switch (value()) {
      case "tree" ->
          new TreeMutation(
              mutationProbability,
              (Double) findConditionalParameter("mutationDistributionIndex").value(),
              generator);
      default -> throw new JMetalException("Unsupported tree mutation operator: " + value());
    };
  }
}
