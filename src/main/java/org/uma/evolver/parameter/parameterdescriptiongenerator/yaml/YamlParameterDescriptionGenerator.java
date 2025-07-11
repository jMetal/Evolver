package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A generator for creating parameter description files in YAML format.
 *
 * <p>This class is responsible for converting a {@link ParameterSpace} into a YAML string
 * representation. It handles different types of parameters and their relationships (global and
 * specific sub-parameters) in a hierarchical YAML structure.
 *
 * <p>Example usage:
 * <pre>{@code
 * YamlParameterDescriptionGenerator&lt;Solution&lt;?&gt;&gt; generator = new YamlParameterDescriptionGenerator<>();
 * ParameterSpace parameterSpace = new MyParameterSpace();
 * String yamlConfig = generator.generateConfiguration(parameterSpace);
 * }</pre>
 *
 * @param <S> The type of solution the parameters are associated with
 * @author Antonio J. Nebro
 */
public class YamlParameterDescriptionGenerator<S extends Solution<?>> {

  /**
   * Generates a YAML configuration string from the given parameter space.
   *
   * @param parameterSpace The parameter space to generate configuration for
   * @return A YAML formatted string representing the parameter space
   * @throws NullPointerException if parameterSpace is null
   */
  public String generateConfiguration(ParameterSpace parameterSpace) {
    List<Parameter<?>> parameterList = parameterSpace.topLevelParameters() ;

    StringBuilder parameterStringBuilder = new StringBuilder();
    int tabSize = 0;

    for (Parameter<?> parameter : parameterList) {
      parameterStringBuilder = decodeParameter(parameter, parameterStringBuilder, tabSize, false);
      parameterStringBuilder.append("#\n");
    }

    return parameterStringBuilder.toString();
  }

  /**
   * Recursively decodes a parameter and its sub-parameters into YAML format.
   *
   * @param parameter The parameter to decode
   * @param stringBuilder The string builder to append the YAML to
   * @param tabSize The current indentation level
   * @param isList Whether the parameter is part of a YAML list
   * @return The updated string builder with the parameter's YAML representation
   * @throws NullPointerException if parameter or stringBuilder is null
   */
  private StringBuilder decodeParameter(Parameter<?> parameter, StringBuilder stringBuilder,
                                      int tabSize, boolean isList) {
    printName(parameter, stringBuilder, tabSize, isList);
    // If the parameter is part of a list, add 2 spaces more to offset the "- "
    tabSize += (isList?4:2);
    printType(parameter, stringBuilder, tabSize);
    decodeGlobalParameters(parameter, stringBuilder, tabSize);
    stringBuilder.append(decodeValidValues(parameter, tabSize));

    return stringBuilder;
  }

  /**
   * Decodes and appends global sub-parameters to the YAML output.
   *
   * @param parameter The parent parameter containing global sub-parameters
   * @param stringBuilder The string builder to append the YAML to
   * @param tabSize The current indentation level
   * @throws NullPointerException if any parameter is null
   */
  private void decodeGlobalParameters(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    var globalParameters = parameter.globalSubParameters();
    if (!globalParameters.isEmpty()) {
      stringBuilder.append(spaces(tabSize) + "globalSubparameters: \n");
      for (Parameter<?> param : globalParameters) {
        stringBuilder.append(decodeParameter(param, new StringBuilder(), tabSize +2, true));
      }
    }
  }

  /**
   * Appends the parameter's type to the YAML output.
   *
   * @param parameter The parameter whose type to print
   * @param stringBuilder The string builder to append to
   * @param tabSize The current indentation level
   * @throws NullPointerException if any parameter is null
   */
  private void printType(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    stringBuilder.append(
            spaces(tabSize) + "type: " + decodeType(parameter) + "\n"
    );
  }

  /**
   * Appends the parameter's name to the YAML output with proper indentation.
   *
   * @param parameter The parameter whose name to print
   * @param stringBuilder The string builder to append to
   * @param tabSize The current indentation level
   * @param isList Whether the parameter is part of a YAML list
   * @throws NullPointerException if any parameter is null
   */
  private void printName(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize, boolean isList) {
    stringBuilder.append(spaces(tabSize));
    stringBuilder.append(parameter.name()).append(":\n");
  }


  /**
   * Generates a string of spaces for YAML indentation.
   *
   * @param length The number of spaces to generate
   * @return A string containing the specified number of spaces
   * @throws IllegalArgumentException if length is negative
   */
  String spaces(int length) {
    String spaces = "";
    for (int i = 0; i < length; i++) {
      spaces += " ";
    }

    return spaces;
  }

  /**
   * Decodes the valid values of a parameter into YAML format.
   *
   * @param parameter The parameter whose valid values to decode
   * @param tabSize The current indentation level
   * @return A YAML string representing the parameter's valid values
   * @throws NullPointerException if parameter is null
   */
  private String decodeValidValues(Parameter<?> parameter, int tabSize) {
    StringBuilder result = new StringBuilder();

    if (parameter instanceof CategoricalParameter) {
      result.append(spaces(tabSize) + "values: \n");
      List<String> validValues = ((CategoricalParameter) parameter).validValues();
      for (String value : validValues) {
        result.append(spaces(tabSize + 2));
        result.append(value + ":\n");
        var conditionalSubParameters = parameter.findConditionalSubParameters(value);
        if (!conditionalSubParameters.isEmpty()) {
          result.append(spaces(tabSize + 6) + "conditionalSubparameters: \n");
          for (Parameter<?> param : conditionalSubParameters) {
            result.append(decodeParameter(param, new StringBuilder(), tabSize + 8, true));
          }
        }
      }
    } else  if (parameter instanceof CategoricalIntegerParameter) {
      result.append(spaces(tabSize) + "values: [");
      List<Integer> validValues = ((CategoricalIntegerParameter) parameter).validValues();
      String joinedValues = validValues.stream()
              .map(Object::toString)
              .collect(Collectors.joining(", "));
      result.append(joinedValues).append("]\n");

    } else if (parameter instanceof IntegerParameter) {
      result.append(spaces(tabSize) + "range: ");
      result.append("[" + ((IntegerParameter) parameter).minValue() + ", "
          + ((IntegerParameter) parameter).maxValue() + "]\n");
    } else if (parameter instanceof DoubleParameter) {
      result.append(spaces(tabSize) + "values: ");
      result.append("[" + ((DoubleParameter) parameter).minValue() + ", "
          + ((DoubleParameter) parameter).maxValue() + "]\n");
    } else if (parameter instanceof BooleanParameter) {
      result.append(spaces(tabSize) + "values: \n");
      List<Boolean> validValues = List.of(true, false) ;
      for (Boolean value : validValues) {
        result.append(spaces(tabSize + 2));
        result.append(value + ":\n");
        var conditionalSubParameters = parameter.findConditionalSubParameters("" + value);
        if (!conditionalSubParameters.isEmpty()) {
          result.append(spaces(tabSize + 6) + "conditionalSubParamaters: \n");
          for (Parameter<?> param : conditionalSubParameters) {
            result.append(decodeParameter(param, new StringBuilder(), tabSize + 8, true));
          }
        }
      }
    }

    return result.toString();
  }

  /**
   * Determines the YAML type string for a given parameter.
   *
   * @param parameter The parameter to get the type for
   * @return A string representing the parameter's type in YAML
   * @throws JMetalException if the parameter type is not supported
   * @throws NullPointerException if parameter is null
   */
  private String decodeType(Parameter<?> parameter) {
    String result = " ";
    if (parameter instanceof CategoricalParameter) {
      result = "categorical";
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = "categorical";
    } else if (parameter instanceof BooleanParameter) {
      result = "boolean";
    } else if (parameter instanceof IntegerParameter) {
      result = "integer";
    } else if (parameter instanceof DoubleParameter) {
      result = "double";
    } else if (parameter != null) {
      throw new JMetalException("The parameter type does not exist") ;
    }

    return result;
  }
}
