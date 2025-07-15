package org.uma.evolver.algorithm.base.moead.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsPermutationParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.PermutationCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.PermutationMutationParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationDoubleParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Parameter space for the MOEA/D algorithm using permutation-based solutions.
 *
 * <p>This class defines all configurable parameters, their relationships, and top-level parameters
 * for the MOEA/D algorithm when applied to permutation problems. It is designed to be flexible and
 * extensible, following the same structure as the NSGA-II and MOEA/D double parameter space classes.
 *
 * <p>Typical parameters include:
 * <ul>
 *   <li>Neighborhood size and replacement settings</li>
 *   <li>Aggregation function and normalization options</li>
 *   <li>Permutation-based variation, crossover, and mutation operators</li>
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
 * public class CustomMOEADPermutationParameterSpace extends MOEADPermutationParameterSpace {
 *   // Add custom parameters and relationships here
 * }
 * }</pre>
 *
 * @see MOEADCommonParameterSpace
 */
public class MOEADPermutationParameterSpace extends MOEADCommonParameterSpace<PermutationSolution<Integer>> {
  public MOEADPermutationParameterSpace() {
    super();
    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }

  @Override
  public MOEADPermutationParameterSpace createInstance() {
    return new MOEADPermutationParameterSpace();
  }

  // Initial solutions creation
  public static final String DEFAULT = "default";

  // Variation
  public static final String CROSSOVER_AND_MUTATION_VARIATION = "crossoverAndMutationVariation";

  // Crossover
  public static final String CROSSOVER = "crossover";
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";

  // Crossover strategies
  public static final String PMX = "PMX";
  public static final String OXD = "OXD";
  public static final String CX = "CX";

  // Mutation
  public static final String MUTATION = "mutation";
  public static final String MUTATION_PROBABILITY = "mutationProbability";

  // Mutation strategies
  public static final String SWAP = "swap";
  public static final String INSERT = "insert";
  public static final String SCRAMBLE = "scramble";
  public static final String INVERSION = "inversion";
  public static final String SIMPLE_INVERSION = "simpleInversion";
  public static final String DISPLACEMENT = "displacement";

  @Override
  /**
   * Defines and adds all parameters to the parameter space for MOEA/D.
   * This includes neighborhood, aggregation, variation, mutation, crossover, selection, archive, and initialization parameters.
   */
  protected void setParameterSpace() {
    super.setParameterSpace();
    put(new CreateInitialSolutionsPermutationParameter(List.of(DEFAULT)));
    put(new PermutationCrossoverParameter(List.of(PMX, OXD, CX)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.6, 0.9));
    put(new PermutationMutationParameter(List.of(SWAP, INSERT, SCRAMBLE, INVERSION, SIMPLE_INVERSION, DISPLACEMENT)));
    put(new DoubleParameter(MUTATION_PROBABILITY, 0.05, 0.1));
    put(new VariationDoubleParameter(List.of(CROSSOVER_AND_MUTATION_VARIATION)));
  }

  @Override
  /**
   * Establishes relationships and dependencies between parameters in the MOEA/D parameter space.
   */
  protected void setParameterRelationships() {
    super.setParameterRelationships();
    // Variation dependencies
    get(CROSSOVER).addGlobalSubParameter(parameterSpace.get(CROSSOVER_PROBABILITY));
    get(MUTATION).addGlobalSubParameter(parameterSpace.get(MUTATION_PROBABILITY));

    get(VARIATION)
            .addGlobalSubParameter(get(MUTATION))
            .addConditionalParameter(CROSSOVER_AND_MUTATION_VARIATION, get(CROSSOVER)) ;
  }
}
