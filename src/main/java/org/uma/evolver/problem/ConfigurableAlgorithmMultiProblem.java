package org.uma.evolver.problem;

import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.uma.evolver.util.ParameterManagement.decodeParametersToString;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

public class ConfigurableAlgorithmMultiProblem extends ConfigurableAlgorithmBaseProblem {

  private final List<DoubleProblem> problems;
  private List<QualityIndicator> indicators;
  private List<Integer> evaluations;
  private List<Parameter<?>> parameters;
  private List<double[][]> normalizedReferenceFronts;
  private List<double[][]> referenceFronts;
  private final int numberOfIndependentRuns;
  private ConfigurableAlgorithmBuilder configurableAlgorithm;

  public ConfigurableAlgorithmMultiProblem(ConfigurableAlgorithmBuilder configurableAlgorithm, List<DoubleProblem> problems,
                                           List<String> referenceFrontFileNames, List<QualityIndicator> indicators, List<Integer> evaluations) {
    this(configurableAlgorithm, problems, referenceFrontFileNames, indicators, evaluations, 1);
  }

  public ConfigurableAlgorithmMultiProblem(ConfigurableAlgorithmBuilder configurableAlgorithmBuilder, List<DoubleProblem> problems,
                                           List<String> referenceFrontFileNames, List<QualityIndicator> indicators, List<Integer> evaluations,
                                           int numberOfIndependentRuns) {

    if (problems.size() != referenceFrontFileNames.size()) {
      System.err.println("There must be the same number of problems as reference fronts: " + problems.size() + " vs " + referenceFrontFileNames.size());
      System.exit(1);
    }
    if (problems.size() != evaluations.size()) {
      System.err.println("There must be the same number of problems as different evaluations: " + problems.size() + " vs " + evaluations.size());
      System.exit(1);
    }

    this.configurableAlgorithm = configurableAlgorithmBuilder;
    this.indicators = indicators;
    this.numberOfIndependentRuns = numberOfIndependentRuns;
    this.problems = problems;
    this.evaluations = evaluations;

    parameters = ConfigurableAlgorithmBuilder.parameterFlattening(
        configurableAlgorithmBuilder.configurableParameterList());

    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i < parameters.size(); i++) {
      lowerLimit.add(0.0);
      upperLimit.add(1.0);
    }
    computeNormalizedReferenceFronts(referenceFrontFileNames);

    variableBounds(lowerLimit, upperLimit);
    for (var parameter : parameters) {
      JMetalLogger.logger.info(parameter.name() + ",");
    }
    JMetalLogger.logger.info("");
  }

  private void computeNormalizedReferenceFronts(List<String> referenceFrontFileNames) {
    referenceFronts = new ArrayList<double[][]>();
    normalizedReferenceFronts = new ArrayList<double[][]>();
    for (String referenceFrontFileName: referenceFrontFileNames) {
      double[][] referenceFront;
      try {
        referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
        referenceFronts.add(referenceFront);
      } catch (IOException e) {
        throw new JMetalException("The file does not exist", e);
      }
      normalizedReferenceFronts.add(NormalizeUtils.normalize(referenceFront));
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
    return "Configurable algorithm problem";
  }

  public List<Parameter<?>> parameters() {
    return parameters;
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    StringBuilder parameterString = decodeParametersToString(parameters, solution.variables());

    String[] parameterArray = parameterString.toString().split("\\s+");

    /*
    var algorithm = configurableAlgorithm
        .createBuilderInstance()
        .parse(parameterArray)
        .build();

    algorithm.run();

    NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>();
    nonDominatedSolutions.addAll(algorithm.result());

    double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());

    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    IntStream.range(0, indicators.size()).forEach(i -> {
      indicators.get(i).referenceFront(normalizedReferenceFront);
      solution.objectives()[i] = indicators.get(i).compute(normalizedFront);
    });
*/

    // Run each problem n independent times
    double[][] indicatorValuesPerProblem = new double[problems.size()][indicators.size()];
    for (int i = 0;i<problems.size();i++) {
      double[] medianIndicatorValues = computeIndependentRuns(parameterArray, i) ; // Values for each indicator
      indicatorValuesPerProblem[i] = medianIndicatorValues;
    }

    double[] medianProblemValues = new double[indicators.size()];
    for (int indicatorIndex = 0 ; indicatorIndex < indicators.size(); indicatorIndex++) {

      // Group the indicator values per problem
      double[] indicatorPerProblem = new double[problems.size()];
      for (int problemIndex = 0 ; problemIndex < problems.size(); problemIndex++)
        indicatorPerProblem[problemIndex] = indicatorValuesPerProblem[problemIndex][indicatorIndex];

      // Calculate the median per quality index, the mean can improve this if there are high values on some of the problems
      medianProblemValues[indicatorIndex] = median(indicatorPerProblem) ;
    }

    // Update the solution's objectives
    IntStream.range(0, indicators.size()).forEach(j -> solution.objectives()[j] = medianProblemValues[j]);

    return solution;
  }

  private double[] computeIndependentRuns(String[] parameterArray, int problem) {
    double[] medianIndicatorValues = new double[indicators.size()];
    double[][] indicatorValues = new double[indicators.size()][];
    IntStream.range(0, indicators.size()).forEach(i -> indicatorValues[i] = new double[numberOfIndependentRuns]);

    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
      var algorithm = configurableAlgorithm
          .createBuilderInstance(problems.get(problem), evaluations.get(problem))
          .parse(parameterArray)
          .build();

      algorithm.run();

      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>();
      nonDominatedSolutions.addAll(algorithm.result());

      double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());
      double[][] normalizedFront =
          NormalizeUtils.normalize(
              front,
              NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFronts.get(problem)),
              NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFronts.get(problem)));

      IntStream.range(0, indicators.size()).forEach(index -> {
        indicators.get(index).referenceFront(normalizedReferenceFronts.get(problem));
      });

      for (int indicatorId = 0; indicatorId < indicators.size(); indicatorId++) {
        QualityIndicator indicator = indicators.get(indicatorId);
        indicator.referenceFront(normalizedReferenceFronts.get(problem));
        indicatorValues[indicatorId][runId] = indicator.compute(normalizedFront);
      }
    }

    for (int i = 0 ; i < indicators.size(); i++) {
      medianIndicatorValues[i] = median(indicatorValues[i]) ;
    }

    return medianIndicatorValues;
  }
}
