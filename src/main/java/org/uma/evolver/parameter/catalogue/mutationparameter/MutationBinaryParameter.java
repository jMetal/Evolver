package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter class for configuring binary mutation operators in evolutionary algorithms.
 * <p>
 * This class provides a factory for creating mutation operators specifically designed for
 * {@link BinarySolution} instances. It extends {@link MutationParameter} to handle binary-specific
 * mutation strategies.
 *
 * <p>Supported mutation operators:
 * <ul>
 *   <li><b>bitFlip</b>: Flips each bit in the solution with a given probability.
 * </ul>
 *
 * <p>Required parameters:
 * <ul>
 *   <li><b>mutationProbabilityFactor</b>: The probability factor used to calculate the actual
 *       mutation probability (actual probability = factor / numberOfBitsInASolution).
 *   <li><b>numberOfBitsInASolution</b>: The total number of bits in a solution (non-configurable).
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * MutationBinaryParameter mutationParam = new MutationBinaryParameter(List.of("bitFlip"));
 * mutationParam.setValue("bitFlip");
 * MutationOperator<BinarySolution> mutation = mutationParam.getMutation();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.operator.mutation.impl.BitFlipMutation
 */
public class MutationBinaryParameter extends MutationParameter<BinarySolution> {

  /**
   * Constructs a new MutationBinaryParameter with the specified list of mutation operator names.
   *
   * @param mutationOperators the list of supported mutation operator names (should contain "bitFlip")
   * @throws IllegalArgumentException if mutationOperators is null, empty, or contains invalid values
   */
  public MutationBinaryParameter(List<String> mutationOperators) {
    super(mutationOperators);
    
    // Validate that only supported operators are provided
    mutationOperators.forEach(operator -> {
      if (!"bitFlip".equals(operator)) {
        throw new IllegalArgumentException("Unsupported mutation operator for binary solutions: " + operator);
      }
    });
  }

  /**
   * Creates and returns a configured mutation operator for binary solutions based on the current parameter value.
   *
   * @return a configured mutation operator for binary solutions
   * @throws JMetalException if the operator cannot be created with the current configuration
   * @throws IllegalStateException if required parameters are not set or have invalid values
   */
  @Override
  public MutationOperator<BinarySolution> getMutation() {
    // Get required parameters
    Integer numberOfBitsInASolution = (Integer) nonConfigurableSubParameters().get("numberOfBitsInASolution");
    if (numberOfBitsInASolution == null || numberOfBitsInASolution <= 0) {
      throw new IllegalStateException("numberOfBitsInASolution must be a positive integer");
    }
    
    Double mutationProbabilityFactor = (Double) findGlobalSubParameter("mutationProbabilityFactor").value();
    if (mutationProbabilityFactor == null || mutationProbabilityFactor <= 0) {
      throw new IllegalStateException("mutationProbabilityFactor must be a positive number");
    }
    
    // Calculate actual mutation probability
    double mutationProbability = mutationProbabilityFactor / numberOfBitsInASolution;
    
    // Create and return the appropriate mutation operator
    if ("bitFlip".equals(value())) {
      return new BitFlipMutation<>(mutationProbability);
    } else {
      throw new JMetalException("Unsupported mutation operator: " + value());
    }
  }
}

