package org.uma.evolver.configurablealgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public interface ConfigurableAlgorithmBuilder {
  ConfigurableAlgorithmBuilder parse(String[] args) ;
  List<Parameter<?>> configurableParameterList() ;

  ConfigurableAlgorithmBuilder createBuilderInstance() ;
  ConfigurableAlgorithmBuilder  createBuilderInstance(DoubleProblem problem,
      int maximumNumberOfEvaluations) ;

  Algorithm<List<DoubleSolution>> build() ;

  /**
   * Given a list of parameters, returns a list with of all of them and all of their sub-parameters
   *
   * @param parameters
   * @return A list of parameters
   */
  static List<Parameter<?>> parameterFlattening(List<Parameter<?>> parameters) {
    List<Parameter<?>> parameterList = new ArrayList<>() ;
    parameters.forEach(parameter -> {
      parameterList.add(parameter);
      parameterList.addAll(parameterFlattening(parameter.globalParameters()));
      List<Parameter<?>> specificParameters = parameter.specificParameters().stream().map(
          Pair::getRight).collect(
          Collectors.toList());
      parameterList.addAll(parameterFlattening(specificParameters));
    });
    return parameterList ;
  }
}
