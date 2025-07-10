package org.uma.evolver.parameter.yaml.processors;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.yaml.ParameterProcessor;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes categorical parameters from YAML configuration.
 */
public class CategoricalParameterProcessor implements ParameterProcessor {
  @Override
  public void process(String parameterName, Object parameterValues, ParameterSpace parameterSpace) {
    List<String> stringCategories = new ArrayList<>();
    List<Integer> numericCategories = new ArrayList<>();

    // Handle array-style values: [val1, val2, ...]
    if (parameterValues instanceof List) {
      List<?> categoryList = (List<?>) parameterValues;

      if (categoryList.isEmpty()) {
        System.out.println("  - Creating empty categorical parameter");
        parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
        return;
      }

      // Check if values are numeric
      if (categoryList.get(0) instanceof Number) {
        for (Object category : categoryList) {
          if (category instanceof Number) {
            numericCategories.add(((Number) category).intValue());
          }
        }
        System.out.println("  - Creating integer categorical parameter with values: " + numericCategories);
        parameterSpace.put(new CategoricalIntegerParameter(parameterName, numericCategories));
      }
      // Handle string values
      else {
        for (Object category : categoryList) {
          stringCategories.add(category.toString());
        }
        System.out.println(
            "  - Creating string categorical parameter with values: " + stringCategories);
        parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
      }
    }
    // Handle map-style values: {value1: {...}, value2: {...}, ...}
    else if (parameterValues instanceof Map) {
      Map<String, Object> categoryMap = (Map<String, Object>) parameterValues;
      stringCategories.addAll(categoryMap.keySet());
      System.out.println("  - Creating categorical parameter from map keys: " + stringCategories);
      parameterSpace.put(new CategoricalParameter(parameterName, stringCategories));
    } else {
      throw new JMetalException("Unsupported values format for parameter " + parameterName);
    }
  }
}
