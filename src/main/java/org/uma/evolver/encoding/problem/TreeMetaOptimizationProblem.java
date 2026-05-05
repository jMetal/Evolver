package org.uma.evolver.encoding.problem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
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
 * Meta-optimization problem using derivation tree encoding.
 *
 * <p>This class implements the same meta-optimization logic as
 * {@link org.uma.evolver.meta.problem.MetaOptimizationProblem} but operates on
 * {@link DerivationTreeSolution} instead of {@code DoubleSolution}. The tree encoding eliminates
 * inactive variables, ensuring that all variation operations affect the phenotype.
 *
 * <p>The evaluation flow is:
 * <ol>
 *   <li>Convert the derivation tree to a parameter string array via
 *       {@link DerivationTreeSolution#toParameterArray()}</li>
 *   <li>Run the base-level algorithm with those parameters on each training problem</li>
 *   <li>Compute quality indicators and assign them as objective values</li>
 * </ol>
 *
 * @param <S> the type of solutions used by the base algorithm being optimized
 * @author Antonio J. Nebro
 */
public class TreeMetaOptimizationProblem<S extends Solution<?>>
    implements Problem<DerivationTreeSolution> {

  private final BaseLevelAlgorithm<S> baseAlgorithm;
  private final List<Problem<S>> problems;
  private final List<QualityIndicator> indicators;
  private final EvaluationBudgetStrategy evaluationBudgetStrategy;
  private final int numberOfIndependentRuns;
  private final TreeSolutionGenerator solutionGenerator;
  private final List<double[][]> normalizedReferenceFronts;
  private final List<double[][]> referenceFronts;

  /**
   * Constructs a tree-based meta-optimization problem.
   *
   * @param baseAlgorithm the base algorithm to configure
   * @param problems the training problems
   * @param referenceFrontFileNames reference front files for each problem
   * @param indicators quality indicators to optimize
   * @param evaluationBudgetStrategy strategy for evaluation budgets
   * @param numberOfIndependentRuns independent runs per evaluation
   * @param solutionGenerator generator for random tree solutions
   */
  public TreeMetaOptimizationProblem(
      BaseLevelAlgorithm<S> baseAlgorithm,
      List<Problem<S>> problems,
      List<String> referenceFrontFileNames,
      List<QualityIndicator> indicators,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      int numberOfIndependentRuns,
      TreeSolutionGenerator solutionGenerator) {
    Check.notNull(baseAlgorithm);
    Check.notNull(problems);
    Check.notNull(referenceFrontFileNames);
    Check.notNull(indicators);
    Check.notNull(evaluationBudgetStrategy);
    Check.notNull(solutionGenerator);
    Check.that(!problems.isEmpty(), "Problems list must not be empty");
    Check.that(!indicators.isEmpty(), "Indicators list must not be empty");
    Check.that(
        problems.size() == referenceFrontFileNames.size(),
        "Number of problems and reference fronts must match");
    Check.that(numberOfIndependentRuns > 0, "Number of independent runs must be positive");

    this.baseAlgorithm = baseAlgorithm;
    this.problems = new ArrayList<>(problems);
    this.indicators = new ArrayList<>(indicators);
    this.evaluationBudgetStrategy = evaluationBudgetStrategy;
    this.numberOfIndependentRuns = numberOfIndependentRuns;
    this.solutionGenerator = solutionGenerator;

    this.referenceFronts = loadReferenceFronts(referenceFrontFileNames);
    this.normalizedReferenceFronts = normalizeReferenceFronts(this.referenceFronts);
  }

  /**
   * Returns the list of training problems.
   *
   * @return the training problems
   */
  public List<Problem<S>> problems() {
    return problems;
  }

  @Override
  public int numberOfVariables() {
    return baseAlgorithm.parameterSpace().topLevelParameters().size();
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
    return "TreeMetaOptimizationProblem";
  }

  @Override
  public DerivationTreeSolution evaluate(DerivationTreeSolution solution) {
    Check.notNull(solution);

    String[] parameterArray = solution.toParameterArray();
    double[][] indicatorValuesPerProblem = computeIndicatorValuesForAllProblems(parameterArray);
    updateObjectives(solution, indicatorValuesPerProblem);

    return solution;
  }

  @Override
  public DerivationTreeSolution createSolution() {
    return solutionGenerator.generate(numberOfObjectives());
  }

  private double[][] computeIndicatorValuesForAllProblems(String[] parameterArray) {
    double[][] result = new double[problems.size()][indicators.size()];

    IntStream.range(0, problems.size())
        .forEach(problemId -> {
          double[] medianValues = computeIndependentRuns(parameterArray, problemId);
          result[problemId] = medianValues;
        });

    return result;
  }

  private double[] computeIndependentRuns(String[] parameterArray, int problemId) {
    double[][] allRunValues = new double[numberOfIndependentRuns][indicators.size()];

    for (int run = 0; run < numberOfIndependentRuns; run++) {
      int evaluations = evaluationBudgetStrategy.getEvaluations(problemId);
      allRunValues[run] = computeIndicatorValuesForRun(parameterArray, problemId, evaluations);
    }

    return computeMedianPerIndicator(allRunValues);
  }

  private double[] computeIndicatorValuesForRun(
      String[] parameterArray, int problemId, int evaluations) {
    double[] values = new double[indicators.size()];

    try {
      Problem<S> problem = problems.get(problemId);
      var algorithmInstance = baseAlgorithm.createInstance(problem, evaluations);
      algorithmInstance.parse(parameterArray);
      var algorithm = algorithmInstance.build();
      algorithm.run();

      List<S> resultPopulation = algorithm.result();
      var archive = new NonDominatedSolutionListArchive<S>();
      resultPopulation.forEach(archive::add);
      List<S> nonDominatedSolutions = archive.solutions();

      double[][] front = getFrontValues(nonDominatedSolutions);
      double[][] normalizedFront = normalizeFront(front, problemId);

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
    } catch (Exception e) {
      Arrays.fill(values, Double.MAX_VALUE);
    }

    return values;
  }

  private void updateObjectives(
      DerivationTreeSolution solution, double[][] indicatorValuesPerProblem) {
    for (int i = 0; i < indicators.size(); i++) {
      double sum = 0.0;
      for (int p = 0; p < problems.size(); p++) {
        sum += indicatorValuesPerProblem[p][i];
      }
      solution.objectives()[i] = sum / problems.size();
    }
  }

  private double[] computeMedianPerIndicator(double[][] allRunValues) {
    double[] medians = new double[indicators.size()];
    for (int i = 0; i < indicators.size(); i++) {
      double[] column = new double[allRunValues.length];
      for (int r = 0; r < allRunValues.length; r++) {
        column[r] = allRunValues[r][i];
      }
      Arrays.sort(column);
      medians[i] = column[column.length / 2];
    }
    return medians;
  }

  private double[][] getFrontValues(List<S> solutions) {
    double[][] front = new double[solutions.size()][];
    for (int i = 0; i < solutions.size(); i++) {
      front[i] = solutions.get(i).objectives().clone();
    }
    return front;
  }

  private double[][] normalizeFront(double[][] front, int problemId) {
    double[][] referenceFront = referenceFronts.get(problemId);
    return NormalizeUtils.normalize(
        front,
        NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
        NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));
  }

  private List<double[][]> loadReferenceFronts(List<String> fileNames) {
    List<double[][]> fronts = new ArrayList<>();
    for (String fileName : fileNames) {
      try {
        fronts.add(VectorUtils.readVectors(fileName, ","));
      } catch (IOException e) {
        throw new JMetalException("Error reading reference front: " + fileName, e);
      }
    }
    return fronts;
  }

  private List<double[][]> normalizeReferenceFronts(List<double[][]> fronts) {
    return fronts.stream()
        .map(NormalizeUtils::normalize)
        .toList();
  }
}
