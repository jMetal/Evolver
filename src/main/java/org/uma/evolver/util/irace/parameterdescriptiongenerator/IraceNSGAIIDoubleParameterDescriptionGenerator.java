package org.uma.evolver.util.irace.parameterdescriptiongenerator;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for the NSGA-II algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the NSGA-II
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceNSGAIIDoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see NSGAIIDoubleParameterSpace
 * @see DoubleNSGAII
 * @author Antonio J. Nebro
 */
public class IraceNSGAIIDoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for NSGA-II with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<DoubleSolution>() ;
    var parameterSpace = new NSGAIIDoubleParameterSpace();
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
