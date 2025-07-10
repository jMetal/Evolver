package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.List;
import java.util.Map;

/**
 * Processes double/real parameters from YAML configuration.
 */
public class DoubleParameterProcessor implements ParameterProcessor {
  @Override
  @SuppressWarnings("unchecked")
  public void process(String parameterName, Object parameterConfig, ParameterSpace parameterSpace) {
    if (!(parameterConfig instanceof Map)) {
      throw new JMetalException("Invalid configuration for parameter " + parameterName + ": expected a map");
    }
    
    Map<String, Object> configMap = (Map<String, Object>) parameterConfig;
    
    // Require 'range' key for double parameters
    if (!configMap.containsKey("range")) {
      throw new JMetalException("No range defined for double parameter " + parameterName + ". Use 'range: [min, max]'");
    }
    
    // Reject 'values' key as it's not supported for double parameters
    if (configMap.containsKey("values")) {
      throw new JMetalException("The 'values' key is not supported for double parameters. Use 'range: [min, max]' for parameter " + parameterName);
    }
    
    // Get and validate range
    Object rangeObj = configMap.get("range");
    if (!(rangeObj instanceof List)) {
      throw new JMetalException("Range for parameter " + parameterName + " must be a list [min, max]");
    }
    
    List<?> rangeList = (List<?>) rangeObj;
    if (rangeList.size() != 2) {
      throw new JMetalException("Range for parameter " + parameterName + " must contain exactly 2 values [min, max]");
    }
    
    if (!(rangeList.get(0) instanceof Number) || !(rangeList.get(1) instanceof Number)) {
      throw new JMetalException("Both range values for parameter " + parameterName + " must be numbers");
    }
    
    double minValue = ((Number) rangeList.get(0)).doubleValue();
    double maxValue = ((Number) rangeList.get(1)).doubleValue();
    
    if (minValue >= maxValue) {
      throw new JMetalException("Minimum value must be less than maximum value in range for parameter " + parameterName);
    }
    
    System.out.println("  - Creating double parameter with range: [" + minValue + ", " + maxValue + "]");
    parameterSpace.put(new DoubleParameter(parameterName, minValue, maxValue));
  }
}
