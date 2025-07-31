package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.mopso.BaseMOPSO;
import org.uma.evolver.algorithm.base.mopso.MOPSOParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the YAML configuration file for the MOPSO (Multi-Objective Particle Swarm Optimization) algorithm.
 *
 * <p>This class is a utility that generates a configuration file in YAML format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOPSO
 * algorithm, allowing for automated parameter tuning.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the YAML configuration to standard output
 * YamlMOPSOParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see MOPSOParameterSpace
 * @see BaseMOPSO
 * @author Antonio J. Nebro
 */
public class YamlMOPSOParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the YAML configuration for MOPSO.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<DoubleSolution>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(new MOPSOParameterSpace());
    System.out.println(parameterString);
  }
}
