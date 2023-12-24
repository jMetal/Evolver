package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAIIDE}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIDEParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableNSGAIIDE()) ;
  }
}
