package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes double/real parameters from YAML configuration.
 */
public class DoubleParameterProcessor implements ParameterProcessor {
  @Override
  public void process(String parameterName, Object parameterValues, ParameterSpace parameterSpace) {
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
