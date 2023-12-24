package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableSMSEMOA}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceSMSEMOAParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableSMSEMOA()) ;
  }
}
