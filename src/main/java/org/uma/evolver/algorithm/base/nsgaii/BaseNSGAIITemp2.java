package org.uma.evolver.algorithm.base.nsgaii;

import java.util.*;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
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
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Configurable implementation of the NSGA-II algorithm using dependency injection for parameter
 * configuration.
 *
 * <p>This class provides a flexible and extensible foundation for building NSGA-II variants for
 * different solution types using a functional interface approach instead of inheritance. It manages
 * the configuration and assembly of the main algorithmic components (selection, variation,
 * replacement, evaluation, termination, etc.) using a parameter space abstraction and injected
 * parameter configurators.
 *
 * <p>Key features of this implementation include:
 *
 * <ul>
 *   <li>Fast non-dominated sorting for ranking solutions
 *   <li>Crowding distance calculation for diversity preservation
 *   <li>Configurable selection, crossover, and mutation operators
 *   <li>Support for external archives
 *   <li>Flexible termination conditions
 *   <li>Dependency injection for parameter configuration
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Using factory method for double problems
 * NSGAII<DoubleSolution> algorithm = NSGAII.forDoubleProblems(problem, 100, 25000, parameterSpace);
 * EvolutionaryAlgorithm<DoubleSolution> nsgaii = algorithm.build();
 * nsgaii.run();
 *
 * // Using custom lambda configuration
 * NSGAII<DoubleSolution> customAlgorithm = new NSGAII<>(
 *     problem, 100, 25000, parameterSpace,
 *     (prob, params, popSize, maxEval) -> {
 *         // Custom parameter configuration logic
 *     }
 * );
 * }</pre>
 *
 * @param <S> the solution type handled by the algorithm
 * @see <a href="https://ieeexplore.ieee.org/document/996017">A Fast Elitist Non-dominated Sorting
 *     Genetic Algorithm for Multi-objective Optimization: NSGA-II</a>
 */
public class BaseNSGAIITemp2<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  /**
   * Functional interface for configuring non-configurable parameters in NSGA-II algorithm. This
   * interface allows injection of parameter configuration logic without requiring subclasses.
   *
   * @param <S> the solution type handled by the algorithm
   */
  @FunctionalInterface
  public interface NonConfigurableParametersSetter<S extends Solution<?>> {
    /**
     * Configures non-configurable parameters based on the problem and algorithm settings.
     *
     * @param problem the problem being solved
     * @param parameterSpace the parameter space to configure
     * @param populationSize the population size
     * @param maxEvaluations the maximum number of evaluations
     */
    void configure(
        Problem<S> problem, ParameterSpace parameterSpace, int populationSize, int maxEvaluations);
  }

  protected final ParameterSpace parameterSpace;
  protected final NonConfigurableParametersSetter<S> nonConfigurableParametersSetter;

  protected Ranking<S> ranking;
  protected DensityEstimator<S> densityEstimator;
  protected MultiComparator<S> rankingAndCrowdingComparator;

  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;

  /**
   * Constructs an NSGAII with the given population size, parameter space, and parameter
   * configurator. This constructor is typically used when the problem and maximum number of
   * evaluations will be set later.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public BaseNSGAIITemp2(
      int populationSize,
      ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
    this.nonConfigurableParametersSetter = null;
  }

  /**
   * Constructs an NSGAII with the given problem, population size, maximum number of evaluations,
   * parameter space, and parameter configurator. Initializes the ranking and density estimator
   * components.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @param parameterConfigurator the configurator for non-configurable parameters
   */
  public BaseNSGAIITemp2(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      NonConfigurableParametersSetter<S> parameterConfigurator) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
    this.nonConfigurableParametersSetter = parameterConfigurator;

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
   *
   * <p>This method assembles all the main components of the NSGA-II algorithm, including archive,
   * initial solutions creation, variation, selection, evaluation, replacement, and termination.
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
   * Creates a new instance of NSGAII for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of NSGAII
   */
  @Override
  public BaseLevelAlgorithm<S> createInstance(Problem<S> problem, int maximumNumberOfEvaluations) {
    return new BaseNSGAIITemp2<>(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        parameterSpace.createInstance(),
        nonConfigurableParametersSetter);
  }

  /** Configures non-configurable parameters using the injected parameter configurator. */
  protected void setNonConfigurableParameters() {
    nonConfigurableParametersSetter.configure(
        problem, parameterSpace, populationSize, maximumNumberOfEvaluations);
  }

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
    Archive<S> archive = externalArchiveParameter.getExternalArchive();
    return archive;
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

  // ==================== STATIC FACTORY METHODS ====================

  /**
   * Creates an NSGAII instance configured for double-valued (real-coded) problems.
   *
   * <p>This factory method automatically configures:
   *
   * <ul>
   *   <li>The number of problem variables for the mutation operator
   *   <li>The maximum number of iterations for non-uniform mutation
   * </ul>
   *
   * @param problem the double problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @return a configured NSGAII instance for double problems
   */
  public static BaseNSGAIITemp2<DoubleSolution> forDoubleProblems(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    NonConfigurableParametersSetter<DoubleSolution> doubleConfigurator =
        (prob, params, popSize, maxEval) -> {
          var mutationParameter = (MutationParameter<DoubleSolution>) params.get("mutation");
          Check.notNull(mutationParameter);
          mutationParameter.addNonConfigurableSubParameter(
              "numberOfProblemVariables", prob.numberOfVariables());

          Check.that(maxEval > 0, "Maximum number of evaluations must be greater than 0");
          Check.that(popSize > 0, "Population size must be greater than 0");

          if (mutationParameter.value().equals("nonUniform")) {
            mutationParameter.addNonConfigurableSubParameter("maxIterations", maxEval / popSize);
          }
        };

    return new BaseNSGAIITemp2<>(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace, doubleConfigurator);
  }

  /**
   * Creates an NSGAII instance configured for binary problems.
   *
   * <p>This factory method automatically configures:
   *
   * <ul>
   *   <li>The total number of bits in a solution for the mutation operator
   * </ul>
   *
   * @param problem the binary problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @return a configured NSGAII instance for binary problems
   */
  public static BaseNSGAIITemp2<BinarySolution> forBinaryProblems(
      Problem<BinarySolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    NonConfigurableParametersSetter<BinarySolution> binaryConfigurator =
        (prob, params, popSize, maxEval) -> {
          int numberOfBitsInASolution = ((BinaryProblem) prob).totalNumberOfBits();
          var mutationParameter = (MutationParameter<BinarySolution>) params.get("mutation");
          mutationParameter.addNonConfigurableSubParameter(
              "numberOfBitsInASolution", numberOfBitsInASolution);
        };

    return new BaseNSGAIITemp2<>(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace, binaryConfigurator);
  }

  /**
   * Creates an NSGAII instance configured for permutation problems.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @return a configured NSGAII instance with no additional configuration
   */
  public static BaseNSGAIITemp2<PermutationSolution<Integer>> forPermutationProblems(
      Problem<PermutationSolution<Integer>> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    return new BaseNSGAIITemp2<>(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        parameterSpace,
        (prob, params, popSize, maxEval) -> {
          // No additional configuration needed
        });
  }

  /**
   * Creates an NSGAII instance with a custom parameter configurator. This method provides maximum
   * flexibility for custom solution types or parameter configurations.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @param parameterConfigurator custom parameter configuration logic
   * @param <T> the solution type
   * @return a configured NSGAII instance with custom configuration
   */
  public static <T extends Solution<?>> BaseNSGAIITemp2<T> withCustomConfigurator(
      Problem<T> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      NonConfigurableParametersSetter<T> parameterConfigurator) {

    return new BaseNSGAIITemp2<>(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace, parameterConfigurator);
  }

  /**
   * Creates an NSGAII instance with no parameter configuration. This method is useful when all
   * parameters are already configured in the parameter space or when no non-configurable parameters
   * are needed.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space for configuration
   * @param <T> the solution type
   * @return a configured NSGAII instance with no additional configuration
   */
  public static <T extends Solution<?>> BaseNSGAIITemp2<T> withNoConfiguration(
      Problem<T> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {

    return new BaseNSGAIITemp2<>(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        parameterSpace,
        (prob, params, popSize, maxEval) -> {
          // No additional configuration needed
        });
  }

  /**
   * Utility class containing pre-defined parameter configurators for common solution types. This
   * class provides reusable configurators that can be used with the NSGAII constructor for common
   * scenarios.
   */
  final class NSGAIIConfigurators {

    private NSGAIIConfigurators() {
      // Utility class - prevent instantiation
    }

    /**
     * Standard parameter configurator for double-valued problems. Configures number of variables
     * and max iterations for non-uniform mutation.
     */
    public static final NonConfigurableParametersSetter<DoubleSolution> DOUBLE_CONFIGURATOR =
        (problem, parameterSpace, populationSize, maxEvaluations) -> {
          var mutationParameter =
              (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
          Check.notNull(mutationParameter);
          mutationParameter.addNonConfigurableSubParameter(
              "numberOfProblemVariables", problem.numberOfVariables());

          if (mutationParameter.value().equals("nonUniform")) {
            mutationParameter.addNonConfigurableSubParameter(
                "maxIterations", maxEvaluations / populationSize);
          }
        };

    /**
     * Standard parameter configurator for binary problems. Configures the total number of bits in a
     * solution.
     */
    public static final NonConfigurableParametersSetter<BinarySolution> BINARY_CONFIGURATOR =
        (problem, parameterSpace, populationSize, maxEvaluations) -> {
          int numberOfBitsInASolution = ((BinaryProblem) problem).totalNumberOfBits();
          var mutationParameter =
              (MutationParameter<BinarySolution>) parameterSpace.get("mutation");
          mutationParameter.addNonConfigurableSubParameter(
              "numberOfBitsInASolution", numberOfBitsInASolution);
        };

    /** No-op parameter configurator for cases where no additional configuration is needed. */
    public static final NonConfigurableParametersSetter<? extends Solution<?>> NO_CONFIGURATION =
        (problem, parameterSpace, populationSize, maxEvaluations) -> {
          // No configuration needed
        };
  }
}
