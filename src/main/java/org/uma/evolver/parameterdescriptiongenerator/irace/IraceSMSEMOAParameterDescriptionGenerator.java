package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.algorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceSMSEMOAParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableSMSEMOA()) ;
  }
}
