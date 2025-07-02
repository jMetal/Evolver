package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter class for configuring mutation operators specifically designed for permutation solutions.
 * <p>
 * This class provides a factory for creating various mutation operators that work with
 * {@link PermutationSolution} instances. It extends {@link MutationParameter} to handle permutation-specific
 * mutation strategies.
 *
 * <p>Supported mutation operators:
 * <ul>
 *   <li><b>swap</b>: Swaps two randomly selected elements in the permutation
 *   <li><b>displacement</b>: Removes a subsequence and reinserts it at a different position
 *   <li><b>insert</b>: Moves an element from one position to another
 *   <li><b>scramble</b>: Randomly reorders a subsequence of the permutation
 *   <li><b>inversion</b>: Reverses the order of a randomly selected subsequence
 *   <li><b>simpleInversion</b>: A simpler version of inversion with fixed subsequence length
 * </ul>
 *
 * <p>Required parameters:
 * <ul>
 *   <li><b>mutationProbability</b>: The probability of applying the mutation operator
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * MutationPermutationParameter mutationParam = new MutationPermutationParameter(
 *     List.of("swap", "insert", "inversion"));
 * mutationParam.setValue("swap");
 * MutationOperator<PermutationSolution<Integer>> mutation = mutationParam.getMutation();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation
 * @see org.uma.jmetal.operator.mutation.impl.DisplacementMutation
 * @see org.uma.jmetal.operator.mutation.impl.InsertMutation
 * @see org.uma.jmetal.operator.mutation.impl.ScrambleMutation
 * @see org.uma.jmetal.operator.mutation.impl.InversionMutation
 * @see org.uma.jmetal.operator.mutation.impl.SimpleInversionMutation
 */
public class PermutationMutationParameter extends MutationParameter<PermutationSolution<Integer>> {

  /** Valid mutation operator names for permutation solutions. */
  private static final List<String> VALID_OPERATORS = 
      List.of("swap", "displacement", "insert", "scramble", "inversion", "simpleInversion");

  /**
   * Constructs a new MutationPermutationParameter with the specified list of mutation operator names.
   *
   * @param mutationOperators the list of supported mutation operator names
   * @throws IllegalArgumentException if mutationOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public PermutationMutationParameter(List<String> mutationOperators) {
    super(mutationOperators);
    
    if (mutationOperators == null || mutationOperators.isEmpty()) {
      throw new IllegalArgumentException("Mutation operators list cannot be null or empty");
    }
    
    // Validate that all provided operators are supported
    mutationOperators.stream()
        .filter(operator -> !VALID_OPERATORS.contains(operator))
        .findFirst()
        .ifPresent(invalidOperator -> {
          throw new JMetalException(
              "Invalid permutation mutation operator: " + invalidOperator + 
              ". Supported operators are: " + VALID_OPERATORS);
        });
  }

  /**
   * Creates and returns a configured mutation operator for permutation solutions based on the current parameter value.
   *
   * @return a configured mutation operator for permutation solutions
   * @throws JMetalException if the operator cannot be created with the current configuration
   * @throws IllegalStateException if required parameters are not set or have invalid values
   * @throws ClassCastException if any parameter has an unexpected type
   */
  @Override
  public MutationOperator<PermutationSolution<Integer>> getMutation() {
    // Validate and get the mutation probability
    Double mutationProbability = getMutationProbability();
    
    // Create and return the appropriate mutation operator
    return switch (value()) {
      case "swap" -> new PermutationSwapMutation<>(mutationProbability);
      case "displacement" -> new DisplacementMutation<>(mutationProbability);
      case "insert" -> new InsertMutation<>(mutationProbability);
      case "scramble" -> new ScrambleMutation<>(mutationProbability);
      case "inversion" -> new InversionMutation<>(mutationProbability);
      case "simpleInversion" -> new SimpleInversionMutation<>(mutationProbability);
      default -> throw new JMetalException("Unsupported permutation mutation operator: " + value());
    };
  }
  
  /**
   * Helper method to get and validate the mutation probability parameter.
   */
  private double getMutationProbability() {
    try {
      Double probability = (Double) findGlobalSubParameter("mutationProbability").value();
      if (probability == null || probability < 0.0 || probability > 1.0) {
        throw new IllegalStateException(
            "mutationProbability must be a number between 0.0 and 1.0");
      }
      return probability;
    } catch (ClassCastException e) {
      throw new IllegalStateException("Invalid mutation probability value", e);
    }
  }
}

