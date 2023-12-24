package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableMOPSO}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOPSOParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableMOPSO()) ;
  }
}
