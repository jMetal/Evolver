package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOEADParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableMOEAD()) ;
  }
}
