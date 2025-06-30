package org.uma.evolver.parameter.parameterdescriptiongenerator.yaml;

import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Program to generate the irace configuration file for class {@link.EvNSGAIIDouble}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class YamlNSGAIIPermutationParameterDescriptionGenerator {

  public static void main(String[] args) {
    var parameterFileGenerator = new YamlParameterDescriptionGenerator<PermutationSolution<Integer>>();
    String parameterString =
        parameterFileGenerator.generateConfiguration(
            new NSGAIIPermutationParameterSpace());
    System.out.println(parameterString);
  }
}
