package org.uma.evolver.problem;

import static org.uma.evolver.util.ParameterManagement.decodeParameter;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableMOEAD;
import org.uma.evolver.algorithm.ConfigurableNSGAII;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;

public class ConfigurableMOEADProblem extends AbstractDoubleProblem {
  private List<QualityIndicator> indicators ;
  private List<Parameter<?>> parameters ;
  private final StringBuilder nonConfigurableParameterString ;

  private DoubleProblem problem ;
  String referenceFrontFileName ;
  private double[][] normalizedReferenceFront ;
  private double[][] referenceFront ;

  private int numberOfIndependentRuns ;

  public ConfigurableMOEADProblem(DoubleProblem problem, String referenceFrontFileName, List<QualityIndicator> indicators,
      StringBuilder nonConfigurableParameterString) {
    this(problem, referenceFrontFileName, indicators, nonConfigurableParameterString, 1) ;
  }

  public ConfigurableMOEADProblem(DoubleProblem problem, String referenceFrontFileName, List<QualityIndicator> indicators,
      StringBuilder nonConfigurableParameterString, int numberOfIndependentRuns) {
    var algorithm = new ConfigurableMOEAD(problem) ;
    this.nonConfigurableParameterString = nonConfigurableParameterString ;
    this.indicators = indicators ;
    this.problem = problem ;
    this.numberOfIndependentRuns = numberOfIndependentRuns ;
    this.referenceFrontFileName = referenceFrontFileName ;
    parameters = AutoConfigurableAlgorithm.parameterFlattening(algorithm.configurableParameterList()) ;

    // Parameters to configure
    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i<parameters.size(); i++) {
      lowerLimit.add(0.0) ;
      upperLimit.add(0.99999) ;
    }

    computeNormalizedReferenceFront(referenceFrontFileName) ;

    variableBounds(lowerLimit, upperLimit);
    for (var parameter: parameters) {
      System.out.println(parameter.name()) ;
    }
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
    return "AutoMOEAD";
  }

  public List<Parameter<?>> parameters() {
    return parameters ;
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    StringBuilder parameterString = new StringBuilder() ;
    decodeParameters(solution, parameterString);

    parameterString.append(nonConfigurableParameterString) ;

    String[] parameters = parameterString.toString().split("\\s+") ;

    var algorithm = new ConfigurableMOEAD(problem) ;
    algorithm.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> moead = algorithm.create();
    moead.run();

    NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>() ;
    nonDominatedSolutions.addAll(moead.result()) ;

    double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions()) ;

    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    indicators.get(0).referenceFront(normalizedReferenceFront) ;
    indicators.get(1).referenceFront(normalizedReferenceFront);

    solution.objectives()[0] = indicators.get(0).compute(normalizedFront)  ;
    solution.objectives()[1] = indicators.get(1).compute(normalizedFront)   ;

   /*
    double[] objectives = computeIndependentRuns(numberOfIndependentRuns, algorithm) ;
    solution.objectives()[0] = objectives[0] ;
    solution.objectives()[1] = objectives[1] ;
   */
    return solution ;
  }

  private double[] computeIndependentRuns(int numberOfRuns, ConfigurableNSGAII algorithm) {
    double[] medianIndicatorValues = new double[indicators.size()] ;
    double[] valuesFirstIndicator = new double[numberOfRuns];
    double[] valuesSecondIndicator = new double[numberOfRuns] ;

    for (int i = 0; i < numberOfIndependentRuns; i++) {
      EvolutionaryAlgorithm<DoubleSolution> nsgaII = algorithm.create();
      nsgaII.run();

      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>() ;
      nonDominatedSolutions.addAll(nsgaII.result()) ;

      double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions()) ;

      double[][] normalizedFront =
          NormalizeUtils.normalize(
              front,
              NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
              NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

      indicators.get(0).referenceFront(normalizedReferenceFront) ;
      indicators.get(1).referenceFront(normalizedReferenceFront) ;

      valuesFirstIndicator[0] = indicators.get(0).compute(normalizedFront) ;
      valuesSecondIndicator[1] = indicators.get(1).compute(normalizedFront) ;
    }

    medianIndicatorValues[0] = median(valuesFirstIndicator) ;
    medianIndicatorValues[1] = median(valuesSecondIndicator) ;

    return medianIndicatorValues ;
  }

  private void decodeParameters(DoubleSolution solution, StringBuilder parameterString) {
    for (int i = 0; i < parameters.size(); i++) {
      String parameterName = parameters.get(i).name();
      String value = decodeParameter(parameters.get(i), solution.variables().get(i)) ;

      parameterString.append("--").append(parameterName).append(" ").append(value).append(" ");
    }
  }

  public void writeDecodedSolutionsFoFile(List<DoubleSolution> solutions, String fileName)
      throws IOException {
    FileWriter fileWriter = new FileWriter(fileName) ;
    PrintWriter printWriter = new PrintWriter(fileWriter) ;
    for (DoubleSolution solution: solutions) {
      StringBuilder parameterString = new StringBuilder() ;
      parameterString.append(nonConfigurableParameterString) ;
      decodeParameters(solution, parameterString);

      printWriter.println(parameterString);
    }
    printWriter.close();
  }
}
