package org.uma.evolver.algorithm.base.moead.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;

/**
 * Base parameter space for the MOEA/D algorithm.
 * <p>
 * This abstract class defines the common parameter space for the MOEA/D algorithm, providing a
 * standardized structure for configuring the algorithm's components. It is designed to be
 * extended by concrete implementations for specific solution types (e.g., double or permutation).
 * </p>
 * <p>
 * Key components managed by this parameter space include:
 * <ul>
 *   <li><strong>Neighborhood Management:</strong>
 *     <ul>
 *       <li>Neighborhood size</li>
 *       <li>Maximum number of replaced solutions</li>
 *       <li>Neighborhood selection probability</li>
 *     </ul>
 *   </li>
 *   <li><strong>Aggregation Function:</strong>
 *     <ul>
 *       <li>Choice of aggregation function (e.g., Tchebycheff, PBI)</li>
 *       <li>Objective normalization settings</li>
 *       <li>PBI theta parameter</li>
 *     </ul>
 *   </li>
 *   <li><strong>Variation Operators:</strong>
 *     <ul>
 *       <li>Crossover operators</li>
 *       <li>Mutation operators</li>
 *       <li>Operator probabilities and parameters</li>
 *     </ul>
 *   </li>
 *   <li><strong>Selection:</strong>
 *     <ul>
 *       <li>Selection strategy</li>
 *       <li>Neighborhood-based selection probability</li>
 *     </ul>
 *   </li>
 *   <li><strong>Algorithm Configuration:</strong>
 *     <ul>
 *       <li>Population size</li>
 *       <li>Archive settings</li>
 *       <li>Initial solutions creation</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 * <p>
 * The parameter space is designed to be extensible, allowing subclasses to:
 * <ul>
 *   <li>Add solution-type specific parameters</li>
 *   <li>Define specific parameter relationships</li>
 *   <li>Set solution-type specific default values</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * public class MOEADDoubleParameterSpace extends MOEADCommonParameterSpace<DoubleSolution> {
 *   // Add double-specific parameters and relationships
 *   protected void setParameterSpace() {
 *     super.setParameterSpace();
 *     // Add double-specific parameters here
 *   }
 * }
 * }</pre>
 * </p>
 *
 * @param <S> the solution type handled by the algorithm (e.g., DoubleSolution, PermutationSolution)
 * @author Antonio J. Nebro
 * @see ParameterSpace
 * @see CategoricalParameter
 * @see DoubleParameter
 * @see IntegerParameter
 * @since 1.0
 */
public abstract class MOEADCommonParameterSpace<S extends Solution<?>> extends ParameterSpace {
  // Neighborhood parameters
  public static final String NEIGHBORHOOD_SIZE = "neighborhoodSize";
  public static final String MAXIMUM_NUMBER_OF_REPLACED_SOLUTIONS = "maximumNumberOfReplacedSolutions";

  // Aggregation function parameters
  public static final String AGGREGATION_FUNCTION = "aggregationFunction";
  public static final String NORMALIZE_OBJECTIVES = "normalizeObjectives";
  public static final String EPSILON_PARAMETER_FOR_NORMALIZATION = "epsilonParameterForNormalization";
  public static final String PBI_THETA = "pbiTheta";

  // Algorithm result parameters
  public static final String ALGORITHM_RESULT = "algorithmResult";
  public static final String EXTERNAL_ARCHIVE = "externalArchive";

  // Variation parameters
  public static final String VARIATION = "variation";

  // Selection parameters
  public static final String SELECTION = "selection";
  public static final String POPULATION_AND_NEIGHBORHOOD_MATING_POOL_SELECTION = "populationAndNeighborhoodMatingPoolSelection";
  public static final String NEIGHBORHOOD_SELECTION_PROBABILITY = "neighborhoodSelectionProbability";

  // Aggregation function values
  public static final String TSCHEBYSCHEFF = "tschebyscheff";
  public static final String WEIGHTED_SUM = "weightedSum";
  public static final String PENALTY_BOUNDARY_INTERSECTION = "penaltyBoundaryIntersection";
  public static final String MODIFIED_TSCHEBYSCHEFF = "modifiedTschebyscheff";

  // Initial solutions creation
  public static final String CREATE_INITIAL_SOLUTIONS = "createInitialSolutions";
  public static final String DEFAULT = "default";

  // Archive values
  public static final String CROWDING_DISTANCE_ARCHIVE = "crowdingDistanceArchive";
  public static final String UNBOUNDED_ARCHIVE = "unboundedArchive";

  // Common result types
  public static final String POPULATION = "population";

  // Sequence generator types
  public static final String SUB_PROBLEM_ID_GENERATOR = "subProblemIdGenerator";
  public static final String PERMUTATION = "permutation";
  public static final String INTEGER_SEQUENCE = "integerPermutation";

  /**
   * Defines and adds all parameters to the parameter space for MOEA/D.
   * This includes neighborhood, aggregation, variation, mutation, crossover, selection, archive, and initialization parameters.
   */
  protected void setParameterSpace() {
    put(new BooleanParameter(NORMALIZE_OBJECTIVES));
    put(new DoubleParameter(EPSILON_PARAMETER_FOR_NORMALIZATION, 1.e-8, 25.0));
    put(new IntegerParameter(NEIGHBORHOOD_SIZE, 5, 50));
    put(new ProbabilityParameter(NEIGHBORHOOD_SELECTION_PROBABILITY));
    put(new IntegerParameter(MAXIMUM_NUMBER_OF_REPLACED_SOLUTIONS, 1, 5));
    put(new SequenceGeneratorParameter(SUB_PROBLEM_ID_GENERATOR, List.of("randomPermutationCycle", "cyclicIntegerSequence")));
    put(new AggregationFunctionParameter(List.of(
        TSCHEBYSCHEFF, WEIGHTED_SUM, PENALTY_BOUNDARY_INTERSECTION, MODIFIED_TSCHEBYSCHEFF)));
    put(new SelectionParameter<>(List.of(POPULATION_AND_NEIGHBORHOOD_MATING_POOL_SELECTION)));
    put(new DoubleParameter(PBI_THETA, 1.0, 200.0));
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(EXTERNAL_ARCHIVE, POPULATION)));
    put(new ExternalArchiveParameter<>(EXTERNAL_ARCHIVE, List.of(CROWDING_DISTANCE_ARCHIVE, UNBOUNDED_ARCHIVE)));
   }

  /**
   * Establishes relationships and dependencies between parameters in the MOEA/D parameter space.
   */
  protected void setParameterRelationships() {
    get(NORMALIZE_OBJECTIVES)
        .addConditionalParameter(true, get(EPSILON_PARAMETER_FOR_NORMALIZATION));

    get(AGGREGATION_FUNCTION)
        .addGlobalSubParameter(get(NORMALIZE_OBJECTIVES))
        .addConditionalParameter(PENALTY_BOUNDARY_INTERSECTION, get(PBI_THETA));

    // AlgorithmResult dependencies
    get(ALGORITHM_RESULT).addConditionalParameter(EXTERNAL_ARCHIVE, get(EXTERNAL_ARCHIVE));

    // Selection dependencies
    get(SELECTION)
        .addConditionalParameter(
            POPULATION_AND_NEIGHBORHOOD_MATING_POOL_SELECTION,
            get(NEIGHBORHOOD_SELECTION_PROBABILITY));
  }

  /**
   * Identifies and adds the top-level parameters for MOEA/D.
   * These are the main entry points for user configuration.
   */
  protected void setTopLevelParameters() {
    topLevelParameters.add(parameterSpace.get(NEIGHBORHOOD_SIZE));
    topLevelParameters.add(parameterSpace.get(MAXIMUM_NUMBER_OF_REPLACED_SOLUTIONS));
    topLevelParameters.add(parameterSpace.get(AGGREGATION_FUNCTION));
    topLevelParameters.add(parameterSpace.get(ALGORITHM_RESULT));
    topLevelParameters.add(parameterSpace.get(SUB_PROBLEM_ID_GENERATOR));
    topLevelParameters.add(parameterSpace.get(CREATE_INITIAL_SOLUTIONS));
    topLevelParameters.add(parameterSpace.get(VARIATION));
    topLevelParameters.add(parameterSpace.get(SELECTION));
  }
}
