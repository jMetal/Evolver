package org.uma.evolver.meta.problem;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.evolver.util.EvaluationsQualityIndicator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A meta-optimization problem that optimizes the parameters of an optimization
 * algorithm
 * by evaluating its performance across multiple problem instances using quality
 * indicators.
 * 
 * <p>
 * This class implements a meta-optimization approach where the parameters of a
 * base
 * algorithm are automatically tuned by evaluating its performance on multiple
 * problem
 * instances using various quality indicators. The optimization objective is to
 * find parameter
 * settings that work well across all problem instances in the training set.
 * </p>
 * 
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Supports optimization of any configurable algorithm that implements
 * {@code BaseLevelAlgorithm}</li>
 * <li>Allows evaluation on multiple problem instances with different
 * characteristics</li>
 * <li>Uses quality indicators to assess algorithm performance</li>
 * <li>Supports multiple independent runs to account for stochasticity</li>
 * <li>Provides flexible evaluation budget control through
 * {@link EvaluationBudgetStrategy}</li>
 * </ul>
 *
 * <p>
 * Typical usage involves:
 * </p>
 * <ol>
 * <li>Define the base algorithm with its parameter space</li>
 * <li>Select a set of training problems</li>
 * <li>Configure quality indicators for evaluation</li>
 * <li>Set up the evaluation budget strategy</li>
 * <li>Use an optimization algorithm to solve the meta-optimization problem</li>
 * </ol>
 *
 * @param <S> The type of solutions used by the base algorithm being optimized
 * 
 * @see EvaluationBudgetStrategy
 * @see BaseLevelAlgorithm
 * @see org.uma.jmetal.qualityindicator.QualityIndicator
 */
public class MetaOptimizationProblem<S extends Solution<?>> extends AbstractDoubleProblem {
  /** The base algorithm whose parameters are being optimized. */
  private final BaseLevelAlgorithm<S> baseAlgorithm;

  /** List of problems used to evaluate the algorithm's performance. */
  private final List<Problem<S>> problems;

  /** Quality indicators used to evaluate solutions. */
  private final List<QualityIndicator> indicators;

  /** Strategy for determining number of evaluations per problem. */
  private final EvaluationBudgetStrategy evaluationBudgetStrategy;

  /** List of parameters being optimized. */
  private final List<Parameter<?>> parameters;

  /** Normalized reference fronts for each problem. */
  private final List<double[][]> normalizedReferenceFronts;

  /** Original reference fronts for each problem. */
  private final List<double[][]> referenceFronts;

  /** Number of independent runs to perform for each evaluation. */
  private final int numberOfIndependentRuns;

  /**
   * Constructs a new meta-optimization problem instance.
   *
   * @param baseAlgorithm            the base algorithm whose parameters will be
   *                                 optimized (must not be null)
   * @param problems                 the list of problems to evaluate the
   *                                 algorithm on (must not be null or empty)
   * @param referenceFrontFileNames  list of file paths containing reference
   *                                 fronts for each problem
   *                                 (must match the size of problems list)
   * @param indicators               list of quality indicators to evaluate
   *                                 solutions (must not be null or empty)
   * @param evaluationBudgetStrategy strategy for determining the evaluation
   *                                 budget for each problem
   *                                 (must not be null and must be compatible with
   *                                 the number of problems)
   * @param numberOfIndependentRuns  number of independent runs to perform for
   *                                 each evaluation
   *                                 (must be positive)
   * @throws NullPointerException     if any parameter is null
   * @throws IllegalArgumentException if the sizes of problems and
   *                                  referenceFrontFileNames don't match,
   *                                  if the evaluation budget strategy is not
   *                                  compatible with the number of problems,
   *                                  or if numberOfIndependentRuns is not
   *                                  positive
   * @see EvaluationBudgetStrategy#validate(int)
   */
  public MetaOptimizationProblem(
      BaseLevelAlgorithm<S> baseAlgorithm,
      List<Problem<S>> problems,
      List<String> referenceFrontFileNames,
      List<QualityIndicator> indicators,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      int numberOfIndependentRuns) {
    Check.notNull(baseAlgorithm);
    Check.notNull(problems);
    Check.notNull(referenceFrontFileNames);
    Check.notNull(indicators);
    Check.notNull(evaluationBudgetStrategy);

    this.baseAlgorithm = baseAlgorithm;
    this.problems = new ArrayList<>(problems);
    this.indicators = new ArrayList<>(indicators);
    this.evaluationBudgetStrategy = evaluationBudgetStrategy;
    this.numberOfIndependentRuns = numberOfIndependentRuns;

    this.parameters = ParameterManagement.parameterFlattening(baseAlgorithm.parameterSpace().topLevelParameters());

    Check.that(
        problems.size() == referenceFrontFileNames.size(),
        "There must be the same number of problems as reference fronts: "
            + problems.size()
            + " vs "
            + referenceFrontFileNames.size());

    // Validate that the evaluation strategy is compatible with the number of
    // problems
    evaluationBudgetStrategy.validate(problems.size());

    List<Double> lowerLimit = Collections.nCopies(parameters.size(), 0.0);
    List<Double> upperLimit = Collections.nCopies(parameters.size(), 1.0);

    variableBounds(lowerLimit, upperLimit);

    this.referenceFronts = loadReferenceFronts(referenceFrontFileNames);
    this.normalizedReferenceFronts = this.referenceFronts.stream()
        .map(NormalizeUtils::normalize)
        .toList();
  }

  /**
   * Loads reference fronts from the specified files.
   *
   * @param referenceFrontFileNames list of file paths containing reference fronts
   * @return the loaded reference fronts
   * @throws JMetalException if a reference front file cannot be read
   */
  private static List<double[][]> loadReferenceFronts(List<String> referenceFrontFileNames) {
    List<double[][]> fronts = new ArrayList<>();
    for (String fileName : referenceFrontFileNames) {
      try {
        fronts.add(VectorUtils.readVectors(fileName, ","));
      } catch (IOException e) {
        throw new JMetalException("The file does not exist: " + fileName, e);
      }
    }
    return fronts;
  }

  /**
   * Returns the number of variables (parameters) in the problem.
   *
   * @return the number of parameters being optimized
   */
  @Override
  public int numberOfVariables() {
    return parameters.size();
  }

  /**
   * Returns the number of objectives in the problem.
   *
   * @return the number of quality indicators being used
   */
  @Override
  public int numberOfObjectives() {
    return indicators.size();
  }

  /**
   * Returns the number of constraints in the problem.
   * This implementation returns 0 as the problem is unconstrained.
   *
   * @return 0 (no constraints)
   */
  @Override
  public int numberOfConstraints() {
    return 0;
  }

  /**
   * Returns the name of the problem.
   *
   * @return the name of the problem as a string
   */
  @Override
  public String name() {
    return "Meta-optimization problem";
  }

  /**
   * Returns the evaluation strategy being used.
   *
   * @return the evaluation strategy
   */
  public EvaluationBudgetStrategy evaluationBudgetStrategy() {
    return evaluationBudgetStrategy;
  }

  /**
   * Returns the list of parameters being optimized.
   *
   * @return an unmodifiable list of parameters
   */
  public List<Parameter<?>> parameters() {
    return Collections.unmodifiableList(parameters);
  }

  /**
   * Returns the list of problems used for evaluation.
   *
   * @return an unmodifiable list of problems
   */
  public List<Problem<S>> problems() {
    return problems;
  }

  /**
   * Returns the top-level parameters from the parameter space.
   * This is needed for hierarchical traversal when determining active conditional
   * parameters.
   *
   * @return the list of top-level parameters
   */
  public List<Parameter<?>> topLevelParameters() {
    return baseAlgorithm.parameterSpace().topLevelParameters();
  }

  /**
   * Evaluates a solution by running the base algorithm with the specified
   * parameter settings
   * and computing the quality indicators.
   *
   * @param solution the solution containing the parameter values to evaluate
   * @return the evaluated solution with objective values set
   * @throws NullPointerException     if solution is null
   * @throws IllegalArgumentException if the solution's variables don't match the
   *                                  expected number of parameters
   */
  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    String[] parameterArray = convertSolutionToParameters(solution);
    double[][] indicatorValuesPerProblem = computeIndicatorValuesForAllProblems(parameterArray);
    updateSolutionWithMeanIndicatorValues(solution, indicatorValuesPerProblem);

    return solution;
  }

  /**
   * Converts a solution's variables into an array of parameter strings.
   *
   * @param solution the solution containing parameter values
   * @return array of parameter strings in the format expected by the base
   *         algorithm
   * @throws NullPointerException if solution is null
   */
  private String[] convertSolutionToParameters(DoubleSolution solution) {
    StringBuilder parameterString = ParameterManagement.decodeParametersToString(parameters, solution.variables());
    return parameterString.toString().split("\\s+");
  }

  /**
   * Computes the indicator values for all problems using the given parameter
   * settings.
   *
   * @param parameterArray the parameter settings to evaluate
   * @return a 2D array where each row corresponds to a problem and each column to
   *         an indicator
   */
  private double[][] computeIndicatorValuesForAllProblems(String[] parameterArray) {
    double[][] indicatorValuesPerProblem = new double[problems.size()][indicators.size()];

    // Run each problem n independent times
    IntStream.range(0, problems.size())
        .forEach(
            problemId -> {
              double[] medianIndicatorValues = computeIndependentRuns(parameterArray, problemId);
              indicatorValuesPerProblem[problemId] = medianIndicatorValues;
            });

    return indicatorValuesPerProblem;
  }

  /**
   * Updates the solution's objective values with the mean indicator values across
   * all problems.
   *
   * @param solution                  the solution to update
   * @param indicatorValuesPerProblem a 2D array of indicator values
   *                                  [problemIndex][indicatorIndex]
   * @throws IllegalStateException if there are no indicators or problems
   */
  private void updateSolutionWithMeanIndicatorValues(
      DoubleSolution solution, double[][] indicatorValuesPerProblem) {
    // Validate inputs
    if (indicators.isEmpty() || problems.isEmpty()) {
      throw new IllegalStateException("Cannot evaluate: indicators or problems list is empty");
    }

    // Compute means of each indicator across all problems
    double[] meanIndicatorValues = IntStream.range(0, indicators.size())
        .mapToDouble(
            indicatorIndex -> IntStream.range(0, problems.size())
                .mapToDouble(
                    problemIndex -> indicatorValuesPerProblem[problemIndex][indicatorIndex])
                .average()
                .orElse(0))
        .toArray();

    // Update the solution's objectives
    IntStream.range(0, indicators.size())
        .forEach(
            indicatorIndex -> solution.objectives()[indicatorIndex] = meanIndicatorValues[indicatorIndex]);
  }

  /**
   * Performs multiple independent runs of the base algorithm with the given
   * parameters and computes median indicator values.
   *
   * @param parameterArray the parameter settings to evaluate
   * @param problemId      the index of the problem to evaluate against
   * @return array of median indicator values, one for each quality indicator
   * @throws JMetalException           if the computed front dimensions don't
   *                                   match the reference front
   * @throws IndexOutOfBoundsException if problemId is out of bounds
   */
  private double[] computeIndependentRuns(String[] parameterArray, int problemId) {
    double[][] indicatorValues = new double[indicators.size()][numberOfIndependentRuns];

    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
      int evaluations = evaluationBudgetStrategy.getEvaluations(problemId);
      List<S> results = runAlgorithm(parameterArray, problemId, evaluations);
      double[][] front = extractNonDominatedFront(results);
      double[][] normalizedFront = normalizeFront(front, problemId);
      double[] runIndicators = computeIndicatorValuesForRun(normalizedFront, problemId, evaluations);

      for (int i = 0; i < indicators.size(); i++) {
        indicatorValues[i][runId] = runIndicators[i];
      }
    }

    double[] medianIndicatorValues = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      medianIndicatorValues[i] = median(indicatorValues[i]);
    }
    return medianIndicatorValues;
  }

  /**
   * Builds and runs the base algorithm with the given parameters on the specified problem.
   *
   * @param parameterArray the parameter settings
   * @param problemId      the index of the problem
   * @param evaluations    the evaluation budget for this run
   * @return the list of solutions produced by the algorithm
   */
  private List<S> runAlgorithm(String[] parameterArray, int problemId, int evaluations) {
    var algorithm = baseAlgorithm
        .createInstance(problems.get(problemId), evaluations)
        .parse(parameterArray)
        .build();

    algorithm.run();
    return algorithm.result();
  }

  /**
   * Extracts the non-dominated front from a list of solutions.
   *
   * @param solutions the solutions produced by an algorithm run
   * @return the objective value matrix of the non-dominated solutions
   */
  private double[][] extractNonDominatedFront(List<S> solutions) {
    NonDominatedSolutionListArchive<S> archive = new NonDominatedSolutionListArchive<>();
    archive.addAll(solutions);
    return getMatrixWithObjectiveValues(archive.solutions());
  }

  /**
   * Validates the front dimensions and normalizes it using the reference front
   * bounds for the given problem.
   *
   * @param front     the objective value matrix to normalize
   * @param problemId the index of the problem whose reference front provides bounds
   * @return the normalized front
   * @throws JMetalException if front dimensions don't match the reference front
   */
  private double[][] normalizeFront(double[][] front, int problemId) {
    double[][] referenceFront = referenceFronts.get(problemId);
    if (front[0].length != referenceFront[0].length) {
      throw new JMetalException(
          "The front dimension: "
              + front[0].length
              + " does not match the reference front dimension: "
              + referenceFront[0].length);
    }
    return NormalizeUtils.normalize(
        front,
        NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
        NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));
  }

  /**
   * Computes indicator values for a single run using the normalized front.
   *
   * @param normalizedFront the normalized objective value matrix
   * @param problemId       the index of the problem being evaluated
   * @param evaluations     the evaluation budget used for this run
   * @return array of indicator values, one per quality indicator
   */
  private double[] computeIndicatorValuesForRun(
      double[][] normalizedFront, int problemId, int evaluations) {
    double[] values = new double[indicators.size()];

    for (int i = 0; i < indicators.size(); i++) {
      QualityIndicator indicator = indicators.get(i).newInstance();
      if (indicator instanceof EvaluationsQualityIndicator evalIndicator) {
        evalIndicator.setNumberOfEvaluations(evaluations);
        values[i] = evaluations;
      } else {
        indicator.referenceFront(normalizedReferenceFronts.get(problemId));
        values[i] = indicator.compute(normalizedFront);
      }
    }
    return values;
  }

  /**
   * Computes the median of a double array.
   *
   * @param values the array of values (must not be null or empty)
   * @return the median value
   */
  private static double median(double[] values) {
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    if (n % 2 == 0) {
      return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    } else {
      return sorted[n / 2];
    }
  }
}
