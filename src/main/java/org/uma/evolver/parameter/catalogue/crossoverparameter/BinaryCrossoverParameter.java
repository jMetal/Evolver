package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.util.List;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.*;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Parameter class representing the configuration and factory for crossover operators for {@link
 * org.uma.jmetal.solution.binarysolution.BinarySolution} in evolutionary algorithms.
 *
 * <p>This class allows the selection and configuration of a specific crossover operator (such as
 * Single Point, Two Point, or Uniform Crossover) for binary-coded solutions. It retrieves the
 * required parameters (e.g., probability) from its global sub-parameters and produces a configured
 * {@link org.uma.jmetal.operator.crossover.CrossoverOperator} instance accordingly.
 *
 * <p>Supported crossover operators:
 *
 * <ul>
 *   <li><b>singlePoint</b> (Single Point Crossover): requires a probability.
 *   <li><b>HUX</b> (HUX Crossover): requires a probability.
 *   <li><b>uniform</b> (Uniform Crossover): requires a probability.
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>
 * BinaryCrossoverParameter crossoverParameter = new BinaryCrossoverParameter(List.of("singlePoint", "twoPoint", "uniform"));
 * // Configure sub-parameters as needed...
 * CrossoverOperator&lt;BinarySolution&gt; operator = crossoverParameter.getCrossover();
 * </pre>
 */
public class BinaryCrossoverParameter extends CrossoverParameter<BinarySolution> {
  private static List<String> validCrossoverNames = List.of("HUX", "uniform", "singlePoint");

  /**
   * Constructs a new BinaryCrossoverParameter with the specified list of crossover operator names.
   *
   * @param crossoverOperators the list of supported crossover operator names
   * @throws IllegalArgumentException if crossoverOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public BinaryCrossoverParameter(List<String> crossoverOperators) {
    this(DEFAULT_NAME, crossoverOperators);
  }
  
  /**
   * Constructs a new BinaryCrossoverParameter with the specified list of crossover operator names.
   *
   * @param name the name of the parameter
   * @param crossoverOperators the list of supported crossover operator names
   * @throws IllegalArgumentException if crossoverOperators is null, empty, or contains invalid values
   * @throws JMetalException if any operator name is not supported
   */
  public BinaryCrossoverParameter(String name, List<String> crossoverOperators) {
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
  public CrossoverOperator<BinarySolution> getCrossover() {
    Double crossoverProbability = (Double) findGlobalSubParameter("crossoverProbability").value();

    CrossoverOperator<BinarySolution> result;
    switch (value()) {
      case "HUX" -> result = new HUXCrossover<>(crossoverProbability);
      case "uniform" -> result = new UniformCrossover<>(crossoverProbability);
      case "singlePoint" -> result = new SinglePointCrossover<>(crossoverProbability);
      default -> throw new JMetalException("Crossover operator does not exist: " + name());
    }
    return result;
  }
}
