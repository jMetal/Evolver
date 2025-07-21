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
 * Abstract base class for configurable NSGA-II algorithm implementations for evolutionary algorithms.
 *
 * <p>This class provides a flexible and extensible foundation for building NSGA-II variants for
 * different solution types. It manages the configuration and assembly of the main algorithmic
 * components (selection, variation, replacement, evaluation, termination, etc.) using a parameter
 * space abstraction.
 *
 * <p>Subclasses must implement the {@link #setNonConfigurableParameters()} method to set any
 * parameters that are fixed or derived from the problem instance.
 *
 * <p>Typical usage involves:
 * <ul>
 *   <li>Creating a concrete subclass for a specific solution type (e.g., permutation, double).</li>
 *   <li>Configuring the parameter space and problem instance.</li>
 *   <li>Calling {@link #build()} to obtain a ready-to-run {@link EvolutionaryAlgorithm} instance.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * NSGAIIPermutation algorithm = new NSGAIIPermutation(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<PermutationSolution<Integer>> nsgaii = algorithm.build();
 * nsgaii.run();
 * }</pre>
 *
 * @param <S> the solution type handled by the algorithm
 */
public abstract class BaseNSGAII<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  protected final ParameterSpace parameterSpace;

  protected Ranking<S> ranking;
  protected DensityEstimator<S> densityEstimator;
  protected MultiComparator<S> rankingAndCrowdingComparator;

  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;

  /**
   * Constructs an AbstractNSGAII with the given population size and parameter space.
   * This constructor is typically used when the problem and maximum number of evaluations
   * will be set later.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  protected BaseNSGAII(int populationSize, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
  }

  /**
   * Constructs an AbstractNSGAII with the given problem, population size, maximum number of evaluations,
   * and parameter space. Initializes the ranking and density estimator components.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   */
  protected BaseNSGAII(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;

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
  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  /**
   * Builds and returns a configured {@link EvolutionaryAlgorithm} instance using the current
   * parameter space and problem settings.
   * <p>
   * This method assembles all the main components of the NSGA-II algorithm, including archive,
   * initial solutions creation, variation, selection, evaluation, replacement, and termination.
   * Subclasses should ensure that all required parameters are set before calling this method.
   *
   * @return a ready-to-run evolutionary algorithm instance
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

    return new EvolutionaryAlgorithmBuilder<S>().build(
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
   * <p>
   * Subclasses must implement this method to set non-configurable parameters before building the algorithm.
   * If there are not non-configurable parameters, the implementation of this method will be empty.
   * </p>
   */
  protected abstract void setNonConfigurableParameters();

  /**
   * Creates and configures the external archive if required by the parameter space.
   * The archive size is set according to the current population size.
   *
   * @return the configured external archive, or {@code null} if not used
   */
  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");

    externalArchiveParameter.setSize(populationSize);
    Archive<S> archive = externalArchiveParameter.getExternalArchive();
    return archive;
  }

  /**
   * Checks whether the algorithm is configured to use an external archive.
   *
   * @return {@code true} if an external archive is used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace
        .get("algorithmResult")
        .value()
        .equals("externalArchive");
  }

  /**
   * Updates the population size if an external archive is being used.
   * The new population size is retrieved from the parameter space.
   *
   * @param archive the external archive (should not be {@code null} if used)
   */
  private void updatePopulationSize(Archive<S> archive) {
    if (archive != null) {
      populationSize =
          (int) parameterSpace.get("populationSizeWithArchive").value();
    }
  }

  /**
   * Creates the termination condition for the algorithm.
   * By default, termination is based on the maximum number of evaluations.
   *
   * @return the termination condition
   */
  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  /**
   * Creates the evaluation component for the algorithm.
   * If an external archive is used, a sequential evaluation with archive is created;
   * otherwise, a standard sequential evaluation is used.
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
   * Creates the selection operator for the algorithm using the configured selection parameter.
   * The mating pool size is obtained from the variation operator.
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
   * Creates the variation operator for the algorithm using the configured variation parameter.
   * Sets the offspring population size as a non-configurable sub-parameter.
   *
   * @return the variation operator
   */
  protected Variation<S> createVariation() {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter(
        "offspringPopulationSize",
        parameterSpace.get("offspringPopulationSize").value());

    return variationParameter.getVariation();
  }

  /**
   * Creates the initial solutions creation strategy using the configured parameter.
   *
   * @return the solutions creation strategy
   */
  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>)
            parameterSpace.get("createInitialSolutions"))
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
