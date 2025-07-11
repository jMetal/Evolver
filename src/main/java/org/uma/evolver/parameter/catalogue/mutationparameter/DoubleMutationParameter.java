package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter class for configuring mutation operators specifically designed for double-encoded solutions.
 * <p>
 * This class provides a factory for creating various mutation operators that work with
 * {@link DoubleSolution} instances. It extends {@link MutationParameter} to handle double-specific
 * mutation strategies.
 *
 * <p>Supported mutation operators:
 * <ul>
 *   <li><b>polynomial</b>: Polynomial mutation with configurable distribution index
 *   <li><b>linkedPolynomial</b>: Linked polynomial mutation for correlated variables
 *   <li><b>uniform</b>: Uniform mutation with fixed perturbation
 *   <li><b>nonUniform</b>: Non-uniform mutation with decreasing perturbation over time
 * </ul>
 *
 * <p>Required parameters:
 * <ul>
 *   <li><b>mutationProbabilityFactor</b>: Base probability factor for mutation
 *   <li><b>numberOfProblemVariables</b>: Number of variables in the problem (non-configurable)
 *   <li><b>mutationRepairStrategy</b>: Strategy for repairing solutions after mutation
 *   <li>Operator-specific parameters (see below)
 * </ul>
 *
 * <p>Operator-specific parameters:
 * <ul>
 *   <li><b>polynomial</b>: polynomialMutationDistributionIndex
 *   <li><b>linkedPolynomial</b>: linkedPolynomialMutationDistributionIndex
 *   <li><b>uniform</b>: uniformMutationPerturbation
 *   <li><b>nonUniform</b>: nonUniformMutationPerturbation, maxIterations
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * DoubleMutationParameter mutationParam = new DoubleMutationParameter(List.of("polynomial", "uniform"));
 * mutationParam.setValue("polynomial");
 * MutationOperator<DoubleSolution> mutation = mutationParam.getMutation();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.operator.mutation.impl.PolynomialMutation
 * @see org.uma.jmetal.operator.mutation.impl.LinkedPolynomialMutation
 * @see org.uma.jmetal.operator.mutation.impl.UniformMutation
 * @see org.uma.jmetal.operator.mutation.impl.NonUniformMutation
 */
public class DoubleMutationParameter extends MutationParameter<DoubleSolution> {
  /** List of valid mutation operator names for double solutions. */
  private static final List<String> VALID_MUTATION_NAMES = 
      List.of("polynomial", "linkedPolynomial", "uniform", "nonUniform", "levyFlight", "powerLaw");

  /**
   * Constructs a new DoubleMutationParameter with the specified list of mutation operator names.
   *
   * @param mutationOperators the list of supported mutation operator names
   * @throws IllegalArgumentException if mutationOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public DoubleMutationParameter(List<String> mutationOperators) {
    super(mutationOperators);

    if (mutationOperators == null || mutationOperators.isEmpty()) {
      throw new IllegalArgumentException("Mutation operators list cannot be null or empty");
    }

    // Validate that all provided operators are supported
    mutationOperators.stream()
        .filter(operator -> !VALID_MUTATION_NAMES.contains(operator))
        .findFirst()
        .ifPresent(invalidOperator -> {
          throw new JMetalException(
              "Invalid mutation operator: " + invalidOperator + 
              ". Supported operators are: " + VALID_MUTATION_NAMES);
        });
  }

  /**
   * Creates and returns a configured mutation operator for double solutions based on the current parameter value.
   *
   * @return a configured mutation operator for double solutions
   * @throws JMetalException if the operator cannot be created with the current configuration
   * @throws IllegalStateException if required parameters are not set or have invalid values
   * @throws ClassCastException if any parameter has an unexpected type
   */
  @Override
  public MutationOperator<DoubleSolution> getMutation() {
    // Validate and get common parameters
    int numberOfProblemVariables = getNonNegativeIntParameter("numberOfProblemVariables");
    double mutationProbabilityFactor = (Double) findGlobalSubParameter("mutationProbabilityFactor").value();
    double mutationProbability = mutationProbabilityFactor / numberOfProblemVariables;
    
    RepairDoubleSolutionStrategyParameter repairDoubleSolution = getRepairStrategy();
    
    // Create and return the appropriate mutation operator
    return switch (value()) {
      case "polynomial" -> createPolynomialMutation(mutationProbability, repairDoubleSolution);
      case "linkedPolynomial" -> createLinkedPolynomialMutation(mutationProbability, repairDoubleSolution);
      case "uniform" -> createUniformMutation(mutationProbability, repairDoubleSolution);
      case "nonUniform" -> createNonUniformMutation(mutationProbability, repairDoubleSolution);
      case "levyFlight" -> createLevyFlightMutation(mutationProbability, repairDoubleSolution);
      case "powerLaw" -> createPowerLawMutation(mutationProbability, repairDoubleSolution);
      default -> throw new JMetalException("Unsupported mutation operator: " + value());
    };
  }

  /**
   * Creates a LevyFlightMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createLevyFlightMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double beta = (Double) findConditionalSubParameter("levyFlightMutationBeta").value();
    double stepSize = (Double) findConditionalSubParameter("levyFlightMutationStepSize").value();
    return new LevyFlightMutation(
        mutationProbability, 
        beta, 
        stepSize, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Creates a PowerLawMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createPowerLawMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double delta = (Double) findConditionalSubParameter("powerLawMutationDelta").value();
    return new PowerLawMutation(
        mutationProbability, 
        delta, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Creates a PolynomialMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createPolynomialMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double distributionIndex = (Double) findConditionalSubParameter("polynomialMutationDistributionIndex").value();
    return new PolynomialMutation(
        mutationProbability, 
        distributionIndex, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Creates a LinkedPolynomialMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createLinkedPolynomialMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double distributionIndex = (Double) findConditionalSubParameter("linkedPolynomialMutationDistributionIndex").value();
    return new LinkedPolynomialMutation(
        mutationProbability, 
        distributionIndex, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Creates a UniformMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createUniformMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double perturbation = (Double) findConditionalSubParameter("uniformMutationPerturbation").value();
    return new UniformMutation(
        mutationProbability, 
        perturbation, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Creates a NonUniformMutation operator with the given parameters.
   */
  private MutationOperator<DoubleSolution> createNonUniformMutation(
      double mutationProbability, 
      RepairDoubleSolutionStrategyParameter repairStrategy) {
    
    double perturbation = (Double) findConditionalSubParameter("nonUniformMutationPerturbation").value();
    int maxIterations = (Integer) nonConfigurableSubParameters().get("maxIterations");
    return new NonUniformMutation(
        mutationProbability, 
        perturbation, 
        maxIterations, 
        repairStrategy.getRepairDoubleSolutionStrategy());
  }
  
  /**
   * Helper method to get a non-negative integer parameter.
   */
  private int getNonNegativeIntParameter(String paramName) {
    Integer value = (Integer) nonConfigurableSubParameters().get(paramName);
    if (value == null || value < 0) {
      throw new IllegalStateException(paramName + " must be a non-negative integer");
    }
    return value;
  }
  
  /**
   * Helper method to get the repair strategy parameter.
   */
  private RepairDoubleSolutionStrategyParameter getRepairStrategy() {
    try {
      return (RepairDoubleSolutionStrategyParameter) findGlobalSubParameter("mutationRepairStrategy");
    } catch (ClassCastException e) {
      throw new IllegalStateException("Invalid mutation repair strategy configuration", e);
    }
  }
}
