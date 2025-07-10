package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes integer parameters from YAML configuration.
 */
public class IntegerParameterProcessor implements ParameterProcessor {
  @Override
  public void process(String parameterName, Object parameterValues, ParameterSpace parameterSpace) {
    if (parameterValues instanceof List) {
      List<?> valueList = (List<?>) parameterValues;

      if (valueList.size() == 2 && valueList.get(0) instanceof Number) {
        // Treat as [min, max] range
        int minValue = ((Number) valueList.get(0)).intValue();
        int maxValue = ((Number) valueList.get(1)).intValue();
        System.out.println("  - Creating integer parameter with range: [" + minValue + ", " + maxValue + "]");
        parameterSpace.put(new IntegerParameter(parameterName, minValue, maxValue));
      } else {
        // Handle as discrete values
        if (valueList.size() < 2) {
          throw new JMetalException("At least two values are required for categorical parameter " + parameterName);
        }
        
        List<Integer> discreteValues = new ArrayList<>();
        for (Object value : valueList) {
          if (value instanceof Number) {
            discreteValues.add(((Number) value).intValue());
          }
        }
        if (!discreteValues.isEmpty()) {
          if (discreteValues.size() < 2) {
            throw new JMetalException("At least two valid integer values are required for parameter " + parameterName);
          }
          System.out.println("  - Creating integer categorical parameter with values: " + discreteValues);
          parameterSpace.put(new CategoricalIntegerParameter(parameterName, discreteValues));
        } else {
          throw new JMetalException("No valid integer values found for parameter " + parameterName);
        }
      }
    } else {
      throw new JMetalException("Unsupported values format for integer parameter " + parameterName);
    }
  }
}
