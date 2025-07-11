package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;

import java.util.List;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.ConditionalSubParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A generator for creating parameter description files in the irace configuration format.
 * 
 * <p>This class is responsible for converting a {@link ParameterSpace} into a string
 * representation that follows the irace parameter configuration format. It handles different
 * types of parameters and their relationships (global and specific sub-parameters).
 *
 * <p>Example usage:
 * <pre>{@code
 * IraceParameterDescriptionGenerator<Solution<?>> generator = new IraceParameterDescriptionGenerator<>();
 * ParameterSpace parameterSpace = new MyParameterSpace();
 * generator.generateConfigurationFile(parameterSpace);
 * }</pre>
 *
 * @param <S> The type of solution the parameters are associated with
 * @author Antonio J. Nebro
 */
public class IraceParameterDescriptionGenerator<S extends Solution<?>> {

  /** Format string for parameter output alignment */
  private static final String FORMAT_STRING = "%-40s %-40s %-7s %-30s %-20s\n";

  /**
   * Generates and prints the irace configuration for the given parameter space.
   *
   * @param parameterSpace The parameter space to generate configuration for
   * @throws NullPointerException if parameterSpace is null
   */
  public void generateConfigurationFile(ParameterSpace parameterSpace) {
    List<Parameter<?>> parameterList = parameterSpace.topLevelParameters();

    StringBuilder stringBuilder = new StringBuilder();

    for (Parameter<?> parameter : parameterList) {
      decodeParameter(parameter, stringBuilder);
      stringBuilder.append("#\n");
    }

    System.out.println(stringBuilder);
  }

  /**
   * Decodes a single parameter and appends its irace configuration to the string builder.
   *
   * @param parameter The parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @throws NullPointerException if either parameter or stringBuilder is null
   */
  private void decodeParameter(Parameter<?> parameter, StringBuilder stringBuilder) {
    stringBuilder.append(
        String.format(
            FORMAT_STRING,
            parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            ""));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (ConditionalSubParameter<?> specificParameter : parameter.conditionalSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }

  /**
   * Decodes a global sub-parameter and appends its irace configuration to the string builder.
   *
   * <p>This method handles the decoding of global sub-parameters, including their dependencies on
   * parent parameters.
   *
   * @param parameter The global sub-parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @param parentParameter The parent parameter of this sub-parameter
   * @throws NullPointerException if any parameter is null
   */
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
            FORMAT_STRING, parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            "| " + parentParameter.name() + " %in% c(" + dependenceString + ")"));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (ConditionalSubParameter<?> specificParameter : parameter.conditionalSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }


  /**
   * Decodes a specific sub-parameter and appends its irace configuration to the string builder.
   *
   * @param subParameter The specific sub-parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @param parentParameter The parent parameter of this sub-parameter
   * @throws NullPointerException if any parameter is null
   */
  private void decodeSpecificParameter(
          ConditionalSubParameter<?> subParameter, StringBuilder stringBuilder, Parameter<?> parentParameter) {
    stringBuilder.append(
        String.format(
            FORMAT_STRING,
            subParameter.parameter().name(),
            "\"" + "--" + subParameter.parameter().name() + " \"",
            decodeType(subParameter.parameter()),
            decodeValidValues(subParameter.parameter()),
            "| " + parentParameter.name() + " %in% c(\"" + subParameter.description() + "\")"));

    for (Parameter<?> globalParameter : subParameter.parameter().
            globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, subParameter.parameter());
    }

    for (ConditionalSubParameter<?> specificParameter : subParameter.parameter().conditionalSubParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, subParameter.parameter());
    }
  }

  /**
   * Determines the irace parameter type for a given parameter.
   *
   * @param parameter The parameter to get the type for
   * @return A string representing the irace parameter type
   * @throws JMetalException if the parameter type is not supported
   * @throws NullPointerException if parameter is null
   */
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

  /**
   * Generates the valid values string for a parameter in irace format.
   *
   * @param parameter The parameter to get valid values for
   * @return A string representing the valid values in irace format
   * @throws NullPointerException if parameter is null
   */
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
