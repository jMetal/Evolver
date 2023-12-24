package org.uma.evolver.parameterdescriptiongenerator.yaml;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableMOEAD}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlMOEADParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableMOEAD());
    System.out.println(parameterString);
  }
}
