package org.uma.evolver.metaoptimizationproblem;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
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
 * A meta-optimization problem that optimizes the parameters of an optimization algorithm
 * by evaluating its performance across multiple problem instances using quality indicators.
 * 
 * <p>This class implements a meta-optimization approach where the parameters of a base
 * algorithm are automatically tuned by evaluating its performance on multiple problem 
 * instances using various quality indicators. The optimization objective is to find parameter 
 * settings that work well across all problem instances in the training set.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Supports optimization of any configurable algorithm that implements {@code BaseLevelAlgorithm}</li>
 *   <li>Allows evaluation on multiple problem instances with different characteristics</li>
 *   <li>Uses quality indicators to assess algorithm performance</li>
 *   <li>Supports multiple independent runs to account for stochasticity</li>
 *   <li>Provides flexible evaluation budget control through {@link EvaluationBudgetStrategy}</li>
 * </ul>
 *
 * <p>Typical usage involves:</p>
 * <ol>
 *   <li>Define the base algorithm with its parameter space</li>
 *   <li>Select a set of training problems</li>
 *   <li>Configure quality indicators for evaluation</li>
 *   <li>Set up the evaluation budget strategy</li>
 *   <li>Use an optimization algorithm to solve the meta-optimization problem</li>
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
  private List<double[][]> normalizedReferenceFronts;
  
  /** Original reference fronts for each problem. */
  private List<double[][]> referenceFronts;
  
  /** Number of independent runs to perform for each evaluation. */
  private final int numberOfIndependentRuns;

  /**
   * Constructs a new meta-optimization problem instance.
   *
   * @param baseAlgorithm the base algorithm whose parameters will be optimized (must not be null)
   * @param problems the list of problems to evaluate the algorithm on (must not be null or empty)
   * @param referenceFrontFileNames list of file paths containing reference fronts for each problem 
   *        (must match the size of problems list)
   * @param indicators list of quality indicators to evaluate solutions (must not be null or empty)
   * @param evaluationBudgetStrategy strategy for determining the evaluation budget for each problem
   *        (must not be null and must be compatible with the number of problems)
   * @param numberOfIndependentRuns number of independent runs to perform for each evaluation
   *        (must be positive)
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if the sizes of problems and referenceFrontFileNames don't match,
   *         if the evaluation budget strategy is not compatible with the number of problems,
   *         or if numberOfIndependentRuns is not positive
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

    this.parameters =
        ParameterManagement.parameterFlattening(baseAlgorithm.parameterSpace().topLevelParameters());

    Check.that(
        problems.size() == referenceFrontFileNames.size(),
        "There must be the same number of problems as reference fronts: "
            + problems.size()
            + " vs "
            + referenceFrontFileNames.size());
            
    // Validate that the evaluation strategy is compatible with the number of problems
    evaluationBudgetStrategy.validate(problems.size());

    List<Double> lowerLimit = java.util.Collections.nCopies(parameters.size(), 0.0);
    List<Double> upperLimit = java.util.Collections.nCopies(parameters.size(), 1.0);

    variableBounds(lowerLimit, upperLimit);

    computeNormalizedReferenceFronts(referenceFrontFileNames);
  }

  /**
   * Loads and normalizes the reference fronts from the specified files.
   *
   * @param referenceFrontFileNames list of file paths containing reference fronts
   * @throws JMetalException if a reference front file cannot be read
   */
  private void computeNormalizedReferenceFronts(List<String> referenceFrontFileNames) {
    referenceFronts = new ArrayList<>();
    normalizedReferenceFronts = new ArrayList<>();
    for (String referenceFrontFileName : referenceFrontFileNames) {
      double[][] referenceFront;
      try {
        referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
        referenceFronts.add(referenceFront);
        normalizedReferenceFronts.add(NormalizeUtils.normalize(referenceFront));
      } catch (IOException e) {
        throw new JMetalException("The file does not exist", e);
      }
    }
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
    return parameters;
  }

  /**
   * Evaluates a solution by running the base algorithm with the specified parameter settings
   * and computing the quality indicators.
   *
   * @param solution the solution containing the parameter values to evaluate
   * @return the evaluated solution with objective values set
   * @throws NullPointerException if solution is null
   * @throws IllegalArgumentException if the solution's variables don't match the expected number of parameters
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
   * @return array of parameter strings in the format expected by the base algorithm
   * @throws NullPointerException if solution is null
   */
  private String[] convertSolutionToParameters(DoubleSolution solution) {
    StringBuilder parameterString =
        ParameterManagement.decodeParametersToString(parameters, solution.variables());
    return parameterString.toString().split("\\s+");
  }

  /**
   * Computes the indicator values for all problems using the given parameter settings.
   *
   * @param parameterArray the parameter settings to evaluate
   * @return a 2D array where each row corresponds to a problem and each column to an indicator
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
   * Updates the solution's objective values with the mean indicator values across all problems.
   *
   * @param solution the solution to update
   * @param indicatorValuesPerProblem a 2D array of indicator values [problemIndex][indicatorIndex]
   * @throws IllegalStateException if there are no indicators or problems
   */
  private void updateSolutionWithMeanIndicatorValues(
      DoubleSolution solution, double[][] indicatorValuesPerProblem) {
    // Validate inputs
    if (indicators.isEmpty() || problems.isEmpty()) {
      throw new IllegalStateException("Cannot evaluate: indicators or problems list is empty");
    }

    // Compute means of each indicator across all problems
    double[] meanIndicatorValues =
        IntStream.range(0, indicators.size())
            .mapToDouble(
                indicatorIndex ->
                    IntStream.range(0, problems.size())
                        .mapToDouble(
                            problemIndex -> indicatorValuesPerProblem[problemIndex][indicatorIndex])
                        .average()
                        .orElse(0))
            .toArray();

    // Update the solution's objectives
    IntStream.range(0, indicators.size())
        .forEach(
            indicatorIndex ->
                solution.objectives()[indicatorIndex] = meanIndicatorValues[indicatorIndex]);
  }

  /**
   * Performs multiple independent runs of the base algorithm with the given parameters
   * and computes median indicator values.
   *
   * @param parameterArray the parameter settings to evaluate
   * @param problemId the index of the problem to evaluate against
   * @return array of median indicator values, one for each quality indicator
   * @throws JMetalException if the computed front dimensions don't match the reference front
   * @throws IndexOutOfBoundsException if problemId is out of bounds
   */
  private double[] computeIndependentRuns(String[] parameterArray, int problemId) {
    double[] medianIndicatorValues = new double[indicators.size()];
    double[][] indicatorValues = new double[indicators.size()][];
    IntStream.range(0, indicators.size())
        .forEach(i -> indicatorValues[i] = new double[numberOfIndependentRuns]);

    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
      int evaluations = evaluationBudgetStrategy.getEvaluations(problemId);
      var algorithm =
          baseAlgorithm
              .createInstance(problems.get(problemId), evaluations)
              .parse(parameterArray)
              .build();

      algorithm.run();

      NonDominatedSolutionListArchive<S> nonDominatedSolutions =
          new NonDominatedSolutionListArchive<>();
      nonDominatedSolutions.addAll(algorithm.result());

      double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());
      if (front[0].length != referenceFronts.get(problemId)[0].length) {
        throw new JMetalException(
            "The front dimension: "
                + front[0].length
                + " is not equals to the reference front dimension: "
                + referenceFronts.get(problemId)[0].length);
      }
      double[][] normalizedFront =
          NormalizeUtils.normalize(
              front,
              NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFronts.get(problemId)),
              NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFronts.get(problemId)));

      for (int indicatorId = 0; indicatorId < indicators.size(); indicatorId++) {
        QualityIndicator indicator = indicators.get(indicatorId).newInstance();
        if (indicator.name().equals("Evaluations")) {
         ((EvaluationsQualityIndicator)indicator).setNumberOfEvaluations(evaluations);
         indicatorValues[indicatorId][runId] = evaluations;
        } else {
        indicator.referenceFront(normalizedReferenceFronts.get(problemId));
        indicatorValues[indicatorId][runId] = indicator.compute(normalizedFront);
        }
      }
    }

    for (int i = 0; i < indicators.size(); i++) {
      medianIndicatorValues[i] = median(indicatorValues[i]);
    }

    return medianIndicatorValues;
  }
}
