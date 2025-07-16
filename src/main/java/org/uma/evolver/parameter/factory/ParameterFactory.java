package org.uma.evolver.parameter.factory;

import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import java.util.List;

@FunctionalInterface
public interface ParameterFactory<S extends Solution<?>> {
  CategoricalParameter createParameter(String parameterName, List<String> values) ;
}
