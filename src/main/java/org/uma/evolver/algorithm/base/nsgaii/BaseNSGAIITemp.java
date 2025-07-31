package org.uma.evolver.algorithm.base.nsgaii;

import java.util.*;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
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
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Refactored implementation of NSGA-II that supports multiple solution types through a strategy
 * pattern. This is a temporary implementation for testing purposes.
 *
 * @param <S> the solution type handled by the algorithm
 */
public class BaseNSGAIITemp<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  protected final ParameterSpace parameterSpace;
  protected final Problem<S> problem;
  protected int populationSize;
  protected final int maximumNumberOfEvaluations;
  private final NonConfigurableParametersSetter<S> nonConfigurableParametersSetter;
  
  protected Ranking<S> ranking;
  protected DensityEstimator<S> densityEstimator;
  protected MultiComparator<S> rankingAndCrowdingComparator;

  /** Strategy interface for setting non-configurable parameters. */
  @FunctionalInterface
  public interface NonConfigurableParametersSetter<S extends Solution<?>> {
    void set(
        Problem<S> problem, int maxEvaluations, int populationSize, ParameterSpace parameterSpace);
  }

  private BaseNSGAIITemp(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      NonConfigurableParametersSetter<S> nonConfigurableParametersSetter) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
    this.nonConfigurableParametersSetter = nonConfigurableParametersSetter;
  }

  // Factory methods for different problem types

  /** Creates an NSGA-II instance for binary problems. */
  @SuppressWarnings("unchecked")
  public static BaseNSGAIITemp<BinarySolution> forBinaryProblem(
      BinaryProblem problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    NonConfigurableParametersSetter<BinarySolution> setter =
        (p, maxEvals, popSize, params) -> {
          int numberOfBits = ((BinaryProblem) p).totalNumberOfBits();
          var mutationParam = (MutationParameter<BinarySolution>) params.get("mutation");
          mutationParam.addNonConfigurableSubParameter("numberOfBitsInASolution", numberOfBits);
        };

    return new BaseNSGAIITemp<>(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace, setter);
  }

  /** Creates an NSGA-II instance for double problems. */
  @SuppressWarnings("unchecked")
  public static BaseNSGAIITemp<DoubleSolution> forDoubleProblem(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    NonConfigurableParametersSetter<DoubleSolution> setter =
        (p, maxEvals, popSize, params) -> {
          var mutationParam = (MutationParameter<DoubleSolution>) params.get("mutation");
          mutationParam.addNonConfigurableSubParameter(
              "numberOfProblemVariables", p.numberOfVariables());

          if (mutationParam.value() != null && mutationParam.value().equals("nonUniform")) {
            mutationParam.addNonConfigurableSubParameter("maxIterations", maxEvals / popSize);
          }
        };

    return new BaseNSGAIITemp<>(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace, setter);
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  @Override
  public EvolutionaryAlgorithm<S> build() {
    setNonConfigurableParameters();
    
    // Initialize ranking and density estimator components
    ranking = new FastNonDominatedSortRanking<>();
    densityEstimator = new CrowdingDistanceDensityEstimator<>();
    rankingAndCrowdingComparator = new MultiComparator<>(
        Arrays.asList(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value).reversed()));

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
   * Creates and configures the external archive if required by the parameter space.
   * The archive size is set according to the current population size.
   *
   * @return the configured external archive, or {@code null} if not used
   */
  @SuppressWarnings("unchecked")
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
    return parameterSpace.get("archiveType") != null;
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
    if (archive != null) {
      return new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      return new SequentialEvaluation<>(problem);
    }
  }

  /**
   * Creates the selection operator for the algorithm using the configured selection parameter.
   * The mating pool size is obtained from the variation operator.
   *
   * @param variation the variation operator
   * @return the selection operator
   */
  @SuppressWarnings("unchecked")
  private Selection<S> createSelection(Variation<S> variation) {
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
  @SuppressWarnings("unchecked")
  private Variation<S> createVariation() {
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
  @SuppressWarnings("unchecked")
  private SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>)
            parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  /**
   * Creates the replacement operator for the algorithm using ranking and density estimators.
   *
   * @return the replacement operator
   */
  private Replacement<S> createReplacement() {
    return new RankingAndDensityEstimatorReplacement<>(
        ranking, densityEstimator, Replacement.RemovalPolicy.ONE_SHOT);
  }

  @Override
  public BaseLevelAlgorithm<S> createInstance(Problem<S> problem, int maximumNumberOfEvaluations) {
    return new BaseNSGAIITemp<>(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        parameterSpace.createInstance(),
        nonConfigurableParametersSetter);
  }

  /** Sets non-configurable parameters using the provided strategy. */
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    if (nonConfigurableParametersSetter == null) {
      throw new NullPointerException("Non-configurable parameters setter is null");
    }
    nonConfigurableParametersSetter.set(
        problem, maximumNumberOfEvaluations, populationSize, parameterSpace);
    
    // Set common non-configurable parameters
    if (parameterSpace.get("variation") != null) {
      VariationParameter<S> variationParameter = 
          (VariationParameter<S>) parameterSpace.get("variation");
      variationParameter.addNonConfigurableSubParameter("populationSize", populationSize);
    }
  }
}
