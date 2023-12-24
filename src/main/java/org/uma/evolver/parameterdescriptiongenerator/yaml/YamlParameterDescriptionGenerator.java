package org.uma.evolver.parameterdescriptiongenerator.yaml;

import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.impl.BooleanParameter;
import org.uma.evolver.parameter.impl.CategoricalIntegerParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.evolver.parameter.impl.IntegerParameter;
import org.uma.evolver.parameter.impl.OrdinalParameter;
import org.uma.evolver.parameter.impl.RealParameter;


public class YamlParameterDescriptionGenerator {

  public String generateConfiguration(ConfigurableAlgorithmBuilder autoConfigurableAlgorithm) {
    List<Parameter<?>> parameterList = autoConfigurableAlgorithm.configurableParameterList();

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
    var globalParameters = parameter.globalParameters();
    if (!globalParameters.isEmpty()) {
      stringBuilder.append(spaces(tabSize) + "global_parameters: \n");
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
    if (isList)
      stringBuilder.append("- ");
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
        result.append("- " + value + ":\n");
        var specificParameters = parameter.findSpecificParameters(value);
        if (!specificParameters.isEmpty()) {
          result.append(spaces(tabSize + 6) + "specific_parameter: \n");
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
      result.append(spaces(tabSize) + "values: ");
      result.append(((IntegerParameter) parameter).validValues() + "\n");
    } else if (parameter instanceof RealParameter) {
      result.append(spaces(tabSize) + "values: ");
      result.append(((RealParameter) parameter).validValues() + "\n");
    } else if (parameter instanceof BooleanParameter) {
      result.append(spaces(tabSize) + "values: ");
      result.append(((BooleanParameter) parameter).validValues() + "\n");
    }
    /*else if (parameter instanceof BooleanParameter) {
      result = ((BooleanParameter) parameter).getValidValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof OrdinalParameter) {
      result = ((OrdinalParameter<?>) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter != null) {
      result = "(" + parameter.value() + ")";
    }
*/

    return result.toString();
  }

  private String decodeType(Parameter<?> parameter) {
    String result = " ";
    if (parameter instanceof CategoricalParameter) {
      result = "categorical";
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = "categorical";
    } else if (parameter instanceof BooleanParameter) {
      result = "categorical";
    } else if (parameter instanceof OrdinalParameter) {
      result = "ordinal";
    } else if (parameter instanceof IntegerParameter) {
      result = "integer";
    } else if (parameter instanceof RealParameter) {
      result = "real";
    } else if (parameter != null) {
      result = "ordinal";
    }

    return result;
  }
}
