package org.uma.evolver.parameterdescriptiongenerator.irace;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIWithDE;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class IraceNSGAIIWithDEParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new ConfigurableNSGAIIWithDE(new FakeDoubleProblem(), 100, 20000)) ;
  }
}
