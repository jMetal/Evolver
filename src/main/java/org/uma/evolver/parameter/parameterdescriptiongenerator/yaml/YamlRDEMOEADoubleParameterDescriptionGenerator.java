package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEADoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for class {@link RDEMOEADoubleParameterSpace}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlRDEMOEADoubleParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new RDEMOEADoubleParameterSpace());
    System.out.println(parameterString);
  }
}
