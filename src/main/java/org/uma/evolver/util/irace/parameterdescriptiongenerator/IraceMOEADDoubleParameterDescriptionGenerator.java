package org.uma.evolver.util.irace.parameterdescriptiongenerator;

import org.uma.evolver.algorithm.base.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Program to generate the irace configuration file for the MOEA/D algorithm with real-coded solutions.
 *
 * <p>This class is a utility that generates a configuration file in the irace format, which can be used
 * for automatic algorithm configuration. The configuration includes all tunable parameters of the MOEA/D
 * algorithm when applied to real-coded solutions, allowing for automated parameter tuning using the irace package.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Run the generator to print the irace configuration to standard output
 * IraceMOEADDoubleParameterDescriptionGenerator.main(new String[]{});
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @see MOEADDoubleParameterSpace
 * @see DoubleMOEAD
 * @author Antonio J. Nebro
 */
public class IraceMOEADDoubleParameterDescriptionGenerator {
  /**
   * Main method that generates and prints the irace configuration for MOEA/D with real-coded solutions.
   * The output is printed to standard output and can be redirected to a file.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    var parameterFileGenerator = new IraceParameterDescriptionGenerator<DoubleSolution>() ;
    String yamlParameterSpaceFile = "MOEADDouble.yaml" ;
    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    parameterFileGenerator.generateConfigurationFile(parameterSpace);
  }
}
