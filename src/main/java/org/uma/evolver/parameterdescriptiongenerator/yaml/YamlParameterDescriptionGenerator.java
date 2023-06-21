package org.uma.evolver.parameterdescriptiongenerator.yaml;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.impl.BooleanParameter;
import org.uma.evolver.parameter.impl.CategoricalIntegerParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.evolver.parameter.impl.IntegerParameter;
import org.uma.evolver.parameter.impl.OrdinalParameter;
import org.uma.evolver.parameter.impl.RealParameter;


public class YamlParameterDescriptionGenerator {

  private static String formatString = "%-40s %-40s %-7s %-30s %-20s\n";

  public String generateConfiguration(ConfigurableAlgorithmBuilder autoConfigurableAlgorithm) {
    List<Parameter<?>> parameterList = autoConfigurableAlgorithm.configurableParameterList();

    StringBuilder parameterStringBuilder = new StringBuilder();
    int tabSize = 0;

    for (Parameter<?> parameter : parameterList) {
      parameterStringBuilder = decodeParameter(parameter, parameterStringBuilder, tabSize);
      parameterStringBuilder.append("#\n");
    }

    return parameterStringBuilder.toString();
  }

  private StringBuilder decodeParameter(Parameter<?> parameter, StringBuilder stringBuilder,
      int tabSize) {
    printName(parameter, stringBuilder, tabSize);
    printType(parameter, stringBuilder, tabSize);

    stringBuilder.append(decodeValidValuesV2(parameter, tabSize));

    return stringBuilder;
  }

  private void printType(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    stringBuilder.append(
        spaces(tabSize) + "type: " + decodeType(parameter) + "\n"
    );
  }

  private void printName(Parameter<?> parameter, StringBuilder stringBuilder, int tabSize) {
    stringBuilder.append(spaces(tabSize) + "name: " + parameter.name()).append("\n");
  }


  String spaces(int length) {
    String spaces = "";
    for (int i = 0; i < length; i++) {
      spaces += " ";
    }

    return spaces;
  }

  private String decodeValidValuesV2(Parameter<?> parameter, int tabSize) {
    StringBuilder result = new StringBuilder();

    if (parameter instanceof CategoricalParameter) {
      result.append(spaces(tabSize) + "values: \n");
      List<String> validValues = ((CategoricalParameter) parameter).validValues();
      for (String value : validValues) {
        result.append(spaces(tabSize));
        result.append(" - " + value + "\n");
        var specificParameters = parameter.findSpecificParameters(value);
        if (!specificParameters.isEmpty()) {
          for (Parameter<?> param : specificParameters) {
            result.append(spaces(tabSize + 6) + "specific_parameter: \n");
            result.append(decodeParameter(param, new StringBuilder(), tabSize + 9));
          }
        }
      }
    } else  if (parameter instanceof CategoricalIntegerParameter) {
      result.append(spaces(tabSize) + "values: \n");
      List<Integer> validValues = ((CategoricalIntegerParameter) parameter).validValues();
      for (int value : validValues) {
        result.append(spaces(tabSize));
        result.append(" - " + value + "\n");
      }
    } else if (parameter instanceof IntegerParameter) {
      result.append(spaces(tabSize) + "values: ");
      result.append(((IntegerParameter) parameter).validValues() + "\n");
    } else if (parameter instanceof RealParameter) {
      result.append(spaces(tabSize) + "values: ");
      result.append(((RealParameter) parameter).validValues() + "\n");
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
      result = "c";
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = "c";
    } else if (parameter instanceof BooleanParameter) {
      result = "c";
    } else if (parameter instanceof OrdinalParameter) {
      result = "o";
    } else if (parameter instanceof IntegerParameter) {
      result = "i";
    } else if (parameter instanceof RealParameter) {
      result = "r";
    } else if (parameter != null) {
      result = "o";
    }

    return result;
  }

  private String decodeValidValues(Parameter<?> parameter) {
    String result = " ";

    if (parameter instanceof CategoricalParameter) {
      result = ((CategoricalParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = ((CategoricalIntegerParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof BooleanParameter) {
      result = ((BooleanParameter) parameter).getValidValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof OrdinalParameter) {
      result = ((OrdinalParameter<?>) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof IntegerParameter) {
      result = ((IntegerParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof RealParameter) {
      result = ((RealParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter != null) {
      result = "(" + parameter.value() + ")";
    }

    return result;
  }
}
