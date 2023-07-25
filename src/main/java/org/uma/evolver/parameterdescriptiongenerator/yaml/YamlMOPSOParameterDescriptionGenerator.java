package org.uma.evolver.parameterdescriptiongenerator.yaml;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableMOPSO}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlMOPSOParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableMOPSO());
    System.out.println(parameterString);
  }
}
