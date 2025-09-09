package org.uma.evolver.parameter.yaml.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Processes categorical parameters from YAML configuration.
 * 
 * <p>This processor handles both string and numeric categorical parameters, 
 * as well as parameters defined as maps (where keys become the categories).
 * It also supports global sub-parameters that can be associated with categories.</p>
 */
public class CategoricalParameterProcessor implements ParameterProcessor {
  private final ParameterFactory<?> parameterFactory ;

  public CategoricalParameterProcessor(ParameterFactory<?> parameterFactory) {
    this.parameterFactory = parameterFactory ;
  }
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
    
    // Process conditional parameters for each value
    processConditionalParameters(parameterName, configMap, parameterSpace);
    
    // Then process global sub-parameters if they exist
    processGlobalSubParameters(parameterName, configMap, parameterSpace);
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
   * Processes the values for a parameter from the configuration map.
   *
   * @param parameterName the name of the parameter being processed
   * @param configMap the configuration map containing the parameter values
   * @param parameterSpace the parameter space to add the parameter to
   * @throws JMetalException if the values format is unsupported or invalid
   */
  private void processParameterValues(String parameterName, Map<String, Object> configMap, ParameterSpace parameterSpace) {
    Object parameterValues = configMap.get("values");
    Check.notNull(parameterValues);
    
    if (parameterValues instanceof List) {
      processListValues(parameterName, (List<?>) parameterValues, parameterSpace);
    } else if (parameterValues instanceof Map) {
      processMapValues(parameterName, (Map<String, Object>) parameterValues, parameterSpace);
    } else {
      throw new JMetalException("Unsupported values format for parameter " + parameterName + ". Expected a list or map.");
    }
  }
  
  /**
   * Processes a list of values for a categorical parameter.
   *
   * @param parameterName the name of the parameter being processed
   * @param categoryList the list of category values (can be numbers or strings)
   * @param parameterSpace the parameter space to add the parameter to
   */
  private void processListValues(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    if (categoryList.isEmpty()) {

      parameterSpace.put(parameterFactory.createParameter(parameterName, new ArrayList<>()));
      return;
    }
    
    if (categoryList.get(0) instanceof Number) {
      processNumericCategories(parameterName, categoryList, parameterSpace);
    } else {
      processStringCategories(parameterName, categoryList, parameterSpace);
    }
  }
  
  /**
   * Processes a map of values for a categorical parameter, using the keys as categories.
   *
   * @param parameterName the name of the parameter being processed
   * @param categoryMap the map whose keys will be used as category values
   * @param parameterSpace the parameter space to add the parameter to
   */
  private void processMapValues(String parameterName, Map<String, Object> categoryMap, ParameterSpace parameterSpace) {
    List<String> stringCategories = new ArrayList<>(categoryMap.keySet());

    parameterSpace.put(parameterFactory.createParameter(parameterName, stringCategories));
  }
  
  /**
   * Processes a list of numeric values for a categorical parameter.
   *
   * @param parameterName the name of the parameter being processed
   * @param categoryList the list of numeric category values
   * @param parameterSpace the parameter space to add the parameter to
   * @throws JMetalException if any value in the list is not a number
   */
  private void processNumericCategories(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    List<Integer> numericCategories = new ArrayList<>();
    for (Object item : categoryList) {
      Check.that(item instanceof Number, 
          "All values in numeric category list must be numbers for parameter " + parameterName + ". Found: " + item);
      numericCategories.add(((Number) item).intValue());
    }

    parameterSpace.put(new CategoricalIntegerParameter(parameterName, numericCategories));
  }
  
  /**
   * Processes a list of string values for a categorical parameter.
   *
   * @param parameterName the name of the parameter being processed
   * @param categoryList the list of string category values
   * @param parameterSpace the parameter space to add the parameter to
   * @throws JMetalException if any value in the list is null
   */
  private void processStringCategories(String parameterName, List<?> categoryList, ParameterSpace parameterSpace) {
    List<String> stringCategories = new ArrayList<>();
    for (Object item : categoryList) {
      Check.notNull(item);
      stringCategories.add(item.toString());
    }

    parameterSpace.put(parameterFactory.createParameter(parameterName, stringCategories));
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
  private void processConditionalParameters(String parameterName, Map<String, Object> configMap, ParameterSpace parameterSpace) {
    // Get the values map which contains the categorical options
    Object valuesObj = configMap.get("values");
    if (!(valuesObj instanceof Map)) {
      return; // No values or values is not a map
    }
    
    Map<String, Object> valuesMap = (Map<String, Object>) valuesObj;
    
    // Check each value for conditional subparameters
    for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
      String valueName = entry.getKey();
      Object valueConfig = entry.getValue();
      
      // For the YAML structure, the values can be either a simple string or a map with subparameters
      if (valueConfig instanceof Map) {
        Map<String, Object> valueConfigMap = (Map<String, Object>) valueConfig;
        processConditionalParametersForValue(parameterName, valueName, valueConfigMap, parameterSpace);
      }
      // Handle the case where the value is a map with nested values (like in the YAML example)
      //else if (valueName.equals("externalArchive") && valueConfig instanceof Map) {
      //  Map<String, Object> externalArchiveMap = (Map<String, Object>) valueConfig;
      //  processConditionalParametersForValue(parameterName, "externalArchive", externalArchiveMap, parameterSpace);
      //}
      else if (valueConfig instanceof Map) {
        Map<String, Object> externalArchiveMap = (Map<String, Object>) valueConfig;
        processConditionalParametersForValue(parameterName, valueName, externalArchiveMap, parameterSpace);
      }
    }
  }

  /**
   * Processes conditional parameters for a specific value of a parent parameter.
   *
   * @param parentParamName the name of the parent parameter
   * @param valueName the value of the parent parameter that activates these sub-parameters
   * @param valueConfigMap the configuration map containing the conditional parameters
   * @param parameterSpace the parameter space to add parameters to
   * @throws JMetalException if the parameter space is not a YAMLParameterSpace
   */
  @SuppressWarnings("unchecked")
  private void processConditionalParametersForValue(String parentParamName, String valueName,
                                                  Map<String, Object> valueConfigMap, ParameterSpace parameterSpace) {
    // Guard clauses for preconditions
    if (!valueConfigMap.containsKey("conditionalParameters")) {
      return;
    }
    
    Object subParamsObj = valueConfigMap.get("conditionalParameters");
    if (!(subParamsObj instanceof Map)) {
      return;
    }
    
    Map<String, Object> subParams = (Map<String, Object>) subParamsObj;
    if (subParams.isEmpty()) {
      return;
    }
    
    Check.that(parameterSpace instanceof YAMLParameterSpace, 
        "Parameter space must be an instance of YAMLParameterSpace");
    
    // Main processing logic
    YAMLParameterSpace yamlParameterSpace = (YAMLParameterSpace) parameterSpace;
    processSubParameters(parentParamName, valueName, subParams, yamlParameterSpace);
  }
  
  /**
   * Processes a collection of sub-parameters for a specific parameter value.
   *
   * @param parentParamName the name of the parent parameter
   * @param valueName the value of the parent parameter that activates these sub-parameters
   * @param subParams the map of sub-parameter configurations to process
   * @param yamlParameterSpace the YAML parameter space to add parameters to
   */
  private void processSubParameters(String parentParamName, String valueName,
                                  Map<String, Object> subParams,
                                  YAMLParameterSpace yamlParameterSpace) {
    for (Map.Entry<String, Object> subParamEntry : subParams.entrySet()) {
      processSingleConditionalParameter(
          parentParamName, 
          valueName, 
          subParamEntry.getKey(), 
          subParamEntry.getValue(), 
          yamlParameterSpace
      );
    }
  }
  
  /**
   * Processes a single conditional sub-parameter.
   *
   * @param parentParamName the name of the parent parameter
   * @param valueName the value of the parent parameter that activates this sub-parameter
   * @param subParamName the name of the sub-parameter
   * @param subParamConfig the configuration object for the sub-parameter
   * @param yamlParameterSpace the YAML parameter space to add the parameter to
   * @throws JMetalException if the sub-parameter configuration is invalid or processing fails
   */
  @SuppressWarnings("unchecked")
  private void processSingleConditionalParameter(String parentParamName, String valueName,
                                               String subParamName, Object subParamConfig,
                                               YAMLParameterSpace yamlParameterSpace) {
    Check.that(subParamConfig instanceof Map, 
        "Sub-parameter configuration for " + subParamName + " should be a map");
    
    Map<String, Object> subParamConfigMap = (Map<String, Object>) subParamConfig;
    String subParamType = subParamConfigMap.getOrDefault("type", "unknown").toString();
    ParameterProcessor processor = yamlParameterSpace.getParameterProcessor(subParamType);
    
    Check.notNull(processor);
    
    try {
      // Process the subparameter with just its base name
      processor.process(subParamName, subParamConfigMap, yamlParameterSpace);
      
      // Get the parent parameter and add this as a conditional parameter
      Parameter<?> parentParam = yamlParameterSpace.get(parentParamName);
      Parameter<?> subParam = parentParam != null ? yamlParameterSpace.get(subParamName) : null;
      
      if (parentParam != null && subParam != null) {
        parentParam.addConditionalParameter(valueName, subParam);
      }
    } catch (Exception e) {
      throw new JMetalException("Error processing specific subparameter " + subParamName + ": " + e.getMessage(), e);
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
    Check.that(subParamsObj instanceof Map, 
        "globalSubParameters for " + parameterName + " should be a map");
    
    Map<String, Object> subParams = (Map<String, Object>) subParamsObj;
    if (subParams.isEmpty()) {
      return;
    }
    
    if (!(parameterSpace instanceof YAMLParameterSpace yamlParameterSpace)) {
      throw new JMetalException("Parameter space must be an instance of YAMLParameterSpace");
    }

    Map<String, ParameterProcessor> parameterProcessors = yamlParameterSpace.getParameterProcessors();
    
    // Process each sub-parameter and add it to the parameter space
    for (Map.Entry<String, Object> entry : subParams.entrySet()) {
      String subParamName = entry.getKey();
      Object subParamConfig = entry.getValue();
      
      Check.that(subParamConfig instanceof Map, 
          "Sub-parameter configuration for " + subParamName + " should be a map");
      
      try {
        Map<String, Object> subParamConfigMap = (Map<String, Object>) subParamConfig;
        
        // Get the parameter type
        String paramType = (String) subParamConfigMap.get("type");
        Check.that(paramType != null, "No type specified for sub-parameter " + subParamName);
        
        // Get the appropriate processor for this parameter type
        ParameterProcessor processor = parameterProcessors.get(paramType);
        Check.that(processor != null, 
            "No processor found for type " + paramType + " for parameter " + subParamName);
        
        // Process the parameter with the appropriate processor
        processor.process(subParamName, subParamConfig, parameterSpace);
        
        // Get the parent and sub-parameter from the parameter space
        Parameter<?> parentParameter = parameterSpace.get(parameterName);
        Parameter<?> subParameter = parameterSpace.get(subParamName);
        
        Check.that(subParameter != null, "Failed to create sub-parameter " + subParamName);
        
        // Add the sub-parameter to the parent's global sub-parameters
        Check.that(parentParameter instanceof CategoricalParameter, 
            "Cannot add global sub-parameter to non-categorical parameter: " + parameterName);
        ((CategoricalParameter) parentParameter).addGlobalSubParameter(subParameter);
      } catch (Exception e) {
        throw new JMetalException("Error processing global sub-parameter " + subParamName + ": " + e.getMessage(), e);
      }
    }
  }
}
