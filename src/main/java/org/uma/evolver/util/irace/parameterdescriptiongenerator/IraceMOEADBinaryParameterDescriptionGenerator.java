package org.uma.evolver.util.irace.parameterdescriptiongenerator;

import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Program to generate the irace configuration file for the MOEA/D algorithm with binary solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOEA/D
 * algorithm when applied to binary-encoded problems, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceMOEADBinaryParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @author Antonio J. Nebro
 */
public class IraceMOEADBinaryParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for MOEA/D with binary solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<BinarySolution>();
    String yamlParameterSpaceFile = "MOEADBinary.yaml";
    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory());
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
