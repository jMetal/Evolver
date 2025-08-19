package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.DifferentialEvolutionCrossoverParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.DifferentialEvolutionCrossoverVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

/**
 * A parameter class for configuring variation operators specifically designed for double-encoded solutions.
 * <p>
 * This class provides a factory for creating variation operators that work with
 * {@link DoubleSolution} instances. It extends {@link VariationParameter} to handle double-specific
 * variation strategies, including standard genetic operators and differential evolution.
 *
 * <p>Supported variation strategies:
 * <ul>
 *   <li><b>crossoverAndMutationVariation</b>: Applies both crossover and mutation operators
 *       to create new offspring solutions
 *   <li><b>differentialEvolutionVariation</b>: Uses differential evolution crossover
 *       combined with mutation
 * </ul>
 *
 * <p>Required parameters for "crossoverAndMutationVariation":
 * <ul>
 *   <li><b>offspringPopulationSize</b>: The number of offspring solutions to generate
 *   <li><b>crossover</b>: Configuration for the crossover operator
 *   <li><b>mutation</b>: Configuration for the mutation operator (can be global or specific)
 * </ul>
 *
 * <p>Required parameters for "differentialEvolutionVariation":
 * <ul>
 *   <li><b>differentialEvolutionCrossover</b>: Configuration for the differential evolution crossover
 *   <li><b>mutation</b>: Configuration for the mutation operator (can be global or specific)
 *   <li><b>subProblemIdGenerator</b>: A sequence generator for sub-problem indices (non-configurable)
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * // For standard genetic variation
 * VariationDoubleParameter variationParam = new VariationDoubleParameter(
 *     List.of("crossoverAndMutationVariation"));
 * // Configure sub-parameters...
 * Variation<DoubleSolution> variation = variationParam.getVariation();
 * 
 * // For differential evolution
 * VariationDoubleParameter deVariationParam = new VariationDoubleParameter(
 *     List.of("differentialEvolutionVariation"));
 * // Configure DE-specific parameters...
 * Variation<DoubleSolution> deVariation = deVariationParam.getVariation();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation
 * @see org.uma.jmetal.component.catalogue.ea.variation.impl.DifferentialEvolutionCrossoverVariation
 */
public class DoubleVariationParameter extends VariationParameter<DoubleSolution> {
  
  /** Variation strategy that combines crossover and mutation. */
  private static final String CROSSOVER_AND_MUTATION = "crossoverAndMutationVariation";
  
  /** Variation strategy that uses differential evolution. */
  private static final String DIFFERENTIAL_EVOLUTION = "differentialEvolutionVariation";

  /**
   * Constructs a new VariationDoubleParameter with the specified list of variation strategy names.
   *
   * @param variationStrategies the list of supported variation strategy names
   * @throws IllegalArgumentException if variationStrategies is null, empty, or contains invalid values
   * @throws JMetalException if any strategy name is not supported
   */
  public DoubleVariationParameter(List<String> variationStrategies) {
    this(DEFAULT_NAME, variationStrategies);
  }

  /**
   * Constructs a new VariationDoubleParameter with the specified list of variation strategy names.
   *
   * @param variationStrategies the list of supported variation strategy names
   * @throws IllegalArgumentException if variationStrategies is null, empty, or contains invalid values
   * @throws JMetalException if any strategy name is not supported
   */
  public DoubleVariationParameter(String name, List<String> variationStrategies) {
    super(variationStrategies);
    
    if (variationStrategies == null || variationStrategies.isEmpty()) {
      throw new IllegalArgumentException("Variation strategies list cannot be null or empty");
    }
    
    // Validate that all provided strategies are supported
    variationStrategies.stream()
        .filter(strategy -> !CROSSOVER_AND_MUTATION.equals(strategy) && 
                           !DIFFERENTIAL_EVOLUTION.equals(strategy))
        .findFirst()
        .ifPresent(invalidStrategy -> {
          throw new JMetalException(
              "Invalid variation strategy for double solutions: " + invalidStrategy + 
              ". Supported strategies: " + String.join(", ", 
                  List.of(CROSSOVER_AND_MUTATION, DIFFERENTIAL_EVOLUTION)));
        });
  }

  /**
   * Creates and returns a configured variation operator for double solutions based on the current parameter value.
   *
   * @return a configured variation operator for double solutions
   * @throws JMetalException if the operator cannot be created with the current configuration
   * @throws IllegalStateException if required parameters are not set or have invalid values
   * @throws ClassCastException if any parameter has an unexpected type
   */
  @Override
  @SuppressWarnings("unchecked")
  public Variation<DoubleSolution> getVariation() {
    // Try to get the global mutation parameter if it exists
    MutationParameter<DoubleSolution> mutationParameter = 
        (MutationParameter<DoubleSolution>) findGlobalSubParameter("mutation");

    switch (value()) {
      case CROSSOVER_AND_MUTATION -> {
        return createCrossoverAndMutationVariation(mutationParameter);
      }
      case DIFFERENTIAL_EVOLUTION -> {
        return createDifferentialEvolutionVariation(mutationParameter);
      }
      default -> throw new JMetalException("Unsupported variation strategy: " + value());
    }
  }
  
  /**
   * Creates a crossover and mutation variation operator.
   */
  private Variation<DoubleSolution> createCrossoverAndMutationVariation(
      MutationParameter<DoubleSolution> mutationParameter) {
    
    // Get and validate the offspring population size
    int offspringPopulationSize = getOffspringPopulationSize();
    
    // Get and validate the crossover parameter
    CrossoverParameter<DoubleSolution> crossoverParameter = getCrossoverParameter();
    CrossoverOperator<DoubleSolution> crossoverOperator = crossoverParameter.getCrossover();
    
    // Get and validate the mutation parameter (either global or specific)
    MutationOperator<DoubleSolution> mutationOperator = getMutationOperator(mutationParameter);
    
    return new CrossoverAndMutationVariation<>(
        offspringPopulationSize, 
        crossoverOperator, 
        mutationOperator);
  }
  
  /**
   * Creates a differential evolution variation operator.
   */
  @SuppressWarnings("unchecked")
  private Variation<DoubleSolution> createDifferentialEvolutionVariation(
      MutationParameter<DoubleSolution> mutationParameter) {
    
    // Get and validate the differential evolution crossover parameter
    var deCrossoverParameter = (DifferentialEvolutionCrossoverParameter)
        findConditionalParameter("differentialEvolutionCrossover");
    if (deCrossoverParameter == null) {
      throw new JMetalException("differentialEvolutionCrossover parameter not found");
    }
    
    // Get and validate the mutation parameter (either global or specific)
    MutationOperator<DoubleSolution> mutationOperator = getMutationOperator(mutationParameter);
    
    // Get and validate the sub-problem ID generator
    var subProblemIdGenerator = (SequenceGenerator<Integer>) 
        nonConfigurableSubParameters().get("subProblemIdGenerator");
    if (subProblemIdGenerator == null) {
      throw new JMetalException("subProblemIdGenerator not found");
    }
    
    // Create and return the differential evolution variation
    return new DifferentialEvolutionCrossoverVariation(
        1, // Always create one offspring per variation
        deCrossoverParameter.getParameter(),
        mutationOperator,
        subProblemIdGenerator);
  }
  
  /**
   * Helper method to get and validate the offspring population size parameter.
   */
  private int getOffspringPopulationSize() {
    Integer size = (Integer) nonConfigurableSubParameters().get("offspringPopulationSize");
    if (size == null || size <= 0) {
      throw new IllegalStateException(
          "offspringPopulationSize must be a positive integer");
    }
    return size;
  }
  
  /**
   * Helper method to get and validate the crossover parameter.
   */
  @SuppressWarnings("unchecked")
  private CrossoverParameter<DoubleSolution> getCrossoverParameter() {
    return (CrossoverParameter<DoubleSolution>) findConditionalParameter("crossover");
  }
  
  /**
   * Helper method to get and validate the mutation operator, trying both global and specific parameters.
   */
  @SuppressWarnings("unchecked")
  private MutationOperator<DoubleSolution> getMutationOperator(
      MutationParameter<DoubleSolution> globalMutationParameter) {
    
    // Try to use the global mutation parameter if it exists
    if (globalMutationParameter != null) {
      return globalMutationParameter.getMutation();
    }
    
    // Otherwise, try to get a specific mutation parameter
    MutationParameter<DoubleSolution> specificMutationParameter = 
        (MutationParameter<DoubleSolution>) findConditionalParameter("mutation");
    if (specificMutationParameter == null) {
      throw new JMetalException("No mutation parameter found");
    }
    
    return specificMutationParameter.getMutation();
  }
}

