package org.uma.evolver.algorithm.base.moead.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;

/**
 * Parameter space for the MOEA/D algorithm.
 *
 * <p>This class defines all configurable parameters, their relationships, and top-level parameters
 * for the MOEA/D algorithm, supporting both real-coded and permutation-based problems.
 * It is designed to be flexible and extensible, following the same structure as the NSGA-II parameter space classes.
 *
 * <p>Typical parameters include:
 * <ul>
 *   <li>Neighborhood size and replacement settings</li>
 *   <li>Aggregation function and normalization options</li>
 *   <li>Variation, crossover, and mutation operators</li>
 *   <li>Selection strategy and neighborhood selection probability</li>
 *   <li>Archive configuration and algorithm result type</li>
 *   <li>Initial solutions creation and sub-problem ID generator</li>
 * </ul>
 *
 * <p>Parameter relationships and dependencies are also defined, ensuring that the configuration is consistent and valid.
 * Subclasses can extend this class to add or specialize parameters for specific encodings or MOEA/D variants.
 *
 * <p>To configure the parameter space, override the methods {@link #setParameterSpace()},
 * {@link #setParameterRelationships()}, and {@link #setTopLevelParameters()} as needed.
 *
 * <p>Example of extension:
 * <pre>{@code
 * public class MOEADDoubleParameterSpace extends MOEADCommonParameterSpace<DoubleSolution> {
 *   // Add double-specific parameters and relationships here
 * }
 * }</pre>
 *
 * @param <S> the solution type handled by the algorithm
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

  @Override
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
    put(new SequenceGeneratorParameter(SUB_PROBLEM_ID_GENERATOR));
    put(new AggregationFunctionParameter(List.of(
        TSCHEBYSCHEFF, WEIGHTED_SUM, PENALTY_BOUNDARY_INTERSECTION, MODIFIED_TSCHEBYSCHEFF)));
    put(new SelectionParameter<>(List.of(POPULATION_AND_NEIGHBORHOOD_MATING_POOL_SELECTION)));
    put(new DoubleParameter(PBI_THETA, 1.0, 200.0));
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(EXTERNAL_ARCHIVE, POPULATION)));
    put(new ExternalArchiveParameter<>(EXTERNAL_ARCHIVE, List.of(CROWDING_DISTANCE_ARCHIVE, UNBOUNDED_ARCHIVE)));
   }

  @Override
  /**
   * Establishes relationships and dependencies between parameters in the MOEA/D parameter space.
   */
  protected void setParameterRelationships() {
    get(NORMALIZE_OBJECTIVES)
        .addSpecificSubParameter(true, get(EPSILON_PARAMETER_FOR_NORMALIZATION));

    get(AGGREGATION_FUNCTION)
        .addGlobalSubParameter(get(NORMALIZE_OBJECTIVES))
        .addSpecificSubParameter(PENALTY_BOUNDARY_INTERSECTION, get(PBI_THETA));

    // AlgorithmResult dependencies
    get(ALGORITHM_RESULT).addSpecificSubParameter(EXTERNAL_ARCHIVE, get(EXTERNAL_ARCHIVE));

    // Selection dependencies
    get(SELECTION)
        .addSpecificSubParameter(
            POPULATION_AND_NEIGHBORHOOD_MATING_POOL_SELECTION,
            get(NEIGHBORHOOD_SELECTION_PROBABILITY));
  }

  @Override
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
