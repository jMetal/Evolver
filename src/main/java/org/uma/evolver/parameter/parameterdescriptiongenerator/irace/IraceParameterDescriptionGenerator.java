package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;

import java.util.List;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.SpecificSubParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;

public class IraceParameterDescriptionGenerator<S extends Solution<?>> {

  private static String formatString = "%-40s %-40s %-7s %-30s %-20s\n";

  public void generateConfigurationFile(ParameterSpace parameterSpace) {
    List<Parameter<?>> parameterList = parameterSpace.topLevelParameters();

    StringBuilder stringBuilder = new StringBuilder();

    for (Parameter<?> parameter : parameterList) {
      decodeParameter(parameter, stringBuilder);
      stringBuilder.append("#\n");
    }

    System.out.println(stringBuilder);
  }

  private void decodeParameter(Parameter<?> parameter, StringBuilder stringBuilder) {
    stringBuilder.append(
        String.format(
            formatString,
            parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            ""));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (SpecificSubParameter<?> specificParameter : parameter.specificSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }

  private void decodeGlobalParameter(Parameter<?> parameter, StringBuilder stringBuilder,
                                     Parameter<?> parentParameter) {
    StringBuilder dependenceString = new StringBuilder("\"" + parameter.name() + "\"");
    if (parentParameter instanceof CategoricalParameter) {
      var validValues = ((CategoricalParameter) parentParameter).validValues();
      dependenceString = new StringBuilder();
      for (String value : validValues) {
        dependenceString.append("\"").append(value).append("\"").append(",");
      }
      dependenceString = new StringBuilder(
          dependenceString.substring(0, dependenceString.length() - 1));
    }

    stringBuilder.append(
        String.format(
            formatString,
            parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            "| " + parentParameter.name() + " %in% c(" + dependenceString + ")"));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (SpecificSubParameter<?> specificParameter : parameter.specificSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }


  private void decodeSpecificParameter(
      SpecificSubParameter<?> subParameter, StringBuilder stringBuilder, Parameter<?> parentParameter) {
    stringBuilder.append(
        String.format(
            formatString,
            subParameter.parameter().name(),
            "\"" + "--" + subParameter.parameter().name() + " \"",
            decodeType(subParameter.parameter()),
            decodeValidValues(subParameter.parameter()),
            "| " + parentParameter.name() + " %in% c(\"" + subParameter.description() + "\")"));

    for (Parameter<?> globalParameter : subParameter.parameter().
            globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, subParameter.parameter());
    }

    for (SpecificSubParameter<?> specificParameter : subParameter.parameter().specificSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, subParameter.parameter());
    }
  }

  private String decodeType(Parameter<?> parameter) {
    String result = " ";
    if (parameter instanceof CategoricalParameter) {
      result = "c";
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = "c";
    } else if (parameter instanceof BooleanParameter) {
      result = "c";
    } else if (parameter instanceof IntegerParameter) {
      result = "i";
    } else if (parameter instanceof DoubleParameter) {
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
      result = List.of(true, false).toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof IntegerParameter) {
      result = "[" +((IntegerParameter) parameter).minValue() + " , "
          + ((IntegerParameter) parameter).maxValue() + "]";
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof DoubleParameter) {
      result = "[" +((DoubleParameter) parameter).minValue() + " , "
          + ((DoubleParameter) parameter).maxValue() + "]";
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter != null) {
      result = "(" + parameter.value() + ")";
    }

    return result;
  }
}
