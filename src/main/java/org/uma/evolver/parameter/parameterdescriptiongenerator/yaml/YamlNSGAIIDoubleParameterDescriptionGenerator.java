package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for class {@link.EvNSGAIIDouble}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlNSGAIIDoubleParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new NSGAIIDoubleParameterSpace());
    System.out.println(parameterString);
  }
}
