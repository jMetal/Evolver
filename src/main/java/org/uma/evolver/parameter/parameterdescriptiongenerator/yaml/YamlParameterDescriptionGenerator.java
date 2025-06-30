package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class YamlParameterDescriptionGenerator<S extends Solution<?>> {

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

  private void decodeGlobalParameters(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    var globalParameters = parameter.globalSubParameters();
    if (!globalParameters.isEmpty()) {
      stringBuilder.append(spaces(tabSize) + "global_subparameters: \n");
      for (Parameter<?> param : globalParameters) {
        stringBuilder.append(decodeParameter(param, new StringBuilder(), tabSize +2, true));
      }
    }
  }

  private void printType(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    stringBuilder.append(
            spaces(tabSize) + "type: " + decodeType(parameter) + "\n"
    );
  }

  private void printName(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize, boolean isList) {
    stringBuilder.append(spaces(tabSize));
    stringBuilder.append(parameter.name()).append(":\n");
  }


  String spaces(int length) {
    String spaces = "";
    for (int i = 0; i < length; i++) {
      spaces += " ";
    }

    return spaces;
  }

  private String decodeValidValues(Parameter<?> parameter, int tabSize) {
    StringBuilder result = new StringBuilder();

    if (parameter instanceof CategoricalParameter) {
      result.append(spaces(tabSize) + "values: \n");
      List<String> validValues = ((CategoricalParameter) parameter).validValues();
      for (String value : validValues) {
        result.append(spaces(tabSize + 2));
        result.append(value + ":\n");
        var specificParameters = parameter.findSpecificSubParameters(value);
        if (!specificParameters.isEmpty()) {
          result.append(spaces(tabSize + 6) + "specific_subparameters: \n");
          for (Parameter<?> param : specificParameters) {
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
        var specificParameters = parameter.findSpecificSubParameters("" + value);
        if (!specificParameters.isEmpty()) {
          result.append(spaces(tabSize + 6) + "specific_subparameter: \n");
          for (Parameter<?> param : specificParameters) {
            result.append(decodeParameter(param, new StringBuilder(), tabSize + 8, true));
          }
        }
      }
    }

    return result.toString();
  }

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
