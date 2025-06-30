package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;


import org.uma.evolver.algorithm.base.moead.MOEADDouble;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for class {@link MOEADDouble}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOEADDoubleParameterDescriptionGenerator {
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<DoubleSolution>() ;
    var parameterSpace = new MOEADDoubleParameterSpace();
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
