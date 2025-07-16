package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.util.List;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.*;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Parameter class representing the configuration and factory for crossover operators for {@link
 * org.uma.jmetal.solution.permutationsolution.PermutationSolution} in evolutionary algorithms.
 *
 * <p>This class allows the selection and configuration of a specific crossover operator (such as
 * PMX, CX, OXD, Position-Based, or Edge Recombination) for permutation-coded solutions. It
 * retrieves the required parameters (e.g., probability) from its global sub-parameters and produces
 * a configured {@link org.uma.jmetal.operator.crossover.CrossoverOperator} instance accordingly.
 *
 * <p>Supported crossover operators:
 *
 * <ul>
 *   <li><b>PMX</b> (Partially Mapped Crossover): requires a probability.
 *   <li><b>CX</b> (Cycle Crossover): requires a probability.
 *   <li><b>OXD</b> (Order Crossover - Davis): requires a probability.
 *   <li><b>positionBased</b> (Position-Based Crossover): requires a probability.
 *   <li><b>edgeRecombination</b> (Edge Recombination Crossover): requires a probability.
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>
 * PermutationCrossoverParameter crossoverParameter = new PermutationCrossoverParameter(List.of("PMX", "CX", "OXD", "positionBased", "edgeRecombination"));
 * // Configure sub-parameters as needed...
 * CrossoverOperator&lt;PermutationSolution&lt;Integer&gt;&gt; operator = crossoverParameter.getCrossover();
 * </pre>
 */
public class PermutationCrossoverParameter
    extends CrossoverParameter<PermutationSolution<Integer>> {

  private static List<String> validCrossoverNames =
      List.of("PMX", "CX", "OXD", "positionBased", "edgeRecombination");

  /** 
   * @param crossoverOperators the list of supported crossover operator names
   * @throws IllegalArgumentException if crossoverOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public PermutationCrossoverParameter(List<String> crossoverOperators) {
    this(DEFAULT_NAME, crossoverOperators);
  }

  /** 
   * @param name the name of the parameter
   * @param crossoverOperators the list of supported crossover operator names
   * @throws IllegalArgumentException if crossoverOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public PermutationCrossoverParameter(String name, List<String> crossoverOperators) {
    super(name, crossoverOperators);

    crossoverOperators.stream()
        .filter(crossoverOperator -> !validCrossoverNames.contains(crossoverOperator))
        .forEach(
            crossoverOperator -> {
              throw new JMetalException(
                  "Invalid crossover operator name: "
                      + crossoverOperator
                      + ". Supported names are: "
                      + validCrossoverNames);
            });
  }

  @Override
  public CrossoverOperator<PermutationSolution<Integer>> getCrossover() {
    Double crossoverProbability = (Double) findGlobalSubParameter("crossoverProbability").value();

    CrossoverOperator<PermutationSolution<Integer>> result;

    switch (value()) {
      case "PMX" -> result = new PMXCrossover(crossoverProbability);
      case "CX" -> result = new CycleCrossover(crossoverProbability);
      case "OXD" -> result = new OXDCrossover(crossoverProbability);
      case "positionBased" -> result = new PositionBasedCrossover(crossoverProbability);
      case "edgeRecombination" -> result = new EdgeRecombinationCrossover(crossoverProbability);
      default -> throw new JMetalException("Crossover operator does not exist: " + name());
    }

    return result;
  }
}
