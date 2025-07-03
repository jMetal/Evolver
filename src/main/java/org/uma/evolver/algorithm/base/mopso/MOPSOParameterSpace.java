package org.uma.evolver.algorithm.base.mopso;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.List;

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
  public static final String ALGORITHM_RESULT = "algorithmResult";
  public static final String LEADER_ARCHIVE = "leaderArchive";
  public static final String EXTERNAL_ARCHIVE = "externalArchive";
  public static final String EXTERNAL_ARCHIVE_TYPE = "externalArchiveType";
  public static final String UNBOUNDED_ARCHIVE = "unboundedArchive";
  public static final String HYPERVOLUME_ARCHIVE = "hypervolumeArchive";
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
  public static final String CONSTANT_VALUE = "constantValue";
  public static final String LINEAR_DECREASING = "linearDecreasing";
  public static final String LINEAR_INCREASING = "linearIncreasing";
  public static final String RANDOM_SELECTED_VALUE = "randomSelectedValue";
  public static final String INERTIA_WEIGHT = "inertiaWeight";
  public static final String INERTIA_WEIGHT_MIN = "inertiaWeightMin";
  public static final String INERTIA_WEIGHT_MAX = "inertiaWeightMax";

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
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(EXTERNAL_ARCHIVE, LEADER_ARCHIVE)));

    // Leader archive type
    put(
        new ExternalArchiveParameter<DoubleSolution>(
            LEADER_ARCHIVE,
            List.of(
                CROWDING_DISTANCE_ARCHIVE, SPATIAL_SPREAD_DEVIATION_ARCHIVE, HYPERVOLUME_ARCHIVE)));

    // External archive type
    put(new ExternalArchiveParameter<DoubleSolution>(EXTERNAL_ARCHIVE_TYPE, List.of(UNBOUNDED_ARCHIVE)));

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

    put(
        new VelocityUpdateParameter(
            List.of(
                DEFAULT_VELOCITY_UPDATE, SPSO2011_VELOCITY_UPDATE, CONSTRAINED_VELOCITY_UPDATE)));

    put(new LocalBestInitializationParameter(List.of(DEFAULT_LOCAL_BEST_INITIALIZATION)));
    put(new LocalBestUpdateParameter(List.of(DEFAULT_LOCAL_BEST_UPDATE)));

    put(new GlobalBestInitializationParameter(List.of(DEFAULT_GLOBAL_BEST_INITIALIZATION)));
    put(new GlobalBestUpdateParameter(List.of(DEFAULT_GLOBAL_BEST_UPDATE)));

    put(new GlobalBestSelectionParameter(List.of(TOURNAMENT_SELECTION, RANDOM_SELECTION)));
    put(new IntegerParameter(SELECTION_TOURNAMENT_SIZE, 2, 10));

    put(new PositionUpdateParameter(List.of(DEFAULT_POSITION_UPDATE)));
    put(new DoubleParameter(VELOCITY_CHANGE_WHEN_LOWER_LIMIT_IS_REACHED, -1.0, 1.0));
    put(new DoubleParameter(VELOCITY_CHANGE_WHEN_UPPER_LIMIT_IS_REACHED, -1.0, 1.0));

    put(new PerturbationParameter(List.of(FREQUENCY_SELECTION_MUTATION_BASED_PERTURBATION)));
    put(new IntegerParameter(FREQUENCY_OF_APPLICATION_OF_MUTATION_OPERATOR, 1, 10));
    put(new DoubleMutationParameter(List.of(POLYNOMIAL, UNIFORM, LINKED_POLYNOMIAL, NON_UNIFORM)));
    put(new DoubleParameter(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(NON_UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 2.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            MUTATION_REPAIR_STRATEGY, List.of(REPAIR_RANDOM, REPAIR_ROUND, REPAIR_BOUNDS)));

    put(
        new ExternalArchiveParameter<DoubleSolution>(
            LEADER_ARCHIVE,
            List.of(
                CROWDING_DISTANCE_ARCHIVE, SPATIAL_SPREAD_DEVIATION_ARCHIVE, HYPERVOLUME_ARCHIVE)));

    put(
        new InertiaWeightComputingParameter(
            List.of(CONSTANT_VALUE, LINEAR_DECREASING, LINEAR_INCREASING, RANDOM_SELECTED_VALUE)));
    put(new DoubleParameter(INERTIA_WEIGHT_MIN, 0.1, 0.5));
    put(new DoubleParameter(INERTIA_WEIGHT_MAX, 0.5, 1.0));
    put(new DoubleParameter(INERTIA_WEIGHT, 0.1, 1.0));
  }

  /**
   * Sets up relationships between parameters. This method is called by the ParameterSpace
   * constructor.
   */
  @Override
  protected void setParameterRelationships() {
    // Set up relationships between parameters
    // For example, mutation parameters are sub-parameters of perturbation
    get(ALGORITHM_RESULT).addSpecificSubParameter(EXTERNAL_ARCHIVE, get(EXTERNAL_ARCHIVE_TYPE)) ;

    get(VELOCITY_UPDATE).addGlobalSubParameter(get(C1_MIN));
    get(VELOCITY_UPDATE).addGlobalSubParameter(get(C1_MAX));
    get(VELOCITY_UPDATE).addGlobalSubParameter(get(C2_MIN));
    get(VELOCITY_UPDATE).addGlobalSubParameter(get(C2_MAX));

    get(GLOBAL_BEST_SELECTION)
        .addSpecificSubParameter(TOURNAMENT_SELECTION, get(SELECTION_TOURNAMENT_SIZE));

    get(POSITION_UPDATE)
        .addSpecificSubParameter(
            DEFAULT_POSITION_UPDATE, get(VELOCITY_CHANGE_WHEN_LOWER_LIMIT_IS_REACHED));
    get(POSITION_UPDATE)
        .addSpecificSubParameter(
            DEFAULT_POSITION_UPDATE, get(VELOCITY_CHANGE_WHEN_UPPER_LIMIT_IS_REACHED));

    // Add mutation-specific parameters
    get(MUTATION).addGlobalSubParameter(get(MUTATION_REPAIR_STRATEGY));
    get(MUTATION).addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR));
    get(MUTATION).addSpecificSubParameter(UNIFORM, get(UNIFORM_MUTATION_PERTURBATION));
    get(MUTATION).addSpecificSubParameter(NON_UNIFORM, get(NON_UNIFORM_MUTATION_PERTURBATION));
    get(MUTATION).addSpecificSubParameter(POLYNOMIAL, get(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX));
    get(MUTATION)
        .addSpecificSubParameter(
            LINKED_POLYNOMIAL, get(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX));

    get(PERTURBATION)
        .addSpecificSubParameter(FREQUENCY_SELECTION_MUTATION_BASED_PERTURBATION, get(MUTATION));
    get(PERTURBATION)
        .addSpecificSubParameter(
            FREQUENCY_SELECTION_MUTATION_BASED_PERTURBATION,
            get(FREQUENCY_OF_APPLICATION_OF_MUTATION_OPERATOR));

    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(CONSTANT_VALUE, get(INERTIA_WEIGHT));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(LINEAR_DECREASING, get(INERTIA_WEIGHT_MIN));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(LINEAR_DECREASING, get(INERTIA_WEIGHT_MAX));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(LINEAR_INCREASING, get(INERTIA_WEIGHT_MIN));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(LINEAR_INCREASING, get(INERTIA_WEIGHT_MAX));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(RANDOM_SELECTED_VALUE, get(INERTIA_WEIGHT_MIN));
    get(INERTIA_WEIGHT_COMPUTING_STRATEGY)
        .addSpecificSubParameter(RANDOM_SELECTED_VALUE, get(INERTIA_WEIGHT_MAX));
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
    topLevelParameters().add(get(INERTIA_WEIGHT_COMPUTING_STRATEGY));
    topLevelParameters().add(get(VELOCITY_UPDATE));
    topLevelParameters().add(get(LOCAL_BEST_INITIALIZATION));
    topLevelParameters().add(get(LOCAL_BEST_UPDATE));
    topLevelParameters().add(get(GLOBAL_BEST_INITIALIZATION));
    topLevelParameters().add(get(GLOBAL_BEST_UPDATE));
    topLevelParameters().add(get(GLOBAL_BEST_SELECTION));
    topLevelParameters().add(get(POSITION_UPDATE));
  }
}
