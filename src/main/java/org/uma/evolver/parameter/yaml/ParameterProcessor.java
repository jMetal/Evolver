package org.uma.evolver.parameter.yaml;

import org.uma.evolver.parameter.ParameterSpace;

/**
 * Interface for parameter processors that handle different types of parameters.
 */
public interface ParameterProcessor {
  /**
   * Processes a parameter and adds it to the parameter space.
   *
   * @param parameterName Name of the parameter
   * @param parameterValues Configuration values for the parameter
   * @param parameterSpace The parameter space to add the parameter to
   * @throws org.uma.jmetal.util.errorchecking.JMetalException if there's an error processing the parameter
   */
  void process(String parameterName, Object parameterValues, ParameterSpace parameterSpace);
}
