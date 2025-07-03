package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.nsgaii.NSGAIIPermutation;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Program to generate the YAML configuration file for the NSGA-II algorithm with permutation solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the NSGA-II
 * algorithm when applied to permutation-based solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlNSGAIIPermutationParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see NSGAIIPermutationParameterSpace
 * @see NSGAIIPermutation
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class YamlNSGAIIPermutationParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for NSGA-II with permutation solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<PermutationSolution<Integer>>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new NSGAIIPermutationParameterSpace());
    System.out.println(parameterString);
  }
}
