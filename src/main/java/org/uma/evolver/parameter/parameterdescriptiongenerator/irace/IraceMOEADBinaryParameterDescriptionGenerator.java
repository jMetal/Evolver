package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;


import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADBinaryParameterSpace;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;

/**
 * Program to generate the irace configuration file for class {@link DoubleNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOEADBinaryParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<>() ;
    parameterFileGenerator.generateConfigurationFile(new MOEADBinaryParameterSpace()) ;
  }
}
