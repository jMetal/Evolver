package org.uma.evolver.parameter.parameterdescriptiongenerator.irace;

import org.uma.evolver.algorithm.base.rdsmoea.DoubleRDEMOEA;
import org.uma.evolver.algorithm.base.rdsmoea.parameterspace.RDEMOEADoubleParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for the RDS-MOEA/D algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the RDS-MOEA/D
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceRDEMOEADoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see RDEMOEADoubleParameterSpace
 * @see DoubleRDEMOEA
 * @author Antonio J. Nebro
 */
public class IraceRDEMOEADoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for RDS-MOEA/D with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<DoubleSolution>() ;
    var parameterSpace = new RDEMOEADoubleParameterSpace();
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
