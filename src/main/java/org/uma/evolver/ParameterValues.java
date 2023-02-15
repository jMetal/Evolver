package org.uma.evolver;

import java.util.List;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.P;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.IntegerParameter;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.auto.parameter.RealParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import scala.annotation.meta.param;

public class ParameterValues {


  public static String decodeParameter(Parameter<?> parameter, double value) {
    String result = "";
    if (parameter instanceof CategoricalParameter) {
      CategoricalParameter categoricalParameter = (CategoricalParameter) parameter;
      var index = (int)Math.floor(value * categoricalParameter.validValues().size());
      result = categoricalParameter.validValues().get(index) ;
    } else if (parameter instanceof RealParameter) {
      RealParameter realParameter = (RealParameter) parameter;
      double min = realParameter.validValues().get(0);
      double max = realParameter.validValues().get(1);
      double decodedValue = min + value * (max - min) ;
      return "" + decodedValue ;
    } else if (parameter instanceof IntegerParameter) {
      IntegerParameter integerParameter = (IntegerParameter) parameter;
      int min = integerParameter.validValues().get(0);
      int max = integerParameter.validValues().get(1);
      int decodedValue = min + (int) Math.floor(value * (max - min)) ;
      return "" + decodedValue;
    } else {
      throw new JMetalException("The parameter is non-configurable: " + parameter.name()) ;
    }

    return result;
  }

  public static String decodeCategoricalParameter(List<String> values, double probability) {
    int index = (int) Math.floor(probability * values.size());
    return values.get(index);
  }

  public static double decodeDoubleParameter(List<Double> values, double probability) {
    double min = values.get(0);
    double max = values.get(values.size() - 1);
    return min + probability * (max - min);
  }

  public static int decodeIntegerParameter(List<Integer> values, double probability) {
    int min = values.get(0);
    int max = values.get(values.size() - 1) + 1; // Non-inclusive
    return min + (int) Math.floor(probability * (max - min));
  }

  public static List<Integer> offspringPopulationSize = List.of(
      1, 200
  );

  public static List<Integer> tournamentSize = List.of(
      2, 8
  );
  public static List<Double> sbxCrossoverDistributionIndexRange = List.of(
      5.0, 400.0
  );

  public static List<Double> blxAlphaCrossoverValueRange = List.of(
      0.0, 1.0
  );

  public static List<Double> polynomialMutationDistributionIndexRange = List.of(
      5.0, 400.0
  );

  public static List<Double> linkedPolynomialMutationDistributionIndexRange = List.of(
      5.0, 400.0
  );
  public static List<Double> uniformMutationPerturbationRange = List.of(
      0.0, 1.0
  );
  public static List<Double> nonUniformMutationPerturbationRange = List.of(
      0.0, 1.0
  );
  public static List<String> createInitialSolutions = List.of(
      "random", "latinHypercubeSampling", "scatterSearch"
  );

  public static List<String> crossover = List.of(
      "SBX", "BLX_ALPHA", "wholeArithmetic"
  );

  public static List<String> mutation = List.of(
      "uniform", "polynomial", "linkedPolynomial", "nonUniform"
  );


}
