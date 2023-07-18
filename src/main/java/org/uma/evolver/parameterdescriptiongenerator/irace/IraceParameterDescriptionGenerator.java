package org.uma.evolver.parameterdescriptiongenerator.irace;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.impl.BooleanParameter;
import org.uma.evolver.parameter.impl.CategoricalIntegerParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.evolver.parameter.impl.IntegerParameter;
import org.uma.evolver.parameter.impl.OrdinalParameter;
import org.uma.evolver.parameter.impl.RealParameter;


public class IraceParameterDescriptionGenerator {
  private static String formatString = "%-40s %-40s %-7s %-30s %-20s\n";

  public void generateConfigurationFile(ConfigurableAlgorithmBuilder autoConfigurableAlgorithm) {
    List<Parameter<?>> parameterList = autoConfigurableAlgorithm.configurableParameterList() ;

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

    for (Parameter<?> globalParameter : parameter.globalParameters()) {
      decodeParameterGlobal(globalParameter, stringBuilder, parameter);
    }

    for (Pair<String, Parameter<?>> specificParameter : parameter.specificParameters()) {
      decodeParameterSpecific(specificParameter, stringBuilder, parameter);
    }
  }

  private void decodeParameterGlobal(Parameter<?> parameter, StringBuilder stringBuilder, Parameter<?> parentParameter) {
    StringBuilder dependenceString = new StringBuilder("\"" + parameter.name() + "\"");
    if (parentParameter instanceof CategoricalParameter) {
      var validValues = ((CategoricalParameter) parentParameter).validValues();
      dependenceString = new StringBuilder();
      for (String value : validValues) {
        dependenceString.append("\"").append(value).append("\"").append(",");
      }
      dependenceString = new StringBuilder(dependenceString.substring(0, dependenceString.length() - 1));
    }

    stringBuilder.append(
            String.format(
                    formatString,
                    parameter.name(),
                    "\"" + "--" + parameter.name() + " \"",
                    decodeType(parameter),
                    decodeValidValues(parameter),
                    "| " + parentParameter.name() + " %in% c(" + dependenceString + ")"));

    for (Parameter<?> globalParameter : parameter.globalParameters()) {
      decodeParameterGlobal(globalParameter, stringBuilder, parameter);
    }

    for (Pair<String, Parameter<?>> specificParameter : parameter.specificParameters()) {
      decodeParameterSpecific(specificParameter, stringBuilder, parameter);
    }
  }


  private void decodeParameterSpecific(
          Pair<String, Parameter<?>> pair, StringBuilder stringBuilder, Parameter<?> parentParameter) {
    stringBuilder.append(
            String.format(
                    formatString,
                    pair.getRight().name(),
                    "\"" + "--" + pair.getRight().name() + " \"",
                    decodeType(pair.getRight()),
                    decodeValidValues(pair.getRight()),
                    "| " + parentParameter.name() + " %in% c(\"" + pair.getLeft() + "\")"));

    for (Parameter<?> globalParameter : pair.getValue().globalParameters()) {
      decodeParameterGlobal(globalParameter, stringBuilder, pair.getValue());
    }

    for (Pair<String, Parameter<?>> specificParameter : pair.getValue().specificParameters()) {
      decodeParameterSpecific(specificParameter, stringBuilder, pair.getValue());
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
    } else     if (parameter instanceof CategoricalIntegerParameter) {
      result = ((CategoricalIntegerParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof BooleanParameter) {
      result = ((BooleanParameter) parameter).validValues().toString();
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
