package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;


import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIBinaryParameterSpace;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;

/**
 * Program to generate the irace configuration file for class {@link NSGAIIDouble}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIBinaryParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<>() ;
    parameterFileGenerator.generateConfigurationFile(new NSGAIIBinaryParameterSpace()) ;
  }
}
