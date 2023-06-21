package org.uma.evolver.problem;

import static org.uma.evolver.util.ParameterManagement.decodeParametersToString;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class ConfigurableAlgorithmProblem extends ConfigurableAlgorithmBaseProblem  {

  private List<QualityIndicator> indicators;
  private List<Parameter<?>> parameters;
  private double[][] normalizedReferenceFront;
  private double[][] referenceFront;
  private final int numberOfIndependentRuns;
  private ConfigurableAlgorithmBuilder configurableAlgorithm;

  public ConfigurableAlgorithmProblem(ConfigurableAlgorithmBuilder configurableAlgorithm,
      String referenceFrontFileName, List<QualityIndicator> indicators) {
    this(configurableAlgorithm, referenceFrontFileName, indicators, 1);
  }

  public ConfigurableAlgorithmProblem(ConfigurableAlgorithmBuilder configurableAlgorithmBuilder,
      String referenceFrontFileName, List<QualityIndicator> indicators,
      int numberOfIndependentRuns) {
    this.configurableAlgorithm = configurableAlgorithmBuilder;
    this.indicators = indicators;
    this.numberOfIndependentRuns = numberOfIndependentRuns;

    parameters = ConfigurableAlgorithmBuilder.parameterFlattening(
        configurableAlgorithmBuilder.configurableParameterList());

    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i < parameters.size(); i++) {
      lowerLimit.add(0.0);
      upperLimit.add(1.0);
    }

    computeNormalizedReferenceFront(referenceFrontFileName);

    variableBounds(lowerLimit, upperLimit);
    for (var parameter : parameters) {
      JMetalLogger.logger.info(parameter.name() + ",");
    }
    JMetalLogger.logger.info("");
  }

  private void computeNormalizedReferenceFront(String referenceFrontFileName) {
    referenceFront = null;
    try {
      referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    } catch (IOException e) {
      throw new JMetalException("The file does not exist", e);
    }
    normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
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

    double[] medianIndicatorValues = computeIndependentRuns(parameterArray) ;
    IntStream.range(0, indicators.size()).forEach(i -> solution.objectives()[i] = medianIndicatorValues[i]);

    return solution;
  }

  private double[] computeIndependentRuns(String[] parameterArray) {
    double[] medianIndicatorValues = new double[indicators.size()];
    double[][] indicatorValues = new double[indicators.size()][];
    IntStream.range(0, indicators.size()).forEach(i -> indicatorValues[i] = new double[numberOfIndependentRuns]);

    for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
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

      IntStream.range(0, indicators.size()).forEach(index -> {
        indicators.get(index).referenceFront(normalizedReferenceFront);
      });

      for (int indicatorId = 0; indicatorId < indicators.size(); indicatorId++) {
        indicators.get(indicatorId).referenceFront(normalizedReferenceFront);
        indicatorValues[indicatorId][runId] = indicators.get(indicatorId).compute(normalizedFront);
      }
    }

    for (int i = 0 ; i < indicators.size(); i++) {
      medianIndicatorValues[i] = median(indicatorValues[i]) ;
    }

    return medianIndicatorValues;
  }
}
