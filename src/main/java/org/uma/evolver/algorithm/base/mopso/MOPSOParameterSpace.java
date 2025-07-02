package org.uma.evolver.algorithm.base.mopso;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.VelocityInitializationParameter;
import org.uma.evolver.parameter.catalogue.VelocityUpdateParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parameter space configuration for Multi-Objective Particle Swarm Optimization (MOPSO) algorithms.
 *
 * <p>This class defines the configuration space for MOPSO algorithms, including parameters for
 * swarm initialization, velocity update, perturbation, and other components.
 *
 * @author Antonio J. Nebro
 */
public class MOPSOParameterSpace extends ParameterSpace {

  // Parameter names
  public static final String SWARM_SIZE = "swarmSize";
  public static final String LEADER_ARCHIVE = "leaderArchive";
  public static final String UNBOUNDED_ARCHIVE = "unboundedArchive";
  public static final String HYPERVOLUME_ARCHIVE = "hypervolumeArchive";
  public static final String ALGORITHM_RESULT = "algorithmResult";
  public static final String CROWDING_DISTANCE_ARCHIVE = "crowdingDistanceArchive";
  public static final String SPATIAL_SPREAD_DEVIATION_ARCHIVE = "spatialSpreadDeviationArchive";

  public static final String SWARM_INITIALIZATION = "swarmInitialization";
  public static final String DEFAULT_INITIALIZATION = "default";
  public static final String LATIN_HYPERCUBE_SAMPLING_INITIALIZATION = "latinHypercubeSampling";
  public static final String SCATTER_SEARCH_INITIALIZATION = "scatterSearch";

  public static final String VELOCITY_INITIALIZATION = "velocityInitialization";
  public static final String DEFAULT_VELOCITY_INITIALIZATION = "defaultVelocityInitialization";
  public static final String SPSO2007_VELOCITY_INITIALIZATION = "SPSO2007VelocityInitialization";
  public static final String SPSO2011_VELOCITY_INITIALIZATION = "SPSO2011VelocityInitialization";

  public static final String PERTURBATION = "perturbation";
  public static final String FREQUENCY_SELECTION_MUTATION_BASED_PERTURBATION =
      "frequencySelectionMutationBasedPerturbation";

  public static final String MUTATION = "mutation";
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";
  public static final String MUTATION_REPAIR_STRATEGY = "mutationRepairStrategy";
  // Mutation strategies
  public static final String UNIFORM = "uniform";
  public static final String POLYNOMIAL = "polynomial";
  public static final String LINKED_POLYNOMIAL = "linkedPolynomial";
  public static final String NON_UNIFORM = "nonUniform";
  public static final String POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "polynomialMutationDistributionIndex";
  public static final String LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "linkedPolynomialMutationDistributionIndex";
  public static final String UNIFORM_MUTATION_PERTURBATION = "uniformMutationPerturbation";
  public static final String NON_UNIFORM_MUTATION_PERTURBATION = "nonUniformMutationPerturbation";

  // Repair strategy values
  public static final String REPAIR_RANDOM = "random";
  public static final String REPAIR_ROUND = "round";
  public static final String REPAIR_BOUNDS = "bounds";

  public static final String FREQUENCY_SELECTION_OF_MUTATION_BASED_PERTURBATION =
      "frequencySelectionOfMutationBasedPerturbation";

  public static final String FREQUENCY_OF_APPLICATION_OF_MUTATION_OPERATOR =
      "frequencyOfApplicationOfMutationOperator";

  public static final String INERTIA_WEIGHT_COMPUTING_STRATEGY = "inertiaWeightComputingStrategy";

  public static final String WEIGHT = "weight";
  public static final String WEIGHT_MIN = "weightMin";
  public static final String WEIGHT_MAX = "weightMax";

  public static final String VELOCITY_UPDATE = "velocityUpdate";
  public static final String DEFAULT_VELOCITY_UPDATE = "defaultVelocityUpdate";
  public static final String SPSO2011_VELOCITY_UPDATE = "SPSO2011VelocityUpdate";
  public static final String CONSTRAINED_VELOCITY_UPDATE = "constrainedVelocityUpdate";
  public static final String C1_MIN = "c1Min";
  public static final String C1_MAX = "c1Max";
  public static final String C2_MIN = "c2Min";
  public static final String C2_MAX = "c2Max";

  public static final String LOCAL_BEST_INITIALIZATION = "localBestInitialization";
  public static final String DEFAULT_LOCAL_BEST_INITIALIZATION = "defaultLocalBestInitialization";
  public static final String LOCAL_BEST_UPDATE = "localBestUpdate";
  public static final String DEFAULT_LOCAL_BEST_UPDATE = "defaultLocalBestUpdate";
  public static final String GLOBAL_BEST_INITIALIZATION = "globalBestInitialization";
  public static final String DEFAULT_GLOBAL_BEST_INITIALIZATION = "defaultGlobalBestInitialization";
  public static final String GLOBAL_BEST_UPDATE = "globalBestUpdate";
  public static final String DEFAULT_GLOBAL_BEST_UPDATE = "defaultGlobalBestUpdate";
  public static final String GLOBAL_BEST_SELECTION = "globalBestSelection";
  public static final String TOURNAMENT_SELECTION = "tournamentSelection";
  public static final String SELECTION_TOURNAMENT_SIZE = "selectionTournamentSize";
  public static final String RANDOM_SELECTION = "randomSelection";
  public static final String POSITION_UPDATE = "positionUpdate";
  public static final String DEFAULT_POSITION_UPDATE = "defaultPositionUpdate";

  public static final String VELOCITY_CHANGE_WHEN_LOWER_LIMIT_IS_REACHED =
      "velocityChangeWhenLowerLimitIsReached";
  public static final String VELOCITY_CHANGE_WHEN_UPPER_LIMIT_IS_REACHED =
      "velocityChangeWhenUpperLimitIsReached";

  // Parameter validation constants
  private static final int MIN_SWARM_SIZE = 10;
  private static final int MAX_SWARM_SIZE = 200;
  private static final double MIN_MUTATION_PROB_FACTOR = 0.0;
  private static final double MAX_MUTATION_PROB_FACTOR = 2.0;
  private static final double MIN_PERTURBATION = 0.0;
  private static final double MAX_PERTURBATION = 1.0;

  /**
   * Creates a new MOPSOParameterSpace with default parameter values. The constructor calls the
   * parent constructor which will initialize the parameters and their relationships.
   */
  public MOPSOParameterSpace() {
    super();
  }

  /**
   * Initializes all parameters in the parameter space. This method is called by the ParameterSpace
   * constructor.
   */
  @Override
  protected void setParameterSpace() {
    // Swarm size
    put(new IntegerParameter(SWARM_SIZE, MIN_SWARM_SIZE, MAX_SWARM_SIZE));

    // Algorithm result
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(UNBOUNDED_ARCHIVE, LEADER_ARCHIVE)));

    // Leader archive type
    put(
        new ExternalArchiveParameter<DoubleSolution>(
            LEADER_ARCHIVE,
            List.of(
                CROWDING_DISTANCE_ARCHIVE, SPATIAL_SPREAD_DEVIATION_ARCHIVE, HYPERVOLUME_ARCHIVE)));

    // Swarm initialization
    put(
        new CreateInitialSolutionsDoubleParameter(
            SWARM_INITIALIZATION,
            List.of(
                DEFAULT_INITIALIZATION,
                LATIN_HYPERCUBE_SAMPLING_INITIALIZATION,
                SCATTER_SEARCH_INITIALIZATION)));

    // Velocity initialization
    put(
        new VelocityInitializationParameter(
            List.of(
                DEFAULT_VELOCITY_INITIALIZATION,
                SPSO2007_VELOCITY_INITIALIZATION,
                SPSO2011_VELOCITY_INITIALIZATION)));

    put(new DoubleParameter(C1_MIN, 1.0, 2.0));
    put(new DoubleParameter(C1_MAX, 2.0, 3.0));
    put(new DoubleParameter(C2_MIN, 1.0, 2.0));
    put(new DoubleParameter(C2_MAX, 2.0, 3.0));

    put(new VelocityUpdateParameter(List.of(DEFAULT_VELOCITY_UPDATE, SPSO2011_VELOCITY_UPDATE, CONSTRAINED_VELOCITY_UPDATE)));

    put(new LocalBestInitializationParameter(List.of(DEFAULT_LOCAL_BEST_INITIALIZATION)));
    
    

  /**
   * Sets up relationships between parameters. This method is called by the ParameterSpace
   * constructor.
   */
  @Override
  protected void setParameterRelationships() {
    // Set up relationships between parameters
    // For example, mutation parameters are sub-parameters of perturbation
    get(PERTURBATION).addSpecificSubParameter("mutation", get(MUTATION));
    get(MUTATION).addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR));

    // Add mutation-specific parameters
    get(MUTATION).addSpecificSubParameter("uniform", get(UNIFORM_MUTATION_PERTURBATION));
    get(MUTATION).addGlobalSubParameter(get(MUTATION_REPAIR_STRATEGY));
  }

  /**
   * Identifies and adds the top-level parameters to the list. This method is called by the
   * ParameterSpace constructor.
   */
  @Override
  protected void setTopLevelParameters() {
    topLevelParameters().add(get(SWARM_SIZE));
    topLevelParameters().add(get(LEADER_ARCHIVE));
    topLevelParameters().add(get(ALGORITHM_RESULT));
    topLevelParameters().add(get(SWARM_INITIALIZATION));
    topLevelParameters().add(get(VELOCITY_INITIALIZATION));
    topLevelParameters().add(get(PERTURBATION));
  }

  /**
   * Creates a MOPSO parameter space from a configuration map.
   *
   * @param configuration the configuration map
   * @return a configured MOPSOParameterSpace instance
   * @throws NullPointerException if configuration is null
   */
  public static MOPSOParameterSpace fromConfiguration(Map<String, Object> configuration) {
    Objects.requireNonNull(configuration, "Configuration map cannot be null");

    MOPSOParameterSpace parameterSpace = new MOPSOParameterSpace();

    // Apply configuration values to parameters
    configuration.forEach(
        (key, value) -> {
          if (parameterSpace.parameters().containsKey(key)) {
            Parameter<?> param = parameterSpace.get(key);
            if (value instanceof String) {
              param.parse(new String[] {(String) value});
            } else if (value instanceof Number) {
              param.parse(new String[] {value.toString()});
            } else if (value instanceof Boolean) {
              param.parse(new String[] {value.toString()});
            }
          }
        });

    // Validate the configured parameters
    parameterSpace.validate();

    return parameterSpace;
  }

  /**
   * Validates the parameter configuration.
   *
   * @throws JMetalException if the configuration is invalid
   */
  public void validate() {
    // Validate swarm size
    int swarmSize = (int) get(SWARM_SIZE).value();
    if (swarmSize < MIN_SWARM_SIZE || swarmSize > MAX_SWARM_SIZE) {
      throw new JMetalException(
          String.format(
              "Swarm size must be between %d and %d", (int) MIN_SWARM_SIZE, (int) MAX_SWARM_SIZE));
    }

    // Validate mutation probability factor
    double mutationProbFactor = (double) get(MUTATION_PROBABILITY_FACTOR).value();
    if (mutationProbFactor < MIN_MUTATION_PROB_FACTOR
        || mutationProbFactor > MAX_MUTATION_PROB_FACTOR) {
      throw new JMetalException(
          String.format(
              "Mutation probability factor must be between %.1f and %.1f",
              MIN_MUTATION_PROB_FACTOR, MAX_MUTATION_PROB_FACTOR));
    }

    // Validate uniform mutation perturbation
    double perturbation = (double) get(UNIFORM_MUTATION_PERTURBATION).value();
    if (perturbation < MIN_PERTURBATION || perturbation > MAX_PERTURBATION) {
      throw new JMetalException(
          String.format(
              "Uniform mutation perturbation must be between %.1f and %.1f",
              MIN_PERTURBATION, MAX_PERTURBATION));
    }
  }
}
