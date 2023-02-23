package org.uma.evolver.util;

import java.util.List;
import org.uma.jmetal.auto.parameter.BooleanParameter;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.IntegerParameter;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.auto.parameter.RealParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class ParameterValues {

  public static String decodeParameter(Parameter<?> parameter, double value) {
    String result = "";
    if (parameter instanceof CategoricalParameter) {
      CategoricalParameter categoricalParameter = (CategoricalParameter) parameter;
      var index = (int) Math.floor(value * categoricalParameter.validValues().size());
      if ((index == 2) && (categoricalParameter.validValues().size() <= 2)) {
        int a = 0;
      }
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
}
