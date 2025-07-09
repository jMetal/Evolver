package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.moead.MOEADDouble;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the YAML configuration file for the MOEA/D algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOEA/D
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlMOEADDoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see MOEADDoubleParameterSpace
 * @see MOEADDouble
 * @author Antonio J. Nebro
 */
public class YamlMOEADDoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for MOEA/D with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new MOEADDoubleParameterSpace());
    System.out.println(parameterString);
  }
}
