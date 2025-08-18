package org.uma.evolver.util.irace.parameterdescriptiongenerator;

import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADPermutationParameterSpace;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Program to generate the irace configuration file for the MOEA/D algorithm with permutation solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOEA/D
 * algorithm when applied to permutation-based solutions, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceMOEADPermutationParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see MOEADPermutationParameterSpace
 * @author Antonio J. Nebro
 */
public class IraceMOEADPermutationParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for MOEA/D with permutation solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<PermutationSolution<?>>() ;
    var parameterSpace = new MOEADPermutationParameterSpace();
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
