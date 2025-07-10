package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.List;
import java.util.Map;

/**
 * Processes integer parameters from YAML configuration.
 */
public class IntegerParameterProcessor implements ParameterProcessor {
  @Override
  @SuppressWarnings("unchecked")
  public void process(String parameterName, Object parameterConfig, ParameterSpace parameterSpace) {
    if (!(parameterConfig instanceof Map)) {
      throw new JMetalException("Invalid configuration for parameter " + parameterName + ": expected a map");
    }
    
    Map<String, Object> configMap = (Map<String, Object>) parameterConfig;
    
    // Require 'range' key for integer parameters
    if (!configMap.containsKey("range")) {
      throw new JMetalException("No range defined for integer parameter " + parameterName + ". Use 'range: [min, max]'");
    }
    
    // Reject 'values' key as it's not supported for integer parameters
    if (configMap.containsKey("values")) {
      throw new JMetalException("The 'values' key is not supported for integer parameters. Use 'range: [min, max]' for parameter " + parameterName);
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
    
    int minValue = ((Number) rangeList.get(0)).intValue();
    int maxValue = ((Number) rangeList.get(1)).intValue();
    
    if (minValue >= maxValue) {
      throw new JMetalException("Minimum value must be less than maximum value in range for parameter " + parameterName);
    }
    
    System.out.println("  - Creating integer parameter with range: [" + minValue + ", " + maxValue + "]");
    parameterSpace.put(new IntegerParameter(parameterName, minValue, maxValue));
  }
}
