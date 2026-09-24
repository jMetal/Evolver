package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Variation parameter for derivation tree solutions ({@link DerivationTreeSolution}). The only
 * supported strategy is {@code crossoverAndMutationVariation}, combining the conditional {@code
 * crossover} and {@code mutation} parameters; the offspring population size is taken from the
 * non-configurable sub-parameter {@code offspringPopulationSize}.
 */
public class TreeVariationParameter extends VariationParameter<DerivationTreeSolution> {

  private static final String CROSSOVER_AND_MUTATION = "crossoverAndMutationVariation";

  public TreeVariationParameter(List<String> variationStrategies) {
    this(DEFAULT_NAME, variationStrategies);
  }

  public TreeVariationParameter(String name, List<String> variationStrategies) {
    super(name, variationStrategies);
    variationStrategies.stream()
        .filter(strategy -> !CROSSOVER_AND_MUTATION.equals(strategy))
        .findFirst()
        .ifPresent(
            invalidStrategy -> {
              throw new JMetalException(
                  "Invalid variation strategy for tree solutions: "
                      + invalidStrategy
                      + ". Supported strategy: "
                      + CROSSOVER_AND_MUTATION);
            });
  }

  @Override
  @SuppressWarnings("unchecked")
  public Variation<DerivationTreeSolution> getVariation() {
    if (!CROSSOVER_AND_MUTATION.equals(value())) {
      throw new JMetalException("Unsupported variation strategy: " + value());
    }
    var crossoverParameter =
        (CrossoverParameter<DerivationTreeSolution>) findConditionalParameter("crossover");
    var mutationParameter =
        (MutationParameter<DerivationTreeSolution>) findConditionalParameter("mutation");
    if (crossoverParameter == null || mutationParameter == null) {
      throw new JMetalException("crossover and mutation parameters are required");
    }

    Integer offspringPopulationSize =
        (Integer) nonConfigurableSubParameters().get("offspringPopulationSize");
    if (offspringPopulationSize == null || offspringPopulationSize <= 0) {
      throw new IllegalStateException("offspringPopulationSize must be a positive integer");
    }

    return new CrossoverAndMutationVariation<>(
        offspringPopulationSize, crossoverParameter.getCrossover(), mutationParameter.getMutation());
  }
}
