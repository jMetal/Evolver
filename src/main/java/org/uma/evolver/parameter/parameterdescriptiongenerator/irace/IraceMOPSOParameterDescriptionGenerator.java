package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.base.moead.MOEADDouble;
import org.uma.evolver.algorithm.base.mopso.MOPSOParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for the MOPSO (Multi-Objective Particle Swarm Optimization)
 * algorithm.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOPSO
 * algorithm, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceMOPSOParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see MOPSOParameterSpace
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class IraceMOPSOParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for MOPSO.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<DoubleSolution>() ;
    var parameterSpace = new MOPSOParameterSpace();
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
