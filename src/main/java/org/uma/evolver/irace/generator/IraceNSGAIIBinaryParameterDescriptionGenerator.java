package org.uma.evolver.irace.generator;

import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Program to generate the irace configuration file for the NSGA-II algorithm with binary solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the NSGA-II
 * algorithm when applied to binary-encoded problems, allowing for automated parameter tuning.
 *
 * <p>The generated configuration file defines the parameter space for binary-specific operators:
 * <ul>
 *   <li>Binary crossover operators (HUX, uniform, single-point)
 *   <li>Bit-flip mutation
 *   <li>Population size and other NSGA-II specific parameters
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceNSGAIIBinaryParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @author Antonio J. Nebro
 */
public class IraceNSGAIIBinaryParameterDescriptionGenerator {

  /**
   * Main method that generates and prints the irace configuration for NSGA-II with binary solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<BinarySolution>();
    String yamlParameterSpaceFile = "NSGAIIBinary.yaml";
    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new BinaryParameterFactory());
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
