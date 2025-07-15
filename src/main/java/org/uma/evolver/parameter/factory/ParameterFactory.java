package org.uma.evolver.parameter.factory;

import org.uma.evolver.parameter.type.CategoricalParameter;

import java.util.List;

@FunctionalInterface
public interface ParameterFactory {
  CategoricalParameter createParameter(String parameterName, List<String> values) ;
}
