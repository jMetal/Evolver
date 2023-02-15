package org.uma.evolver;

import static org.uma.evolver.ParameterValues.decodeParameter;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableNSGAII;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;


public class ConfigurableNSGAIIProblem extends AbstractDoubleProblem {
  private List<QualityIndicator> indicators ;
  private List<Parameter<?>> parameters ;

  public ConfigurableNSGAIIProblem(List<QualityIndicator> indicators) {
    var algorithm = new ConfigurableNSGAII() ;
    this.indicators = indicators ;
    parameters = AutoConfigurableAlgorithm.parameterFlattening(algorithm.configurableParameterList()) ;

    // Parameters to configure
    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i<parameters.size(); i++) {
      lowerLimit.add(0.0) ;
      upperLimit.add(1.0) ;
    }

    variableBounds(lowerLimit, upperLimit);
    for (var parameter: parameters) {
      System.out.println(parameter.name()) ;
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
    return "AutoNSGAII";
  }


  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    StringBuilder parameterString = new StringBuilder() ;
    for (int i = 0; i < parameters.size(); i++) {
      String parameterName = parameters.get(i).name();
      String value = decodeParameter(parameters.get(i), solution.variables().get(i)) ;

      parameterString.append("--" + parameterName + " " + value + " ") ;
    }

    parameterString.append("--maximumNumberOfEvaluations 8000 " ) ;
    parameterString.append("--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT1 " ) ;
    parameterString.append("--populationSize 100 " ) ;
    parameterString.append("--randomGeneratorSeed 4 " ) ;
    parameterString.append("--referenceFrontFileName resources/ZDT1.csv " ) ;

    String[] parameters = parameterString.toString().split("\\s+") ;

    var algorithm = new ConfigurableNSGAII() ;
    algorithm.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = algorithm.create();
    nsgaII.run();

    double[][] referenceFront = null;
    try {
      referenceFront = VectorUtils.readVectors("resources/ZDT1.csv", ",");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>() ;
    nonDominatedSolutions.addAll(nsgaII.result()) ;

    double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions()) ;

    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    indicators.get(0).referenceFront(normalizedReferenceFront) ;
    indicators.get(1).referenceFront(normalizedReferenceFront);

    solution.objectives()[0] = indicators.get(0).compute(normalizedFront)  ;
    solution.objectives()[1] = indicators.get(1).compute(normalizedFront)   ;

    return solution ;
  }
}
