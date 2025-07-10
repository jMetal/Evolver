package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
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
    Object parameterValues = configMap.get("values");
    
    if (parameterValues == null) {
      throw new JMetalException("No values defined for parameter " + parameterName);
    }
    
    if (parameterValues instanceof List) {
      List<?> valueList = (List<?>) parameterValues;

      if (valueList.size() == 2 && valueList.get(0) instanceof Number) {
        // Treat as [min, max] range
        double minValue = ((Number) valueList.get(0)).doubleValue();
        double maxValue = ((Number) valueList.get(1)).doubleValue();
        System.out.println("  - Creating double parameter with range: [" + minValue + ", " + maxValue + "]");
        parameterSpace.put(new DoubleParameter(parameterName, minValue, maxValue));
      } else {
        // Handle as discrete values
        List<Double> discreteValues = new ArrayList<>();
        List<String> stringCategories = new ArrayList<>();
        for (Object value : valueList) {
          if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();
            discreteValues.add(doubleValue);
            stringCategories.add(String.valueOf(doubleValue));
          }
        }
        if (!discreteValues.isEmpty()) {
          System.out.println("  - Creating double categorical parameter with values: " + discreteValues);
          parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
        } else {
          throw new JMetalException("No valid double values found for parameter " + parameterName);
        }
      }
    } else {
      throw new JMetalException("Unsupported values format for double parameter " + parameterName);
    }
  }
}
