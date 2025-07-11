package org.uma.evolver.algorithm.base.ssmoea.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationDoubleParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract parameter space for NSGA-II algorithm variants.
 *
 * &lt;p&gt;This class defines the common configurable parameters, their relationships, and
 * top-level parameters for any NSGA-II implementation (e.g., real-coded, permutation-based).
 * Subclasses should extend this class and add any additional parameters or relationships
 * specific to their encoding.
 *
 * &lt;p&gt;Typical parameters include:
 * &lt;ul&gt;
 *   &lt;li&gt;Algorithm result type (population or external archive)</li>
 *   &lt;li&gt;Population size (with or without archive)</li>
 *   &lt;li&gt;Initial solutions creation strategy</li>
 *   &lt;li&gt;Variation (crossover and mutation)</li>
 *   &lt;li&gt;Selection operator and its configuration</li>
 * </ul>
 *
 * &lt;p&gt;Parameter relationships and dependencies are also defined, ensuring that the
 * configuration is consistent and valid.
 *
 * &lt;p&gt;Subclasses should override {@link #setParameterSpace()}, {@link #setParameterRelationships()},
 * and {@link #setTopLevelParameters()} as needed.
 *
 * &lt;p&gt;Example of extension:
 * <pre&gt;{@code
 * public class NSGAIIDoubleParameterSpace extends NSGAIICommonParameterSpace&lt;DoubleSolution&gt; {
 *   // Add double-specific parameters and relationships here
 * }
 * }</pre>
 *
 * @param S the solution type handled by the algorithm
 */
public abstract class SSMOEACommonParameterSpace<S extends Solution<?>> extends ParameterSpace {
  // Common parameters for any NSGA-II variant

  // Algorithm result options
  public static final String ALGORITHM_RESULT = "algorithmResult";
  public static final String POPULATION = "population";
  public static final String EXTERNAL_ARCHIVE = "externalArchive";
  public static final String ARCHIVE_TYPE = "archiveType";
  public static final String POPULATION_SIZE_WITH_ARCHIVE = "populationSizeWithArchive";

  // External archive types
  public static final String CROWDING_DISTANCE_ARCHIVE = "crowdingDistanceArchive";
  public static final String UNBOUNDED_ARCHIVE = "unboundedArchive";

  // Initial solutions creation
  public static final String CREATE_INITIAL_SOLUTIONS = "createInitialSolutions";

  // Variation
  public static final String VARIATION = "variation";
  public static final String CROSSOVER_AND_MUTATION_VARIATION = "crossoverAndMutationVariation";
  public static final String DIFFERENTIAL_EVOLUTION_VARIATION = "differentialEvolutionVariation";
  public static final String OFFSPRING_POPULATION_SIZE = "offspringPopulationSize";

  // Crossover
  public static final String CROSSOVER = "crossover";

  // Mutation
  public static final String MUTATION = "mutation";

  // Selection
  public static final String SELECTION = "selection";

  // Selection strategies
  public static final String TOURNAMENT = "tournament";
  public static final String SELECTION_TOURNAMENT_SIZE = "selectionTournamentSize";
  public static final String RANDOM_SELECTION = "random";

  /**
   * Defines and adds all common parameters to the parameter space for NSGA-II variants.
   * &lt;p&gt;
   * This includes parameters such as algorithm result type, population size with archive,
   * external archive type, offspring population size, variation, selection strategy, and
   * tournament size. Subclasses should call super and then add their own specific parameters.
   * </p>
   */
  protected void setParameterSpace() {
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(POPULATION, EXTERNAL_ARCHIVE)));
    put(new IntegerParameter(POPULATION_SIZE_WITH_ARCHIVE, 10, 200));
    put(
        new ExternalArchiveParameter<>(
                ARCHIVE_TYPE, List.of(CROWDING_DISTANCE_ARCHIVE, UNBOUNDED_ARCHIVE)));

    put(
        new CategoricalIntegerParameter(
            OFFSPRING_POPULATION_SIZE, List.of(1, 2, 5, 10, 20, 50, 100, 200, 400)));
    put(new VariationDoubleParameter(List.of(CROSSOVER_AND_MUTATION_VARIATION)));

    put(new SelectionParameter<>(List.of(TOURNAMENT, RANDOM_SELECTION)));
    put(new IntegerParameter(SELECTION_TOURNAMENT_SIZE, 2, 10));
  }

  /**
   * Establishes relationships and dependencies between parameters in the parameter space.
   * &lt;p&gt;
   * For example, if the algorithm result is set to use an external archive, then the
   * population size with archive and the external archive type become relevant sub-parameters.
   * Similarly, the variation and selection parameters may have their own specific sub-parameters.
   * </p>
   */
  protected void setParameterRelationships() {
    // AlgorithmResult dependencies
    get(ALGORITHM_RESULT)
        .addConditionalSubParameter(EXTERNAL_ARCHIVE, get(POPULATION_SIZE_WITH_ARCHIVE))
        .addConditionalSubParameter(EXTERNAL_ARCHIVE, get(ARCHIVE_TYPE));

    get(VARIATION)
        .addConditionalSubParameter(CROSSOVER_AND_MUTATION_VARIATION, get(CROSSOVER))
        .addConditionalSubParameter(CROSSOVER_AND_MUTATION_VARIATION, get(MUTATION));

    get(SELECTION).addConditionalSubParameter(TOURNAMENT, get(SELECTION_TOURNAMENT_SIZE));
  }

  /**
   * Identifies and adds the top-level parameters to the list.
   * &lt;p&gt;
   * Top-level parameters are the main entry points for user configuration and are typically
   * exposed in user interfaces or command-line tools.
   * </p>
   */
  protected void setTopLevelParameters() {
    topLevelParameters.add(parameterSpace.get(ALGORITHM_RESULT));
    topLevelParameters.add(parameterSpace.get(CREATE_INITIAL_SOLUTIONS));
    topLevelParameters.add(parameterSpace.get(OFFSPRING_POPULATION_SIZE));
    topLevelParameters.add(parameterSpace.get(VARIATION));
    topLevelParameters.add(parameterSpace.get(SELECTION));
  }
}
