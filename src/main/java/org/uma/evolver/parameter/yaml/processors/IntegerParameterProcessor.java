package org.uma.evolver.parameter.yaml.processors;

import java.util.List;
import java.util.Map;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Processes integer parameters from YAML configuration.
 *
 * <p>This processor handles integer parameters defined with a range [min, max].
 * The range is required and must be specified as a list of two integers.</p>
 */
public class IntegerParameterProcessor implements ParameterProcessor {
  /**
   * Creates a new IntegerParameterProcessor.
   */
  public IntegerParameterProcessor() {
    // No initialization needed
  }

  /**
   * Processes an integer parameter from YAML configuration.
   *
   * @param parameterName the name of the parameter
   * @param parameterConfig the configuration object for the parameter
   * @param parameterSpace the parameter space to add the parameter to
   * @throws JMetalException if the configuration is invalid
   */
  @Override
  @SuppressWarnings("unchecked")
  public void process(String parameterName, Object parameterConfig, ParameterSpace parameterSpace) {
    validateParameterConfig(parameterName, parameterConfig);
    Map<String, Object> configMap = (Map<String, Object>) parameterConfig;
    
    validateNoValuesKey(parameterName, configMap);
    validateRangeExists(parameterName, configMap);
    
    int[] range = extractAndValidateRange(parameterName, configMap);
    parameterSpace.put(new IntegerParameter(parameterName, range[0], range[1]));
  }
  
  /**
   * Validates the configuration object for a parameter.
   *
   * @param parameterName the name of the parameter being validated
   * @param parameterConfig the configuration object to validate
   * @throws JMetalException if the configuration is null or not a Map
   */
  private void validateParameterConfig(String parameterName, Object parameterConfig) {
    Check.notNull(parameterConfig);
    Check.that(parameterConfig instanceof Map, 
        "Invalid configuration for parameter " + parameterName + ": expected a map");
  }
  
  /**
   * Validates that the 'values' key is not present in the configuration.
   *
   * @param parameterName the name of the parameter being validated
   * @param configMap the configuration map to check
   * @throws JMetalException if 'values' key is present
   */
  private void validateNoValuesKey(String parameterName, Map<String, Object> configMap) {
    Check.that(!configMap.containsKey("values"), 
        "The 'values' key is not supported for integer parameters. " +
        "Use 'range: [min, max]' for parameter " + parameterName);
  }
  
  /**
   * Validates that a range is defined in the configuration.
   *
   * @param parameterName the name of the parameter being validated
   * @param configMap the configuration map to check
   * @throws JMetalException if 'range' key is missing
   */
  private void validateRangeExists(String parameterName, Map<String, Object> configMap) {
    Check.that(configMap.containsKey("range"),
        "No range defined for integer parameter " + parameterName + ". " +
        "Use 'range: [min, max]'");
  }
  
  /**
   * Extracts and validates the range from the configuration.
   *
   * @param parameterName the name of the parameter being processed
   * @param configMap the configuration map containing the range
   * @return an array containing [minValue, maxValue]
   * @throws JMetalException if the range is invalid or malformed
   */
  private int[] extractAndValidateRange(String parameterName, Map<String, Object> configMap) {
    Object rangeObj = configMap.get("range");
    Check.that(rangeObj instanceof List, 
        "Range for parameter " + parameterName + " must be a list [min, max]");
    
    List<?> rangeList = (List<?>) rangeObj;
    Check.that(rangeList.size() == 2, 
        "Range for parameter " + parameterName + " must contain exactly 2 values [min, max]");
    
    Check.that(
        rangeList.get(0) instanceof Integer && rangeList.get(1) instanceof Integer,
        "Both range values for parameter " + parameterName + " must be numbers");
    
    int minValue = ((Integer) rangeList.get(0)).intValue();
    int maxValue = ((Integer) rangeList.get(1)).intValue();
    
    Check.that(minValue < maxValue, 
        "Minimum value must be less than maximum value in range for parameter " + parameterName);
    
    return new int[]{minValue, maxValue};
  }
}
