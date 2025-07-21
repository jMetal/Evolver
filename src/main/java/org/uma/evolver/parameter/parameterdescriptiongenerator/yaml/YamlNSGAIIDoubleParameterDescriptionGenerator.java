package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the YAML configuration file for the NSGA-II algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the NSGA-II
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlNSGAIIDoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see NSGAIIDoubleParameterSpace
 * @see DoubleNSGAII
 * @author Antonio J. Nebro
 */
public class YamlNSGAIIDoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for NSGA-II with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new NSGAIIDoubleParameterSpace());
    System.out.println(parameterString);
  }
}
