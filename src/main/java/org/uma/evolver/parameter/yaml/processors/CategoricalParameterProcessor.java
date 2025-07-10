package org.uma.evolver.parameter.yaml.processors;

import java.util.Map;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes categorical parameters from YAML configuration.
 * 
 * <p>This processor handles both string and numeric categorical parameters, 
 * as well as parameters defined as maps (where keys become the categories).
 * It also supports global sub-parameters that can be associated with categories.</p>
 */
public class CategoricalParameterProcessor implements ParameterProcessor {
  
  /**
   * Processes a categorical parameter from YAML configuration.
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
    
    // Process parameter values first
    processParameterValues(parameterName, configMap, parameterSpace);
    
    // Then process global sub-parameters if they exist
    processGlobalSubParameters(parameterName, configMap, parameterSpace);
  }
  
  private void validateParameterConfig(String parameterName, Object parameterConfig) {
    if (parameterConfig == null) {
      throw new JMetalException("Configuration for parameter " + parameterName + " cannot be null");
    }
    if (!(parameterConfig instanceof Map)) {
      throw new JMetalException("Invalid configuration for parameter " + parameterName + ": expected a map");
    }
  }
  
  private void processParameterValues(String parameterName, Map<String, Object> configMap, ParameterSpace parameterSpace) {
    Object parameterValues = configMap.get("values");
    if (parameterValues == null) {
      throw new JMetalException("No values defined for parameter " + parameterName);
    }
    
    if (parameterValues instanceof List) {
      processListValues(parameterName, (List<?>) parameterValues, parameterSpace);
    } else if (parameterValues instanceof Map) {
      processMapValues(parameterName, (Map<String, Object>) parameterValues, parameterSpace);
    } else {
      throw new JMetalException("Unsupported values format for parameter " + parameterName + ". Expected a list or map.");
    }
  }
  
  private void processListValues(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    if (categoryList.isEmpty()) {
      System.out.println("  - Creating empty categorical parameter: " + parameterName);
      parameterSpace.put(new CategoricalParameter(parameterName, new ArrayList<>()));
      return;
    }
    
    if (categoryList.get(0) instanceof Number) {
      processNumericCategories(parameterName, categoryList, parameterSpace);
    } else {
      processStringCategories(parameterName, categoryList, parameterSpace);
    }
  }
  
  private void processMapValues(String parameterName, Map<String, Object> categoryMap, ParameterSpace parameterSpace) {
    List<String> stringCategories = new ArrayList<>(categoryMap.keySet());
    System.out.println("  - Creating categorical parameter from map keys: " + parameterName + " = " + stringCategories);
    parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
  }
  
  private void processNumericCategories(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    List<Integer> numericCategories = new ArrayList<>();
    for (Object item : categoryList) {
      if (item instanceof Number) {
        numericCategories.add(((Number) item).intValue());
      } else {
        throw new JMetalException("All values in numeric category list must be numbers for parameter " + 
            parameterName + ". Found: " + item);
      }
    }
    System.out.println("  - Creating integer categorical parameter: " + parameterName + " = " + numericCategories);
    parameterSpace.put(new CategoricalIntegerParameter(parameterName, numericCategories));
  }
  
  private void processStringCategories(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    List<String> stringCategories = new ArrayList<>();
    for (Object item : categoryList) {
      if (item == null) {
        throw new JMetalException("Category values cannot be null for parameter " + parameterName);
      }
      stringCategories.add(item.toString());
    }
    System.out.println("  - Creating string categorical parameter: " + parameterName + " = " + stringCategories);
    parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
  }
  
  @SuppressWarnings("unchecked")
  private void processGlobalSubParameters(String parameterName, Map<String, Object> configMap, ParameterSpace parameterSpace) {
    if (!configMap.containsKey("globalSubParameters")) {
      return;
    }
    
    Object subParamsObj = configMap.get("globalSubParameters");
    if (!(subParamsObj instanceof Map)) {
      System.err.println("Warning: globalSubParameters for " + parameterName + " should be a map");
      return;
    }
    
    Map<String, Object> subParams = (Map<String, Object>) subParamsObj;
    if (subParams.isEmpty()) {
      return;
    }
    
    System.out.println("  - Processing global sub-parameters for " + parameterName + ": " + 
        String.join(", ", subParams.keySet()));
    
    // Process each sub-parameter and add it to the parameter space
    for (Map.Entry<String, Object> entry : subParams.entrySet()) {
      String subParamName = entry.getKey();
      Object subParamConfig = entry.getValue();
      
      if (!(subParamConfig instanceof Map)) {
        System.err.println("Warning: Sub-parameter configuration for " + subParamName + " should be a map");
        continue;
      }
      
      try {
        // Process the sub-parameter using the same logic as the parent
        // Use the sub-parameter name directly without the parent prefix
        processParameterValues(subParamName, (Map<String, Object>) subParamConfig, parameterSpace);
        
        // Get the parent and sub-parameter from the parameter space
        Parameter<?> parentParameter = parameterSpace.get(parameterName);
        Parameter<?> subParameter = parameterSpace.get(subParamName);
        
        // Add the sub-parameter to the parent's global sub-parameters
        if (parentParameter instanceof CategoricalParameter) {
          ((CategoricalParameter) parentParameter).addGlobalSubParameter(subParameter);
          System.out.println("    - Added global sub-parameter " + subParamName + " to " + parameterName);
        } else if (parentParameter instanceof CategoricalIntegerParameter) {
          ((CategoricalIntegerParameter) parentParameter).addGlobalSubParameter(subParameter);
          System.out.println("    - Added global sub-parameter " + subParamName + " to " + parameterName);
        } else {
          System.err.println("Warning: Cannot add global sub-parameter to non-categorical parameter: " + parameterName);
        }
      } catch (Exception e) {
        System.err.println("Error processing global sub-parameter " + subParamName + ": " + e.getMessage());
        e.printStackTrace(); // Add stack trace for better debugging
      }
    }
  }
}
