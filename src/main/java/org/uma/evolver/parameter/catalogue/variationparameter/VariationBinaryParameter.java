package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter class for configuring variation operators specifically designed for binary solutions.
 *
 * <p>This class provides a factory for creating variation operators that work with {@link
 * BinarySolution} instances. It extends {@link VariationParameter} to handle binary-specific
 * variation strategies.
 *
 * <p>Supported variation strategies:
 *
 * <ul>
 *   <li><b>crossoverAndMutationVariation</b>: Applies both crossover and mutation operators to
 *       create new offspring solutions
 * </ul>
 *
 * <p>Required parameters for "crossoverAndMutationVariation":
 *
 * <ul>
 *   <li><b>offspringPopulationSize</b>: The number of offspring solutions to generate
 *   <li><b>crossover</b>: Configuration for the crossover operator
 *   <li><b>mutation</b>: Configuration for the mutation operator
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * VariationBinaryParameter variationParam = new VariationBinaryParameter(
 *     List.of("crossoverAndMutationVariation"));
 * // Configure sub-parameters for crossover and mutation...
 * Variation<BinarySolution> variation = variationParam.getVariation();
 * }</pre>
 *
 * @see org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation
 */
public class VariationBinaryParameter extends VariationParameter<BinarySolution> {
  /** Valid variation strategy names for binary solutions. */
  private static final String CROSSOVER_AND_MUTATION = "crossoverAndMutationVariation";

  /**
   * Constructs a new VariationBinaryParameter with the specified list of variation strategy names.
   *
   * @param variationStrategies the list of supported variation strategy names
   * @throws IllegalArgumentException if variationStrategies is null, empty, or contains invalid
   *     values
   * @throws JMetalException if any strategy name is not supported
   */
  public VariationBinaryParameter(List<String> variationStrategies) {
    super(variationStrategies);

    if (variationStrategies == null || variationStrategies.isEmpty()) {
      throw new IllegalArgumentException("Variation strategies list cannot be null or empty");
    }

    // Validate that all provided strategies are supported
    variationStrategies.stream()
        .filter(strategy -> !CROSSOVER_AND_MUTATION.equals(strategy))
        .findFirst()
        .ifPresent(
            invalidStrategy -> {
              throw new JMetalException(
                  "Invalid variation strategy for binary solutions: "
                      + invalidStrategy
                      + ". Supported strategies: "
                      + CROSSOVER_AND_MUTATION);
            });
  }

  /**
   * Creates and returns a configured variation operator for binary solutions based on the current
   * parameter value.
   *
   * @return a configured variation operator for binary solutions
   * @throws JMetalException if the operator cannot be created with the current configuration
   * @throws IllegalStateException if required parameters are not set or have invalid values
   * @throws ClassCastException if any parameter has an unexpected type
   */
  @Override
  public Variation<BinarySolution> getVariation() {
    // Validate and get the offspring population size
    Integer offspringPopulationSize = getOffspringPopulationSize();

    if (CROSSOVER_AND_MUTATION.equals(value())) {
      // Get and validate the crossover parameter
      CrossoverParameter<BinarySolution> crossoverParameter =
          (CrossoverParameter<BinarySolution>) findConditionalSubParameter("crossover");
      if (crossoverParameter == null) {
        throw new JMetalException("crossover parameter not found");
      }

      // Get and validate the mutation parameter
      MutationParameter<BinarySolution> mutationParameter =
          (MutationParameter<BinarySolution>) findGlobalSubParameter("mutation");
      if (mutationParameter == null) {
        mutationParameter =
            (MutationParameter<BinarySolution>) findConditionalSubParameter("mutation");
        if (mutationParameter == null) {
          throw new JMetalException("mutation parameter not found");
        }
      }

      // Get the operators
      CrossoverOperator<BinarySolution> crossoverOperator = crossoverParameter.getCrossover();
      MutationOperator<BinarySolution> mutationOperator = mutationParameter.getMutation();

      // Create and return the variation operator
      return new CrossoverAndMutationVariation<>(
          offspringPopulationSize, crossoverOperator, mutationOperator);
    } else {
      throw new JMetalException("Unsupported variation strategy: " + value());
    }
  }

  /** Helper method to get and validate the offspring population size parameter. */
  private int getOffspringPopulationSize() {
    try {
      Integer size = (Integer) nonConfigurableSubParameters().get("offspringPopulationSize");
      if (size == null || size <= 0) {
        throw new IllegalStateException("offspringPopulationSize must be a positive integer");
      }
      return size;
    } catch (ClassCastException e) {
      throw new IllegalStateException("Invalid offspringPopulationSize value", e);
    }
  }
}
