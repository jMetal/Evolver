package org.uma.evolver.algorithm.base.rdsmoea.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.DoubleVariationParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract parameter space for RDS-MOEA (Ranking and Density Selection Multi-Objective Evolutionary
 * Algorithm) variants.
 *
 * <p>This class defines the common configurable parameters, their relationships, and top-level
 * parameters for any RDS-MOEA implementation.
 *
 * @param <S> the solution type handled by the algorithm
 */
public abstract class RDEMOEACommonParameterSpace<S extends Solution<?>> extends ParameterSpace {
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

  // Offspring population size
  public static final String OFFSPRING_POPULATION_SIZE = "offspringPopulationSize";

  // Density estimator
  public static final String DENSITY_ESTIMATOR = "densityEstimator";
  public static final String CROWDING_DISTANCE = "crowdingDistance";
  public static final String KNN = "knn";
  public static final String KNN_NEIGHBORHOOD_SIZE = "knnNeighborhoodSize";
  public static final String KNN_NORMALIZE_OBJECTIVES = "knnNormalizeObjectives";
  // public static final String HYPERVOLUME_CONTRIBUTION = "hypervolumeContribution";
  // public static final String COSINE_SIMILARITY = "cosineSimilarity";

  // Ranking
  public static final String RANKING = "ranking";
  public static final String DOMINANCE_RANKING = "dominanceRanking";
  public static final String STRENGTH_RANKING = "strengthRanking";

  // Variation
  public static final String VARIATION = "variation";
  public static final String CROSSOVER_AND_MUTATION_VARIATION = "crossoverAndMutationVariation";

  // Crossover parameters
  public static final String CROSSOVER = "crossover";
  
  // Mutation parameters
  public static final String MUTATION = "mutation";

  // Selection
  public static final String SELECTION = "selection";
  public static final String TOURNAMENT = "tournament";
  public static final String RANDOM_SELECTION = "random";
  public static final String SELECTION_TOURNAMENT_SIZE = "selectionTournamentSize";

  // Replacement
  public static final String REPLACEMENT = "replacement";
  public static final String RANKING_AND_DENSITY_ESTIMATOR = "rankingAndDensityEstimator";
  public static final String REMOVAL_POLICY = "removalPolicy";
  public static final String ONE_SHOT = "oneShot";
  public static final String SEQUENTIAL = "sequential";

  protected void setParameterSpace() {
    put(new CategoricalParameter(ALGORITHM_RESULT, List.of(POPULATION, EXTERNAL_ARCHIVE)));
    put(new IntegerParameter(POPULATION_SIZE_WITH_ARCHIVE, 10, 200));
    put(
        new ExternalArchiveParameter<>(
            ARCHIVE_TYPE, List.of(CROWDING_DISTANCE_ARCHIVE, UNBOUNDED_ARCHIVE)));

    put(
        new CategoricalIntegerParameter(
            OFFSPRING_POPULATION_SIZE, List.of(1, 2, 5, 10, 20, 50, 100, 200, 400)));
    put(new DoubleVariationParameter(List.of(CROSSOVER_AND_MUTATION_VARIATION)));

    put(new SelectionParameter<>(List.of(TOURNAMENT, RANDOM_SELECTION)));
    put(new IntegerParameter(SELECTION_TOURNAMENT_SIZE, 2, 10));

    put(new RankingParameter<>(RANKING, List.of(DOMINANCE_RANKING, STRENGTH_RANKING)));
    put(new DensityEstimatorParameter<>(DENSITY_ESTIMATOR, List.of(CROWDING_DISTANCE, KNN)));
    put(new IntegerParameter(KNN_NEIGHBORHOOD_SIZE, 1, 5));
    put(new CategoricalParameter(KNN_NORMALIZE_OBJECTIVES, List.of("true", "false")));
    put(new ReplacementParameter<>(List.of(RANKING_AND_DENSITY_ESTIMATOR)));
    put(new CategoricalParameter(REMOVAL_POLICY, List.of(ONE_SHOT, SEQUENTIAL)));
  }

  protected void setParameterRelationships() {
    get(ALGORITHM_RESULT)
        .addConditionalParameter(EXTERNAL_ARCHIVE, get(POPULATION_SIZE_WITH_ARCHIVE))
        .addConditionalParameter(EXTERNAL_ARCHIVE, get(ARCHIVE_TYPE));

    get(VARIATION)
        .addConditionalParameter(CROSSOVER_AND_MUTATION_VARIATION, get(CROSSOVER))
        .addConditionalParameter(CROSSOVER_AND_MUTATION_VARIATION, get(MUTATION));

    get(SELECTION).addConditionalParameter(TOURNAMENT, get(SELECTION_TOURNAMENT_SIZE));

    get(REPLACEMENT).addConditionalParameter(RANKING_AND_DENSITY_ESTIMATOR, get(REMOVAL_POLICY));

    get(DENSITY_ESTIMATOR)
        .addConditionalParameter(KNN, get(KNN_NEIGHBORHOOD_SIZE))
        .addConditionalParameter(KNN, get(KNN_NORMALIZE_OBJECTIVES));
  }

  protected void setTopLevelParameters() {
    // Define which parameters should be exposed at the top level of the configuration
    topLevelParameters.add(parameterSpace.get(ALGORITHM_RESULT));
    topLevelParameters.add(parameterSpace.get(CREATE_INITIAL_SOLUTIONS));
    topLevelParameters.add(parameterSpace.get(OFFSPRING_POPULATION_SIZE));
    topLevelParameters.add(parameterSpace.get(DENSITY_ESTIMATOR));
    topLevelParameters.add(parameterSpace.get(RANKING));
    topLevelParameters.add(parameterSpace.get(VARIATION));
    topLevelParameters.add(parameterSpace.get(SELECTION));
    topLevelParameters.add(parameterSpace.get(REPLACEMENT));
  }
}
