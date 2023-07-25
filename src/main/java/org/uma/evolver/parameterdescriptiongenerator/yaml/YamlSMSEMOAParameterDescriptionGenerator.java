package org.uma.evolver.parameterdescriptiongenerator.yaml;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableSMSEMOA}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlSMSEMOAParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableNSGAII());
    System.out.println(parameterString);
  }
}
