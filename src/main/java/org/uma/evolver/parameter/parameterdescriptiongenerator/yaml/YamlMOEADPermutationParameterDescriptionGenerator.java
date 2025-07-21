package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.moead.PermutationMOEAD;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADPermutationParameterSpace;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Program to generate the YAML configuration file for the MOEA/D algorithm with permutation solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOEA/D
 * algorithm when applied to permutation-based solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlMOEADPermutationParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see MOEADPermutationParameterSpace
 * @see PermutationMOEAD
 * @author Antonio J. Nebro
 */
public class YamlMOEADPermutationParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for MOEA/D with permutation solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<PermutationSolution<?>>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new MOEADPermutationParameterSpace());
    System.out.println(parameterString);
  }
}
