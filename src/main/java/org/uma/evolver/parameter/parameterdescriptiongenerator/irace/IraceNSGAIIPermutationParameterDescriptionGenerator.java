package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;

/**
 * Program to generate the irace configuration file for the NSGA-II algorithm with permutation solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the NSGA-II
 * algorithm when applied to permutation-based solutions, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceNSGAIIPermutationParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see NSGAIIPermutationParameterSpace
 * @see NSGAIIPermutation
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class IraceNSGAIIPermutationParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for NSGA-II with permutation solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<>() ;
    parameterFileGenerator.generateConfigurationFile(new NSGAIIPermutationParameterSpace()) ;
  }
}
