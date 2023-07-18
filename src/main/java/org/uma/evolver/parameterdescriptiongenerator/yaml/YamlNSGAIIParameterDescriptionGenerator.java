package org.uma.evolver.parameterdescriptiongenerator.yaml;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlNSGAIIParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableNSGAII());
    System.out.println(parameterString);
  }
}
