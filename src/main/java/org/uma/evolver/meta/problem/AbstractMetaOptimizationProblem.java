package org.uma.evolver.meta.problem;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.impl.ThreadLocalRandomGenerator;

/**
 * Base class for meta-optimization problems, independent of the solution encoding used by the
 * meta-optimizer.
 *
 * <p>All evaluation logic (running the base algorithm, extracting the non-dominated front,
 * normalizing, computing quality indicators, aggregating across problems and independent runs)
 * lives here and is shared by all encodings.
 *
 * <p>If a problem of the training set has constraints, the indicators are computed on the feasible
 * solutions of each run, and the value of a configuration puts feasibility first (see {@link
 * ConstrainedIndicatorValue}): runs without feasible solutions count before their violation, and
 * that before the quality. Without constraints the evaluation is the usual one.
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

  /** The configurations being evaluated at this moment, by any meta-optimization problem. */
  private static final AtomicInteger EVALUATIONS_IN_PROGRESS = new AtomicInteger();

  protected final BaseLevelAlgorithm<S> baseAlgorithm;
  protected final List<Problem<S>> problems;
  protected final List<QualityIndicator> indicators;
  protected final EvaluationBudgetStrategy evaluationBudgetStrategy;
  protected final int numberOfIndependentRuns;
  protected final List<double[][]> referenceFronts;
  protected final List<double[][]> normalizedReferenceFronts;
  private final Queue<META> initialSolutions = new ConcurrentLinkedQueue<>();

  /** The number of random solutions whose violation sets the scale of a problem's violations. */
  static final int VIOLATION_SAMPLE_SIZE = 100;

  /** Whether some problem has constraints, so that configurations are evaluated as such. */
  private final boolean constrained;

  /** The scale of the violations of each problem (1 for the problems without constraints). */
  private final double[] violationScales;

  /** What the runs of a configuration on a problem with constraints give. */
  private record ProblemSummary(double[] values, int failedRuns, double sumOfScaledViolations) {}

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

    this.constrained =
        this.problems.stream().anyMatch(problem -> problem.numberOfConstraints() > 0);
    this.violationScales = new double[this.problems.size()];
    Arrays.fill(violationScales, 1.0);
    if (constrained) {
      for (QualityIndicator indicator : this.indicators) {
        if (!(indicator instanceof EvaluationsQualityIndicator)) {
          ConstrainedIndicatorValue.threshold(indicator);
        }
      }
      for (int id = 0; id < this.problems.size(); id++) {
        if (this.problems.get(id).numberOfConstraints() > 0) {
          violationScales[id] = violationScale(this.problems.get(id));
        }
      }
    }
  }

  /** The scale of a problem's violations, from those of a sample of random solutions. */
  private static <S extends Solution<?>> double violationScale(Problem<S> problem) {
    double[] violations = new double[VIOLATION_SAMPLE_SIZE];
    for (int i = 0; i < VIOLATION_SAMPLE_SIZE; i++) {
      S solution = problem.createSolution();
      problem.evaluate(solution);
      violations[i] = violation(solution);
    }
    return ConstrainedIndicatorValue.scale(violations);
  }

  /** The (non-negative) overall constraint violation of a solution. */
  private static double violation(Solution<?> solution) {
    return Math.abs(ConstraintHandling.overallConstraintViolationDegree(solution));
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
    double[] objectives = evaluateConfiguration(toParameterArray(solution));
    System.arraycopy(objectives, 0, solution.objectives(), 0, objectives.length);
    return solution;
  }

  /**
   * Evaluates a configuration of the base-level algorithm exactly as the meta-optimizer evaluates
   * its solutions: the algorithm is run with it {@code numberOfIndependentRuns} times on every
   * problem, and the value of each indicator is the mean over the problems of its median over the
   * runs. If a problem has constraints, the value is {@link ConstrainedIndicatorValue#value}
   * instead, with the indicators computed on the feasible solutions. Useful to re-evaluate the
   * configurations found by a training run, for instance with more independent runs than the
   * training used.
   *
   * @param parameterArray the configuration, as the tokens of a configuration string ({@code
   *     "--param1 value1 --param2 value2 ..."} split by whitespace)
   * @return the value of each indicator, in the order of the indicators of this problem
   */
  public double[] evaluateConfiguration(String[] parameterArray) {
    Check.notNull(parameterArray);
    // Configurations evaluated by several threads at once (parallel meta-optimizers, the evaluation
    // server) would contend for jMetal's single random generator, so from then on each thread has
    // its own. A single thread of evaluations keeps drawing from the generator it shares with the
    // rest of the program, as before
    if (EVALUATIONS_IN_PROGRESS.incrementAndGet() > 1) {
      ThreadLocalRandomGenerator.install();
    }
    try {
      return computeConfigurationValues(parameterArray);
    } finally {
      EVALUATIONS_IN_PROGRESS.decrementAndGet();
    }
  }

  private double[] computeConfigurationValues(String[] parameterArray) {
    if (constrained) {
      return evaluateConfigurationWithConstraints(parameterArray);
    }
    double[][] indicatorValuesPerProblem = computeIndicatorValuesForAllProblems(parameterArray);
    double[] objectives = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      double sum = 0.0;
      for (int p = 0; p < problems.size(); p++) {
        sum += indicatorValuesPerProblem[p][i];
      }
      objectives[i] = sum / problems.size();
    }
    return objectives;
  }

  /**
   * Makes the next calls to {@link #createSolution()} return these solutions (known
   * configurations, encoded with {@code ConfigurationEncoding}) before random ones. Every
   * meta-optimizer creates its first solutions with {@code createSolution()}, so they start its
   * initial population (or, for random search, its first samples).
   *
   * @param solutions the solutions to return first, in order
   */
  public void initialSolutions(List<META> solutions) {
    initialSolutions.addAll(solutions);
  }

  /**
   * The next solution given to {@link #initialSolutions}, or null when there are no more.
   *
   * @return a copy of the next initial solution, or null
   */
  @SuppressWarnings("unchecked")
  protected META nextInitialSolution() {
    META solution = initialSolutions.poll();
    return solution == null ? null : (META) solution.copy();
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
    try {
      var algorithm = baseAlgorithm
          .createInstance(problems.get(problemId), evaluations)
          .parse(parameterArray)
          .build();
      algorithm.run();
      return algorithm.result();
    } catch (RuntimeException exception) {
      // Without this context, the error of a component (for instance, a tournament larger than
      // the population) does not tell which configuration of the training caused it
      throw new JMetalException(
          String.format(
              "A configuration failed on problem %s (%d evaluations): %s%nConfiguration: %s",
              problems.get(problemId).name(),
              evaluations,
              exception.getMessage(),
              String.join(" ", parameterArray)),
          exception);
    }
  }

  private double[] evaluateConfigurationWithConstraints(String[] parameterArray) {
    ProblemSummary[] summaries = new ProblemSummary[problems.size()];
    IntStream.range(0, problems.size())
        .forEach(id -> summaries[id] = computeIndependentRunsWithConstraints(parameterArray, id));
    int failedRuns = 0;
    double sumOfScaledViolations = 0.0;
    for (ProblemSummary summary : summaries) {
      failedRuns += summary.failedRuns();
      sumOfScaledViolations += summary.sumOfScaledViolations();
    }
    double[] objectives = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      double sum = 0.0;
      for (ProblemSummary summary : summaries) {
        sum += summary.values()[i];
      }
      double mean = sum / problems.size();
      objectives[i] =
          indicators.get(i) instanceof EvaluationsQualityIndicator
              ? mean
              : ConstrainedIndicatorValue.value(failedRuns, sumOfScaledViolations, mean);
    }
    return objectives;
  }

  /**
   * Runs a configuration on a problem of a training set with constraints. The indicators are
   * computed on the feasible solutions of each run and bounded (see {@link
   * ConstrainedIndicatorValue#bounded}); a run without
   * feasible solutions counts as failed, with the scaled violation of its least violated solution.
   * The value of each indicator is its median over the feasible runs, or its threshold if there
   * are none (the evaluations, their median over all the runs).
   */
  private ProblemSummary computeIndependentRunsWithConstraints(
      String[] parameterArray, int problemId) {
    List<double[]> feasibleRunValues = new ArrayList<>();
    double[] evaluationsPerRun = new double[numberOfIndependentRuns];
    int failedRuns = 0;
    double sumOfScaledViolations = 0.0;
    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
      int evaluations = evaluationBudgetStrategy.getEvaluations(problemId);
      evaluationsPerRun[runId] = evaluations;
      List<S> results = runAlgorithm(parameterArray, problemId, evaluations);
      List<S> feasible = results.stream().filter(ConstraintHandling::isFeasible).toList();
      if (feasible.isEmpty()) {
        failedRuns++;
        sumOfScaledViolations +=
            results.isEmpty()
                ? 1.0
                : ConstrainedIndicatorValue.scaledViolation(
                    results.stream()
                        .mapToDouble(AbstractMetaOptimizationProblem::violation)
                        .min()
                        .orElseThrow(),
                    violationScales[problemId]);
      } else {
        double[][] normalizedFront = normalizeFront(extractNonDominatedFront(feasible), problemId);
        double[] runValues = computeIndicatorValuesForRun(normalizedFront, problemId, evaluations);
        for (int i = 0; i < indicators.size(); i++) {
          if (!(indicators.get(i) instanceof EvaluationsQualityIndicator)) {
            runValues[i] = ConstrainedIndicatorValue.bounded(runValues[i], indicators.get(i));
          }
        }
        feasibleRunValues.add(runValues);
      }
    }

    double[] values = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      if (indicators.get(i) instanceof EvaluationsQualityIndicator) {
        values[i] = median(evaluationsPerRun);
      } else if (feasibleRunValues.isEmpty()) {
        values[i] = ConstrainedIndicatorValue.threshold(indicators.get(i));
      } else {
        int indicator = i;
        values[i] = median(feasibleRunValues.stream().mapToDouble(run -> run[indicator]).toArray());
      }
    }
    return new ProblemSummary(values, failedRuns, sumOfScaledViolations);
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
        // A copy of the rows array: the configurations are evaluated in parallel and share the
        // normalized reference fronts, so an indicator that sorted the reference front in place
        // (as jMetal's Spread did up to 7.6) would corrupt it for the other threads mid-sort.
        indicator.referenceFront(normalizedReferenceFronts.get(problemId).clone());
        double value = indicator.compute(normalizedFront);
        values[i] = Double.isFinite(value) ? value
            : (indicator.isTheLowerTheIndicatorValueTheBetter() ? Double.MAX_VALUE : 0.0);
      }
    }
    return values;
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
