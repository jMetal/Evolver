package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;


import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.RDEMOEAPermutationParameterSpace;

/**
 * Program to generate the irace configuration file for class {@link NSGAIIDouble}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIPermutationParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new NSGAIIPermutationParameterSpace() {}) ;
  }
}
