package org.uma.evolver.parameterdescriptiongenerator.yaml;

import javax.swing.SingleSelectionModel;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.parameterdescriptiongenerator.irace.IraceParameterDescriptionGenerator;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlNSGAIIParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableNSGAII());
    System.out.println(parameterString);
  }
}
