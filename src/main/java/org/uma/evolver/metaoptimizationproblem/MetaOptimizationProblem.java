package org.uma.evolver.metaoptimizationproblem;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
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

public class MetaOptimizationProblem<S extends Solution<?>> extends AbstractDoubleProblem {
  private final BaseLevelAlgorithm<S> configurableAlgorithm;
  private final List<Problem<S>> problems;
  private final List<QualityIndicator> indicators;
  private final List<Integer> maxNumberOfEvaluations;
  private final List<Parameter<?>> parameters;
  private List<double[][]> normalizedReferenceFronts;
  private List<double[][]> referenceFronts;
  private final int numberOfIndependentRuns;

  public MetaOptimizationProblem(
      BaseLevelAlgorithm<S> configurableAlgorithm,
      List<Problem<S>> problems,
      List<String> referenceFrontFileNames,
      List<QualityIndicator> indicators,
      List<Integer> maxNumberOfEvaluations,
      int numberOfIndependentRuns) {
    this.configurableAlgorithm = configurableAlgorithm;
    this.problems = problems;
    this.indicators = indicators;
    this.maxNumberOfEvaluations = maxNumberOfEvaluations;
    this.numberOfIndependentRuns = numberOfIndependentRuns;

    this.parameters =
        ParameterManagement.parameterFlattening(configurableAlgorithm.parameterSpace().topLevelParameters());

    Check.that(
        problems.size() == referenceFrontFileNames.size(),
        "There must be the same number of problems as reference fronts: "
            + problems.size()
            + " vs "
            + referenceFrontFileNames.size());

    Check.that(
        problems.size() == maxNumberOfEvaluations.size(),
        "There must be the same number of problems as different evaluations: "
            + problems.size()
            + " vs "
            + maxNumberOfEvaluations.size());

    List<Double> lowerLimit = java.util.Collections.nCopies(parameters.size(), 0.0);
    List<Double> upperLimit = java.util.Collections.nCopies(parameters.size(), 1.0);

    variableBounds(lowerLimit, upperLimit);

    computeNormalizedReferenceFronts(referenceFrontFileNames);
  }

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

  @Override
  public int numberOfVariables() {
    return parameters.size();
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

  public List<Parameter<?>> parameters() {
    return parameters;
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    String[] parameterArray = convertSolutionToParameters(solution);
    double[][] indicatorValuesPerProblem = computeIndicatorValuesForAllProblems(parameterArray);
    updateSolutionWithMeanIndicatorValues(solution, indicatorValuesPerProblem);

    return solution;
  }

  private String[] convertSolutionToParameters(DoubleSolution solution) {
    StringBuilder parameterString =
        ParameterManagement.decodeParametersToString(parameters, solution.variables());
    return parameterString.toString().split("\\s+");
  }

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

  private double[] computeIndependentRuns(String[] parameterArray, int problemId) {
    double[] medianIndicatorValues = new double[indicators.size()];
    double[][] indicatorValues = new double[indicators.size()][];
    IntStream.range(0, indicators.size())
        .forEach(i -> indicatorValues[i] = new double[numberOfIndependentRuns]);

    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
      var algorithm =
          configurableAlgorithm
              .createInstance(problems.get(problemId), maxNumberOfEvaluations.get(problemId))
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
        indicator.referenceFront(normalizedReferenceFronts.get(problemId));
        indicatorValues[indicatorId][runId] = indicator.compute(normalizedFront);
      }
    }

    for (int i = 0; i < indicators.size(); i++) {
      medianIndicatorValues[i] = median(indicatorValues[i]);
    }

    return medianIndicatorValues;
  }
}
