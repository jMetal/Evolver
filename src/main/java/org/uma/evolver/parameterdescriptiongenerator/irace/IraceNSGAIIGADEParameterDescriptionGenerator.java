package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIGADE;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAIIDE}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIGADEParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableNSGAIIGADE()) ;
  }
}
