package org.uma.evolver.problem;

import static org.uma.evolver.util.ParameterManagement.decodeParametersToString;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithm;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;

public class ConfigurableAlgorithmProblem extends AbstractDoubleProblem {

  private List<QualityIndicator> indicators;
  private List<Parameter<?>> parameters;
  private double[][] normalizedReferenceFront;
  private double[][] referenceFront;
  private final int numberOfIndependentRuns;
  private ConfigurableAlgorithm configurableAlgorithm;

  public ConfigurableAlgorithmProblem(ConfigurableAlgorithm configurableAlgorithm,
      String referenceFrontFileName, List<QualityIndicator> indicators) {
    this(configurableAlgorithm, referenceFrontFileName, indicators,1);
  }

  public ConfigurableAlgorithmProblem(ConfigurableAlgorithm configurableAlgorithm,
      String referenceFrontFileName, List<QualityIndicator> indicators,
      int numberOfIndependentRuns) {
    this.configurableAlgorithm = configurableAlgorithm;
    this.indicators = indicators;
    this.numberOfIndependentRuns = numberOfIndependentRuns;

    parameters = ConfigurableAlgorithm.parameterFlattening(
        configurableAlgorithm.configurableParameterList());

    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i < parameters.size(); i++) {
      lowerLimit.add(0.0);
      upperLimit.add(1.0);
    }

    computeNormalizedReferenceFront(referenceFrontFileName);

    variableBounds(lowerLimit, upperLimit);
    for (var parameter : parameters) {
      System.out.print(parameter.name() + ",");
    }
    System.out.println();
  }

  private void computeNormalizedReferenceFront(String referenceFrontFileName) {
    referenceFront = null;
    try {
      referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    return "AutoNSGAII";
  }

  public List<Parameter<?>> parameters() {
    return parameters;
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    StringBuilder parameterString = decodeParametersToString(parameters, solution.variables());

    String[] parameterArray = parameterString.toString().split("\\s+");

    var algorithm = configurableAlgorithm
        .createInstance()
        .parse(parameterArray)
        .create() ;

    algorithm.run();

    NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>();
    nonDominatedSolutions.addAll(algorithm.result());

    double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());

    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    indicators.get(0).referenceFront(normalizedReferenceFront);
    indicators.get(1).referenceFront(normalizedReferenceFront);

    solution.objectives()[0] = indicators.get(0).compute(normalizedFront);
    solution.objectives()[1] = indicators.get(1).compute(normalizedFront);

   /*
    double[] objectives = computeIndependentRuns(numberOfIndependentRuns, algorithm) ;
    solution.objectives()[0] = objectives[0] ;
    solution.objectives()[1] = objectives[1] ;
   */
    return solution;
  }

  private double[] computeIndependentRuns(int numberOfRuns, ConfigurableNSGAII algorithm) {
    double[] medianIndicatorValues = new double[indicators.size()];
    double[] valuesFirstIndicator = new double[numberOfRuns];
    double[] valuesSecondIndicator = new double[numberOfRuns];

    for (int i = 0; i < numberOfIndependentRuns; i++) {
      EvolutionaryAlgorithm<DoubleSolution> nsgaII = algorithm.create();
      nsgaII.run();

      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>();
      nonDominatedSolutions.addAll(nsgaII.result());

      double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());

      double[][] normalizedFront =
          NormalizeUtils.normalize(
              front,
              NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
              NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

      indicators.get(0).referenceFront(normalizedReferenceFront);
      indicators.get(1).referenceFront(normalizedReferenceFront);

      valuesFirstIndicator[i] = indicators.get(0).compute(normalizedFront);
      valuesSecondIndicator[i] = indicators.get(1).compute(normalizedFront);
    }

    medianIndicatorValues[0] = median(valuesFirstIndicator);
    medianIndicatorValues[1] = median(valuesSecondIndicator);

    return medianIndicatorValues;
  }
}
