package org.uma.evolver.algorithm.base.moead.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsBinaryParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.BinaryCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.BinaryMutationParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationBinaryParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Parameter space for the MOEA/D algorithm using binary solutions.
 *
 * <p>This class defines all configurable parameters, their relationships, and top-level parameters
 * for the MOEA/D algorithm when applied to binary-encoded problems. It is designed to be flexible and
 * extensible, following the same structure as the other MOEA/D parameter space classes.
 *
 * <p>Typical parameters include:
 * <ul>
 *   <li>Neighborhood size and replacement settings</li>
 *   <li>Aggregation function and normalization options</li>
 *   <li>Binary-specific variation, crossover, and mutation operators</li>
 *   <li>Selection strategy and neighborhood selection probability</li>
 *   <li>Archive configuration and algorithm result type</li>
 *   <li>Initial solutions creation and sub-problem ID generator</li>
 * </ul>
 *
 * <p>Parameter relationships and dependencies are also defined, ensuring that the configuration is consistent and valid.
 * Subclasses can extend this class to add or specialize parameters for specific MOEA/D variants.
 *
 * <p>To configure the parameter space, override the methods {@link #setParameterSpace()},
 * {@link #setParameterRelationships()}, and {@link #setTopLevelParameters()} as needed.
 *
 * <p>Example of extension:
 * <pre>{@code
 * public class CustomMOEADBinaryParameterSpace extends MOEADBinaryParameterSpace {
 *   // Add custom parameters and relationships here
 * }
 * }</pre>
 *
 * @see MOEADCommonParameterSpace
 */
public class MOEADBinaryParameterSpace extends MOEADCommonParameterSpace<BinarySolution> {
  public MOEADBinaryParameterSpace() {
    super();
    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }


  // Initial solutions creation
  public static final String DEFAULT_STRATEGY = "default";

  // Variation
  public static final String CROSSOVER_AND_MUTATION_VARIATION = "crossoverAndMutationVariation";

  // Crossover
  public static final String CROSSOVER = "crossover";
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";

  // Crossover strategies
  public static final String HUX_CROSSOVER = "HUX";
  public static final String UNIFORM_CROSSOVER = "uniform";
  public static final String SINGLE_POINT_CROSSOVER = "singlePoint";

  // Mutation
  public static final String MUTATION = "mutation";
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";

  // Mutation strategies
  public static final String BIT_FLIP = "bitFlip";

  @Override
  /**
   * Defines and adds all parameters to the parameter space for MOEA/D with binary solutions.
   * This includes neighborhood, aggregation, variation, mutation, crossover, selection, archive, 
   * and initialization parameters specific to binary solutions.
   */
  protected void setParameterSpace() {
    super.setParameterSpace();
    
    // Initial solutions creation
    put(new CreateInitialSolutionsBinaryParameter(List.of(DEFAULT_STRATEGY)));
    
    // Crossover parameters
    put(new BinaryCrossoverParameter(List.of(HUX_CROSSOVER, UNIFORM_CROSSOVER, SINGLE_POINT_CROSSOVER)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.6, 0.9));
    
    // Mutation parameters
    put(new BinaryMutationParameter(List.of(BIT_FLIP)));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 1.0));
    
    // Variation parameters
    put(new VariationBinaryParameter(List.of(CROSSOVER_AND_MUTATION_VARIATION)));
  }

  @Override
  /**
   * Establishes relationships and dependencies between parameters in the MOEA/D parameter space.
   * This ensures that when certain parameters are selected, their required sub-parameters are properly configured.
   */
  protected void setParameterRelationships() {
    super.setParameterRelationships();

    get(CROSSOVER).addGlobalSubParameter(parameterSpace.get(CROSSOVER_PROBABILITY));
    get(MUTATION).addGlobalSubParameter(parameterSpace.get(MUTATION_PROBABILITY_FACTOR));

    get(VARIATION)
            .addGlobalSubParameter(get(MUTATION))
            .addSpecificSubParameter(CROSSOVER_AND_MUTATION_VARIATION, get(CROSSOVER)) ;
  }
}
