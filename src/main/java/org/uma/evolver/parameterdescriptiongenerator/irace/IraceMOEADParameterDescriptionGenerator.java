package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableMOEAD}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOEADParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableMOEAD()) ;
  }
}
