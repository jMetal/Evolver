package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.InertiaWeightComputingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.ConstantValueStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.LinearDecreasingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.LinearIncreasingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.RandomSelectedValueStrategy;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class InertiaWeightComputingParameter extends CategoricalParameter {
  public InertiaWeightComputingParameter(List<String> mutationOperators) {
    super("inertiaWeightComputingStrategy", mutationOperators);
  }

  public InertiaWeightComputingStrategy getParameter() {
    InertiaWeightComputingStrategy result;

    switch (value()) {
      case "constantValue":
        Double weight = (Double) findSpecificSubParameter("weight").value();
        result = new ConstantValueStrategy(weight) ;
        break;
      case "randomSelectedValue":
        Double weightMin = (Double) findSpecificSubParameter("weightMin").value();
        Double weightMax = (Double) findSpecificSubParameter("weightMax").value();
        result = new RandomSelectedValueStrategy(weightMin, weightMax) ;
        break;
      case "linearDecreasingValue":
        weightMin = (Double) findSpecificSubParameter("weightMin").value();
        weightMax = (Double) findSpecificSubParameter("weightMax").value();
        int iterations = (Integer) nonConfigurableSubParameters().get("maxIterations") ;
        int swarmSize = (Integer) nonConfigurableSubParameters().get("swarmSize") ;
        result = new LinearDecreasingStrategy(weightMin, weightMax, iterations, swarmSize) ;
        break;
      case "linearIncreasingValue":
        weightMin = (Double) findSpecificSubParameter("weightMin").value();
        weightMax = (Double) findSpecificSubParameter("weightMax").value();
        iterations = (Integer) nonConfigurableSubParameters().get("maxIterations");
        swarmSize = (Integer) nonConfigurableSubParameters().get("swarmSize");
        result =new LinearIncreasingStrategy(weightMin, weightMax, iterations, swarmSize) ;
        break;
      default:
        throw new JMetalException("Inertia weight computing strategy does not exist: " + name());
    }
    return result;
  }
}
