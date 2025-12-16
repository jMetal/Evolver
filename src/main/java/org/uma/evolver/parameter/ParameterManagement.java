package org.uma.evolver.parameter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class ParameterManagement {

  public static String decodeParameter(Parameter<?> parameter, double value) {
    Check.valueIsInRange(value, 0.0, 1.0);
    String result;
    if (parameter instanceof CategoricalParameter categoricalParameter) {
      value = Math.min(value, 0.999999999999);
      var index = (int) Math.floor(value * categoricalParameter.validValues().size());
      result = categoricalParameter.validValues().get(index);
    } else if (parameter instanceof CategoricalIntegerParameter categoricalParameter) {
      value = Math.min(value, 0.999999999999);
      var index = (int) Math.floor(value * categoricalParameter.validValues().size());
      result = String.valueOf(categoricalParameter.validValues().get(index));
    } else if (parameter instanceof DoubleParameter realParameter) {
      double min = realParameter.minValue();
      double max = realParameter.maxValue();
      double decodedValue = min + value * (max - min);
      return "" + decodedValue;
    } else if (parameter instanceof IntegerParameter integerParameter) {
      int min = integerParameter.minValue();
      int max = integerParameter.maxValue();
      int decodedValue = min + (int) Math.floor(value * (max - min));
      return "" + decodedValue;
    } else if (parameter instanceof BooleanParameter) {
      String decodedValue;
      if (value < 0.5) {
        decodedValue = "false";
      } else {
        decodedValue = "true";
      }
      return "" + decodedValue;
    } else {
      throw new JMetalException("The parameter is non-configurable: " + parameter.name());
    }

    return result;
  }

  public static double decodeParameterToDoubleValues(Parameter<?> parameter, double value) {
    double result;
    Check.valueIsInRange(value, 0.0, 1.0);
    if (parameter instanceof CategoricalParameter categoricalParameter) {
      result = (int) Math.floor(value * categoricalParameter.validValues().size());
    } else if (parameter instanceof CategoricalIntegerParameter categoricalParameter) {
      result = Math.floor(value * categoricalParameter.validValues().size());
    } else if (parameter instanceof DoubleParameter realParameter) {
      double min = realParameter.minValue();
      double max = realParameter.maxValue();
      return min + value * (max - min);
    } else if (parameter instanceof IntegerParameter integerParameter) {
      int min = integerParameter.minValue();
      int max = integerParameter.maxValue();
      return min + (int) Math.floor(value * (max - min));
    } else if (parameter instanceof BooleanParameter) {
      return value;
    } else {
      throw new JMetalException("The parameter is non-configurable: " + parameter.name());
    }

    return result;
  }

  /**
   * Given a list of parameters and a list of the corresponding encoded parameter
   * values in the
   * range [0.0, 1.0], returns a string where each parameter is encoded as
   * sequence of
   * "--parameterName parameterValue" pairs
   *
   * @param parameters List of parameters
   * @param values     List of encoded parameter values in the range [0.0, 1.0]
   * @return A {@link StringBuilder} object
   */
  public static StringBuilder decodeParametersToString(List<Parameter<?>> parameters,
      List<Double> values) {
    StringBuilder parameterString = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++) {
      String parameterName = parameters.get(i).name();
      String value = decodeParameter(parameters.get(i), values.get(i));

      parameterString.append("--").append(parameterName).append(" ").append(value).append(" ");
    }

    return parameterString;
  }

  /**
   * Given a list of parameters, their encoded values, and a set of active
   * indices,
   * returns a string containing only the active parameters as "--parameterName
   * parameterValue" pairs.
   *
   * @param parameters    List of parameters
   * @param values        List of encoded parameter values in the range [0.0, 1.0]
   * @param activeIndices Set of indices indicating which parameters are active
   * @return A {@link StringBuilder} object containing only active parameters
   */
  public static StringBuilder decodeActiveParametersToString(List<Parameter<?>> parameters,
      List<Double> values, java.util.Set<Integer> activeIndices) {
    StringBuilder parameterString = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++) {
      if (activeIndices.contains(i)) {
        String parameterName = parameters.get(i).name();
        String value = decodeParameter(parameters.get(i), values.get(i));
        parameterString.append("--").append(parameterName).append(" ").append(value).append(" ");
      }
    }
    return parameterString;
  }

  private static StringBuilder decodeParametersToDoubleValues(List<Parameter<?>> parameters,
      DoubleSolution solution) {
    StringBuilder parameterString = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++) {
      double value = decodeParameterToDoubleValues(parameters.get(i), solution.variables().get(i));
      parameterString.append(value).append(",");
    }
    parameterString.deleteCharAt(parameterString.length() - 1);

    return parameterString;
  }

  public static void writeDecodedSolutionsFoFile(List<Parameter<?>> parameters,
      List<DoubleSolution> solutions, String fileName)
      throws IOException {
    FileWriter fileWriter = new FileWriter(fileName);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    for (DoubleSolution solution : solutions) {
      StringBuilder parameterString = decodeParametersToString(parameters, solution.variables());

      printWriter.println(parameterString);
    }
    printWriter.close();
  }

  public static void writeDecodedSolutionsToDoubleValuesFile(List<Parameter<?>> parameters,
      List<DoubleSolution> solutions, String fileName)
      throws IOException {
    FileWriter fileWriter = new FileWriter(fileName);
    PrintWriter printWriter = new PrintWriter(fileWriter);

    StringBuilder csvHeader = new StringBuilder();
    for (var parameter : parameters) {
      csvHeader.append(parameter.name()).append(",");
    }
    csvHeader.deleteCharAt(csvHeader.length() - 1);
    printWriter.println(csvHeader);

    for (DoubleSolution solution : solutions) {
      StringBuilder parameterString = decodeParametersToDoubleValues(parameters, solution);

      printWriter.println(parameterString);
    }
    printWriter.close();
  }

  /**
   * Given a list of parameters, returns a list with all of them and all of their
   * sub-parameters
   *
   * @param parameters
   * @return A list of parameters
   */
  public static List<Parameter<?>> parameterFlattening(List<Parameter<?>> parameters) {
    List<Parameter<?>> parameterList = new ArrayList<>();
    parameters.forEach(parameter -> {
      parameterList.add(parameter);
      parameterList.addAll(parameterFlattening(parameter.globalSubParameters()));
      List<Parameter<?>> specificParameters = parameter.conditionalParameters().stream().map(
          ConditionalParameter::parameter).collect(
              Collectors.toList());
      parameterList.addAll(parameterFlattening(specificParameters));
    });
    return parameterList;
  }

  /**
   * Given a flattened list of parameters and their encoded values, determines
   * which parameters
   * are "active" (i.e., their parent categorical parameter's value activates
   * them).
   *
   * @param topLevelParameters  The top-level parameters from the parameter space
   * @param flattenedParameters The flattened list of all parameters
   * @param values              The encoded values [0.0, 1.0] for each flattened
   *                            parameter
   * @return A set of parameter indices that are active
   */
  public static java.util.Set<Integer> getActiveParameterIndices(
      List<Parameter<?>> topLevelParameters,
      List<Parameter<?>> flattenedParameters,
      List<Double> values) {

    java.util.Set<Integer> activeIndices = new java.util.HashSet<>();
    java.util.Map<String, String> decodedCategoricalValues = new java.util.HashMap<>();

    // First pass: decode all categorical parameter values
    for (int i = 0; i < flattenedParameters.size(); i++) {
      Parameter<?> param = flattenedParameters.get(i);
      if (param instanceof CategoricalParameter) {
        String decodedValue = decodeParameter(param, values.get(i));
        decodedCategoricalValues.put(param.name(), decodedValue);
      }
    }

    // Second pass: determine which parameters are active
    collectActiveParameters(topLevelParameters, flattenedParameters, decodedCategoricalValues, activeIndices);

    return activeIndices;
  }

  private static void collectActiveParameters(
      List<Parameter<?>> parameters,
      List<Parameter<?>> flattenedParameters,
      java.util.Map<String, String> decodedCategoricalValues,
      java.util.Set<Integer> activeIndices) {

    for (Parameter<?> param : parameters) {
      // Find index in flattened list
      int idx = flattenedParameters.indexOf(param);
      if (idx >= 0) {
        activeIndices.add(idx);
      }

      // Always add global sub-parameters
      collectActiveParameters(param.globalSubParameters(), flattenedParameters,
          decodedCategoricalValues, activeIndices);

      // For conditional parameters, only add if condition is met
      String parentValue = decodedCategoricalValues.get(param.name());
      if (parentValue != null) {
        for (ConditionalParameter<?> condParam : param.conditionalParameters()) {
          // Check if the condition description matches the parent value
          if (condParam.description().equals(parentValue)) {
            Parameter<?> childParam = condParam.parameter();
            int childIdx = flattenedParameters.indexOf(childParam);
            if (childIdx >= 0) {
              activeIndices.add(childIdx);
            }
            // Recursively process child's sub-parameters
            collectActiveParameters(List.of(childParam), flattenedParameters,
                decodedCategoricalValues, activeIndices);
          }
        }
      }
    }
  }
}
