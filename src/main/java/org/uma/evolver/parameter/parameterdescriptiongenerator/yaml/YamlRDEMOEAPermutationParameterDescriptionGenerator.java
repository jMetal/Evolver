package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.rdsmoea.PermutationRDEMOEA;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEAPermutationParameterSpace;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Program to generate the YAML configuration file for the RDS-MOEA/D algorithm with permutation solutions.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the RDS-MOEA/D
 * algorithm when applied to permutation-based solutions, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlRDEMOEAPermutationParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see RDEMOEAPermutationParameterSpace
 * @see PermutationRDEMOEA
 * @author Antonio J. Nebro
 */
public class YamlRDEMOEAPermutationParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for RDS-MOEA/D with permutation solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<PermutationSolution<?>>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new RDEMOEAPermutationParameterSpace());
    System.out.println(parameterString);
  }
}
