package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceMOPSOParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableMOPSO()) ;
  }
}
