package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;

/**
 * Program to generate the irace configuration file for class {@link org.uma.evolver.algorithm.impl.ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableNSGAII(new FakeDoubleProblem(), 100, 20000)) ;
  }
}