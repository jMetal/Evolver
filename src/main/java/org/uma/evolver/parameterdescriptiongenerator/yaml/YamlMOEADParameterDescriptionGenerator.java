package org.uma.evolver.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;

/**
 * Program to generate the irace configuration file for class {@link ConfigurableNSGAII}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlMOEADParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator();
    String parameterString = parameterFileGenerator.generateConfiguration(
        new ConfigurableMOEAD(new FakeDoubleProblem(), 100, 20000, ""));
    System.out.println(parameterString);
  }
}