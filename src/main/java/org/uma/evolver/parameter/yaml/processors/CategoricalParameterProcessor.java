package org.uma.evolver.parameter.yaml.processors;

import java.util.Map;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
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
    
    // Process specific subparameters for each value
    processSpecificSubParameters(parameterName, configMap, parameterSpace);
    
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
  
  /**
   * Processes specific subparameters that are defined for particular values of a categorical parameter.
   * These subparameters are only active when their parent parameter has a specific value.
   *
   * @param parameterName the name of the parent parameter
   * @param configMap the configuration map containing the parameter definition
   * @param parameterSpace the parameter space to add parameters to
   */
  @SuppressWarnings("unchecked")
  private void processSpecificSubParameters(String parameterName, Map<String, Object> configMap, ParameterSpace parameterSpace) {
    // Get the values map which contains the categorical options
    Object valuesObj = configMap.get("values");
    if (!(valuesObj instanceof Map)) {
      return; // No values or values is not a map
    }
    
    Map<String, Object> valuesMap = (Map<String, Object>) valuesObj;
    
    // Check each value for specific subparameters
    for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
      String valueName = entry.getKey();
      Object valueConfig = entry.getValue();
      
      // For the YAML structure, the values can be either a simple string or a map with subparameters
      if (valueConfig instanceof Map) {
        Map<String, Object> valueConfigMap = (Map<String, Object>) valueConfig;
        processSpecificSubParametersForValue(parameterName, valueName, valueConfigMap, parameterSpace);
      }
      // Handle the case where the value is a map with nested values (like in the YAML example)
      else if (valueName.equals("externalArchive") && valueConfig instanceof Map) {
        Map<String, Object> externalArchiveMap = (Map<String, Object>) valueConfig;
        processSpecificSubParametersForValue(parameterName, "externalArchive", externalArchiveMap, parameterSpace);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void processSpecificSubParametersForValue(String parentParamName, String valueName, 
      Map<String, Object> valueConfigMap, ParameterSpace parameterSpace) {
    if (!valueConfigMap.containsKey("conditionalSubParameters")) {
      return;
    }
    
    Object subParamsObj = valueConfigMap.get("conditionalSubParameters");
    if (!(subParamsObj instanceof Map)) {
      return;
    }
    
    Map<String, Object> subParams = (Map<String, Object>) subParamsObj;
    if (subParams.isEmpty()) {
      return;
    }
    
    String fullParentName = parentParamName + "." + valueName;
    System.out.println("  - Found conditional subparameters for " + fullParentName + ": " +
        String.join(", ", subParams.keySet()));
    
    // Process each specific subparameter
    for (Map.Entry<String, Object> subParamEntry : subParams.entrySet()) {
      String subParamName = subParamEntry.getKey();
      Object subParamConfig = subParamEntry.getValue();
      
      if (!(subParamConfig instanceof Map)) {
        continue;
      }
      
      Map<String, Object> subParamConfigMap = (Map<String, Object>) subParamConfig;
      String subParamType = subParamConfigMap.getOrDefault("type", "unknown").toString();
      System.out.println("    - " + subParamName + " (type: " + subParamType + ")");
      
      try {
        // Process the subparameter using only its base name (without any prefix)
        if (parameterSpace instanceof YAMLParameterSpace) {
          YAMLParameterSpace yamlParameterSpace = (YAMLParameterSpace) parameterSpace;
          ParameterProcessor processor = yamlParameterSpace.getParameterProcessor(subParamType);
          
          if (processor != null) {
            // Process the subparameter with just its base name
            processor.process(subParamName, subParamConfigMap, parameterSpace);
            
            // Get the parent parameter and add this as a specific subparameter
            Parameter<?> parentParam = parameterSpace.get(parentParamName);
            if (parentParam != null) {
              Parameter<?> subParam = parameterSpace.get(subParamName);
              if (subParam != null) {
                parentParam.addConditionalSubParameter(valueName, subParam);
                System.out.println("      Added as specific subparameter for " + parentParamName + " when value is " + valueName);
              }
            }
          } else {
            System.err.println("      Warning: No processor found for type " + subParamType);
          }
        }
      } catch (Exception e) {
        System.err.println("      Error processing specific subparameter " + subParamName + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  /**
   * Processes global sub-parameters that are always active regardless of the parameter's value.
   * These subparameters are added to the parameter space and linked to their parent parameter.
   *
   * @param parameterName the name of the parent parameter
   * @param configMap the configuration map containing the parameter definition
   * @param parameterSpace the parameter space to add parameters to
   */
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
    
    // Get the parameter processors from the YAMLParameterSpace
    if (!(parameterSpace instanceof YAMLParameterSpace)) {
      System.err.println("Error: Parameter space must be an instance of YAMLParameterSpace");
      return;
    }
    YAMLParameterSpace yamlParameterSpace = (YAMLParameterSpace) parameterSpace;
    Map<String, ParameterProcessor> parameterProcessors = yamlParameterSpace.getParameterProcessors();
    
    // Process each sub-parameter and add it to the parameter space
    for (Map.Entry<String, Object> entry : subParams.entrySet()) {
      String subParamName = entry.getKey();
      Object subParamConfig = entry.getValue();
      
      if (!(subParamConfig instanceof Map)) {
        System.err.println("Warning: Sub-parameter configuration for " + subParamName + " should be a map");
        continue;
      }
      
      try {
        Map<String, Object> subParamConfigMap = (Map<String, Object>) subParamConfig;
        
        // Get the parameter type
        String paramType = (String) subParamConfigMap.get("type");
        if (paramType == null) {
          System.err.println("Warning: No type specified for sub-parameter " + subParamName);
          continue;
        }
        
        // Get the appropriate processor for this parameter type
        ParameterProcessor processor = parameterProcessors.get(paramType);
        if (processor == null) {
          System.err.println("Warning: No processor found for type " + paramType + " for parameter " + subParamName);
          continue;
        }
        
        // Process the parameter with the appropriate processor
        System.out.println("    - Processing global sub-parameter " + subParamName + " as type " + paramType);
        processor.process(subParamName, subParamConfig, parameterSpace);
        
        // Get the parent and sub-parameter from the parameter space
        Parameter<?> parentParameter = parameterSpace.get(parameterName);
        Parameter<?> subParameter = parameterSpace.get(subParamName);
        
        if (subParameter == null) {
          System.err.println("Warning: Failed to create sub-parameter " + subParamName);
          continue;
        }
        
        // Add the sub-parameter to the parent's global sub-parameters
        if (parentParameter instanceof CategoricalParameter) {
          ((CategoricalParameter) parentParameter).addGlobalSubParameter(subParameter);
          System.out.println("      Added as global sub-parameter " + subParamName + " to " + parameterName);
        } else if (parentParameter instanceof CategoricalIntegerParameter) {
          ((CategoricalIntegerParameter) parentParameter).addGlobalSubParameter(subParameter);
          System.out.println("      Added as global sub-parameter " + subParamName + " to " + parameterName);
        } else {
          System.err.println("Warning: Cannot add global sub-parameter to non-categorical parameter: " + parameterName);
        }
      } catch (Exception e) {
        System.err.println("Error processing global sub-parameter " + subParamName + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
