package org.uma.evolver.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.uma.jmetal.auto.parameter.BooleanParameter;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.IntegerParameter;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.auto.parameter.RealParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class ParameterManagement {

  public static String decodeParameter(Parameter<?> parameter, double value) {
    Check.valueIsInRange(value, 0.0, 1.0);
    String result;
    if (parameter instanceof CategoricalParameter) {
      value = Math.min(value, 0.999999999999) ;
      CategoricalParameter categoricalParameter = (CategoricalParameter) parameter;
      var index = (int) Math.floor(value * categoricalParameter.validValues().size());
      result = categoricalParameter.validValues().get(index);
    } else if (parameter instanceof RealParameter) {
      RealParameter realParameter = (RealParameter) parameter;
      double min = realParameter.validValues().get(0);
      double max = realParameter.validValues().get(1);
      double decodedValue = min + value * (max - min);
      return "" + decodedValue;
    } else if (parameter instanceof IntegerParameter) {
      IntegerParameter integerParameter = (IntegerParameter) parameter;
      int min = integerParameter.validValues().get(0);
      int max = integerParameter.validValues().get(1);
      int decodedValue = min + (int) Math.floor(value * (max - min));
      return "" + decodedValue;
    } else if (parameter instanceof BooleanParameter) {
      int min = 0;
      int max = 1;
      int decodedValue = min + (int) Math.floor(value * (max - min));
      return "" + decodedValue;
    } else {
      throw new JMetalException("The parameter is non-configurable: " + parameter.name());
    }

    return result;
  }

  public static double decodeParameterToDoubleValues(Parameter<?> parameter, double value) {
    double result;
    if (parameter instanceof CategoricalParameter) {
      CategoricalParameter categoricalParameter = (CategoricalParameter) parameter;
      result = (int) Math.floor(value * categoricalParameter.validValues().size());
    } else if (parameter instanceof RealParameter) {
      RealParameter realParameter = (RealParameter) parameter;
      double min = realParameter.validValues().get(0);
      double max = realParameter.validValues().get(1);
      return min + value * (max - min);
    } else if (parameter instanceof IntegerParameter) {
      IntegerParameter integerParameter = (IntegerParameter) parameter;
      int min = integerParameter.validValues().get(0);
      int max = integerParameter.validValues().get(1);
      return min + (int) Math.floor(value * (max - min));
    } else if (parameter instanceof BooleanParameter) {
      return value;
    } else {
      throw new JMetalException("The parameter is non-configurable: " + parameter.name());
    }

    return result;
  }

  /**
   * Given a list of parameters and a list of the corresponding encoded parameter values in the
   * range [0.0, 1.0], returns a string where each parameter is encoded as sequence of
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

  private static StringBuilder decodeParametersToDoubleValues(List<Parameter<?>> parameters,
      DoubleSolution solution) {
    StringBuilder parameterString = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++) {
      double value = decodeParameterToDoubleValues(parameters.get(i), solution.variables().get(i));

      parameterString.append(value).append(" ");
    }

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

  public static void writeDecodedSolutionsDoubleValuesFoFile(List<Parameter<?>> parameters,
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
}
