package org.uma.evolver.meta.problem;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.util.EvaluationsQualityIndicator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Base class for meta-optimization problems, independent of the solution encoding used by the
 * meta-optimizer.
 *
 * <p>All evaluation logic (running the base algorithm, extracting the non-dominated front,
 * normalizing, computing quality indicators, aggregating across problems and independent runs)
 * lives here and is shared by all encodings.
 *
 * <p>Subclasses implement three encoding-specific hooks:
 * <ul>
 *   <li>{@link #numberOfVariables()} — variable count for the chosen encoding</li>
 *   <li>{@link #createSolution()} — creates a random meta-solution</li>
 *   <li>{@link #toParameterArray(Solution)} — converts a meta-solution to the parameter string
 *       array understood by the base algorithm</li>
 * </ul>
 *
 * @param <S>    the type of solutions produced by the base-level algorithm being configured
 * @param <META> the type of solutions used by the meta-optimizer (the encoding)
 */
public abstract class AbstractMetaOptimizationProblem<S extends Solution<?>, META extends Solution<?>>
    implements Problem<META> {

  protected final BaseLevelAlgorithm<S> baseAlgorithm;
  protected final List<Problem<S>> problems;
  protected final List<QualityIndicator> indicators;
  protected final EvaluationBudgetStrategy evaluationBudgetStrategy;
  protected final int numberOfIndependentRuns;
  protected final List<double[][]> referenceFronts;
  protected final List<double[][]> normalizedReferenceFronts;

  protected AbstractMetaOptimizationProblem(
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
    Check.that(!problems.isEmpty(), "Problems list must not be empty");
    Check.that(!indicators.isEmpty(), "Indicators list must not be empty");
    Check.that(
        problems.size() == referenceFrontFileNames.size(),
        "There must be the same number of problems as reference fronts: "
            + problems.size() + " vs " + referenceFrontFileNames.size());
    Check.that(numberOfIndependentRuns > 0, "Number of independent runs must be positive");
    evaluationBudgetStrategy.validate(problems.size());

    this.baseAlgorithm = baseAlgorithm;
    this.problems = new ArrayList<>(problems);
    this.indicators = new ArrayList<>(indicators);
    this.evaluationBudgetStrategy = evaluationBudgetStrategy;
    this.numberOfIndependentRuns = numberOfIndependentRuns;

    this.referenceFronts = loadReferenceFronts(referenceFrontFileNames);
    this.normalizedReferenceFronts = this.referenceFronts.stream()
        .map(NormalizeUtils::normalize)
        .toList();
  }

  @Override
  public int numberOfObjectives() {
    return indicators.size();
  }

  @Override
  public int numberOfConstraints() {
    return 0;
  }

  @Override
  public String name() {
    return "Meta-optimization problem";
  }

  public EvaluationBudgetStrategy evaluationBudgetStrategy() {
    return evaluationBudgetStrategy;
  }

  public List<Problem<S>> problems() {
    return problems;
  }

  @Override
  public META evaluate(META solution) {
    Check.notNull(solution);
    String[] parameterArray = toParameterArray(solution);
    double[][] indicatorValuesPerProblem = computeIndicatorValuesForAllProblems(parameterArray);
    updateObjectives(solution, indicatorValuesPerProblem);
    return solution;
  }

  protected abstract String[] toParameterArray(META solution);

  // ── Shared evaluation pipeline ────────────────────────────────────────────

  private double[][] computeIndicatorValuesForAllProblems(String[] parameterArray) {
    double[][] result = new double[problems.size()][indicators.size()];
    IntStream.range(0, problems.size())
        .forEach(id -> result[id] = computeIndependentRuns(parameterArray, id));
    return result;
  }

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

    double[] medians = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      medians[i] = median(indicatorValues[i]);
    }
    return medians;
  }

  private List<S> runAlgorithm(String[] parameterArray, int problemId, int evaluations) {
    var algorithm = baseAlgorithm
        .createInstance(problems.get(problemId), evaluations)
        .parse(parameterArray)
        .build();
    algorithm.run();
    return algorithm.result();
  }

  private double[][] extractNonDominatedFront(List<S> solutions) {
    NonDominatedSolutionListArchive<S> archive = new NonDominatedSolutionListArchive<>();
    archive.addAll(solutions);
    return getMatrixWithObjectiveValues(archive.solutions());
  }

  private double[][] normalizeFront(double[][] front, int problemId) {
    double[][] referenceFront = referenceFronts.get(problemId);
    if (front[0].length != referenceFront[0].length) {
      throw new JMetalException(
          "Front dimension " + front[0].length
              + " does not match reference front dimension " + referenceFront[0].length);
    }
    return NormalizeUtils.normalize(
        front,
        NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
        NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));
  }

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

  private void updateObjectives(META solution, double[][] indicatorValuesPerProblem) {
    for (int i = 0; i < indicators.size(); i++) {
      double sum = 0.0;
      for (int p = 0; p < problems.size(); p++) {
        sum += indicatorValuesPerProblem[p][i];
      }
      solution.objectives()[i] = sum / problems.size();
    }
  }

  private static double median(double[] values) {
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    return (n % 2 == 0) ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 : sorted[n / 2];
  }

  private static List<double[][]> loadReferenceFronts(List<String> fileNames) {
    List<double[][]> fronts = new ArrayList<>();
    for (String fileName : fileNames) {
      try {
        fronts.add(VectorUtils.readVectors(fileName, ","));
      } catch (IOException e) {
        throw new JMetalException("The file does not exist: " + fileName, e);
      }
    }
    return fronts;
  }
}
