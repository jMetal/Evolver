package org.uma.evolver.algorithm.base.nsgaii;

import java.util.*;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.util.RankingAndDensityEstimatorPreference;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Abstract base class for configurable NSGA-II (Non-dominated Sorting Genetic Algorithm II)
 * implementations.
 *
 * <p>This class provides a flexible and extensible foundation for building NSGA-II variants for
 * different solution types. It manages the configuration and assembly of the main algorithmic
 * components (selection, variation, replacement, evaluation, termination, etc.) using a parameter
 * space abstraction. NSGA-II is a fast and elitist multi-objective genetic algorithm that uses a
 * non-dominated sorting approach with crowding distance for maintaining diversity.
 *
 * <p>Key features of this implementation include:
 *
 * <ul>
 *   <li>Fast non-dominated sorting for ranking solutions
 *   <li>Crowding distance calculation for diversity preservation
 *   <li>Configurable selection, crossover, and mutation operators
 *   <li>Support for external archives
 *   <li>Flexible termination conditions
 * </ul>
 *
 * <p>Subclasses must implement the {@link #setNonConfigurableParameters()} method to set any
 * parameters that are fixed or derived from the problem instance.
 *
 * <p>Typical usage involves:
 *
 * <ul>
 *   <li>Creating a concrete subclass for a specific solution type (e.g., permutation, double)
 *   <li>Configuring the parameter space and problem instance
 *   <li>Calling {@link #build()} to obtain a ready-to-run {@link EvolutionaryAlgorithm} instance
 * </ul>
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * // Create a problem instance
 * Problem<MySolution> problem = new MyProblem();
 *
 * // Configure the algorithm
 * int populationSize = 100;
 * int maxEvaluations = 25000;
 * ParameterSpace parameterSpace = new ParameterSpace();
 * // Configure parameter space with desired components and parameters
 *
 * // Create and run the algorithm
 * BaseNSGAII<MySolution> nsgaii = new MyNSGAII(problem, populationSize, maxEvaluations, parameterSpace);
 * nsgaii.run();
 *
 * // Get results
 * List<MySolution> population = nsgaii.result();
 * }</pre>
 *
 * @param <S> the type of solutions handled by this algorithm, must extend {@link Solution}
 * @see <a href="https://ieeexplore.ieee.org/document/996017">A Fast Elitist Non-dominated Sorting
 *     Genetic Algorithm for Multi-objective Optimization: NSGA-II</a>
 * @see BaseLevelAlgorithm
 * @see EvolutionaryAlgorithm
 * @since version
 */
public abstract class BaseNSGAII<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  /** The parameter space containing all configurable components and parameters. */
  protected final ParameterSpace parameterSpace;

  /**
   * Constructs a new BaseNSGAII instance with the specified population size and parameter space.
   *
   * <p>This creates a partially configured instance. The {@link #createInstance(Problem, int)}
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population to be used in the algorithm. Must be positive.
   * @param parameterSpace the parameter space containing configuration parameters for the
   *     algorithm. Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive or parameterSpace is null
   */
  protected BaseNSGAII(int populationSize, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
    this.offspringPopulationSize = populationSize; // Default to same as population size
  }

  /** The ranking strategy used for non-dominated sorting of solutions. */
  protected Ranking<S> ranking;

  /** The density estimator used for maintaining diversity in the population. */
  protected DensityEstimator<S> densityEstimator;

  /** Comparator that combines ranking and crowding distance for solution comparison. */
  protected MultiComparator<S> rankingAndCrowdingComparator;

  /** The optimization problem to be solved. */
  protected Problem<S> problem;

  /** The size of the population. */
  protected int populationSize;

  /** The size of the offspring population generated in each generation. */
  protected int offspringPopulationSize;

  /** The maximum number of evaluations allowed for the algorithm. */
  protected int maximumNumberOfEvaluations;

  /** Optional external archive for storing non-dominated solutions. */
  protected Archive<S> externalArchive;

  /**
   * Constructs a fully configured BaseNSGAII instance ready for execution.
   *
   * @param problem the optimization problem to be solved. Must not be null.
   * @param populationSize the size of the population. Must be positive.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the algorithm.
   *     Must be positive and greater than populationSize.
   * @param parameterSpace the parameter space containing configuration parameters for the
   *     algorithm. Must not be null.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where
   *     required)
   */
  protected BaseNSGAII(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this(populationSize, parameterSpace);
    this.problem = problem;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;

    ranking = new FastNonDominatedSortRanking<>();
    densityEstimator = new CrowdingDistanceDensityEstimator<>();
    rankingAndCrowdingComparator =
        new MultiComparator<>(
            Arrays.asList(
                Comparator.comparing(ranking::getRank),
                Comparator.comparing(densityEstimator::value).reversed()));
  }

  /**
   * Returns the parameter space associated with this algorithm.
   *
   * @return the parameter space
   */
  /**
   * Returns the parameter space used by this algorithm.
   *
   * @return the parameter space containing all configurable components and parameters
   */
  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  /**
   * Builds and returns a configured {@link EvolutionaryAlgorithm} instance using the current
   * parameter space and problem settings.
   *
   * <p>This method assembles all the main components of the NSGA-II algorithm, including archive,
   * initial solutions creation, variation, selection, evaluation, replacement, and termination.
   * Subclasses should ensure that all required parameters are set before calling this method.
   *
   * @return a ready-to-run evolutionary algorithm instance
   */
  /**
   * Builds and configures the NSGA-II algorithm based on the current parameter space.
   *
   * <p>This method initializes all necessary components for the NSGA-II algorithm, including:
   *
   * <ul>
   *   <li>Ranking and density estimation strategies
   *   <li>Population initialization
   *   <li>Selection, crossover, and mutation operators
   *   <li>Termination condition
   *   <li>Optional external archive
   * </ul>
   *
   * @return a fully configured EvolutionaryAlgorithm instance implementing NSGA-II
   * @throws IllegalStateException if required parameters are not properly configured
   */
  @Override
  public EvolutionaryAlgorithm<S> build() {
    setNonConfigurableParameters();
    Archive<S> archive = null;
    if (usingExternalArchive()) {
      archive = createExternalArchive();
      updatePopulationSize(archive);
      Check.notNull(archive);
    }
    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation);
    Evaluation<S> evaluation = createEvaluation(archive);
    Replacement<S> replacement = createReplacement();
    Termination termination = createTermination();

    return new EvolutionaryAlgorithmBuilder<S>()
        .build(
            "NSGAII",
            initialSolutionsCreation,
            evaluation,
            termination,
            selection,
            variation,
            replacement,
            archive);
  }

  /**
   * Configures any parameters that are fixed or derived from the problem instance.
   *
   * <p>Subclasses must implement this method to set non-configurable parameters before building the
   * algorithm. If there are not non-configurable parameters, the implementation of this method will
   * be empty.
   */
  protected abstract void setNonConfigurableParameters();

  /**
   * Creates and configures the external archive if required by the parameter space. The archive
   * size is set according to the current population size.
   *
   * @return the configured external archive, or {@code null} if not used
   */
  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");

    externalArchiveParameter.setSize(populationSize);
    return externalArchiveParameter.getExternalArchive();
  }

  /**
   * Checks whether the algorithm is configured to use an external archive.
   *
   * @return {@code true} if an external archive is used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace.get("algorithmResult").value().equals("externalArchive");
  }

  /**
   * Updates the population size if an external archive is being used. The new population size is
   * retrieved from the parameter space.
   *
   * @param archive the external archive (should not be {@code null} if used)
   */
  private void updatePopulationSize(Archive<S> archive) {
    if (archive != null) {
      populationSize = (int) parameterSpace.get("populationSizeWithArchive").value();
    }
  }

  /**
   * Creates the termination condition for the algorithm. By default, termination is based on the
   * maximum number of evaluations.
   *
   * @return the termination condition
   */
  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  /**
   * Creates the evaluation component for the algorithm. If an external archive is used, a
   * sequential evaluation with archive is created; otherwise, a standard sequential evaluation is
   * used.
   *
   * @param archive the external archive, or {@code null} if not used
   * @return the evaluation component
   */
  protected Evaluation<S> createEvaluation(Archive<S> archive) {
    Evaluation<S> evaluation;
    if (usingExternalArchive()) {
      Check.notNull(archive);
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }
    return evaluation;
  }

  /**
   * Creates the selection operator for the algorithm using the configured selection parameter. The
   * mating pool size is obtained from the variation operator.
   *
   * @param variation the variation operator
   * @return the selection operator
   */
  protected Selection<S> createSelection(Variation<S> variation) {
    var selectionParameter = (SelectionParameter<S>) parameterSpace.get("selection");
    return selectionParameter.getSelection(
        variation.getMatingPoolSize(), rankingAndCrowdingComparator);
  }

  /**
   * Creates the variation operator for the algorithm using the configured variation parameter. Sets
   * the offspring population size as a non-configurable sub-parameter.
   *
   * @return the variation operator
   */
  protected Variation<S> createVariation() {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter(
        "offspringPopulationSize", parameterSpace.get("offspringPopulationSize").value());

    return variationParameter.getVariation();
  }

  /**
   * Creates the initial solutions creation strategy using the configured parameter.
   *
   * @return the solutions creation strategy
   */
  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  /**
   * Creates the replacement operator for the algorithm using ranking and density estimators.
   *
   * @return the replacement operator
   */
  protected Replacement<S> createReplacement() {
    RankingAndDensityEstimatorPreference<S> preferenceForReplacement =
        new RankingAndDensityEstimatorPreference<>(ranking, densityEstimator);
    return new RankingAndDensityEstimatorReplacement<>(
        preferenceForReplacement, Replacement.RemovalPolicy.ONE_SHOT);
  }
}
