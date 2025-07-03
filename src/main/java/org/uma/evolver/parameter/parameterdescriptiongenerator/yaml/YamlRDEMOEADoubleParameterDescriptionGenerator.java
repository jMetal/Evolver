package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.rdsmoea.RDEMOEADouble;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEADoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the YAML configuration file for the RDS-MOEA/D algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the RDS-MOEA/D
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlRDEMOEADoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see RDEMOEADoubleParameterSpace
 * @see RDEMOEADouble
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class YamlRDEMOEADoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for RDS-MOEA/D with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new RDEMOEADoubleParameterSpace());
    System.out.println(parameterString);
  }
}
