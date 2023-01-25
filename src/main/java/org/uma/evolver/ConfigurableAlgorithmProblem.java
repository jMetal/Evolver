package org.uma.evolver;

import java.util.List;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;

public class ConfigurableAlgorithmProblem implements DoubleProblem {
  private AutoConfigurableAlgorithm algorithm ;
  private List<QualityIndicator> indicators ;
  private List<Parameter<?>> parameters ;

  public ConfigurableAlgorithmProblem(AutoConfigurableAlgorithm algorithm, List<QualityIndicator> indicators) {
    this.algorithm = algorithm ;
    this.indicators = indicators ;
    parameters = algorithm.getAutoConfigurableParameterList() ;
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
    return null;
  }

  @Override
  public List<Bounds<Double>> variableBounds() {
    return null;
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    return null;
  }

  @Override
  public DoubleSolution createSolution() {
    return null;
  }
}
