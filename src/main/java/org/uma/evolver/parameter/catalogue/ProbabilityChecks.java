package org.uma.evolver.parameter.catalogue;

import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Checks of the probabilities that the operator parameters build from a configuration.
 *
 * <p>jMetal's operators reject an invalid probability with a message that does not name the
 * parameter ("The parameter 'value' (1.5) is not a valid probability value"), and when the
 * probability is derived from a factor (the mutation probability is {@code
 * mutationProbabilityFactor} divided by the number of variables), the value in the message is not
 * even the one of the configuration. These checks name the parameter and say how to fix it.
 */
public final class ProbabilityChecks {

  private ProbabilityChecks() {}

  /**
   * Checks that a parameter of the configuration is a probability.
   *
   * @param parameterName the name of the parameter, as in the configuration
   * @param value its value
   * @return the value
   * @throws JMetalException if the value is not in [0, 1]
   */
  public static double probability(String parameterName, double value) {
    if (!(value >= 0.0 && value <= 1.0)) {
      throw new JMetalException(
          parameterName + " (" + value + ") is not a probability: it must be in [0, 1]");
    }
    return value;
  }

  /**
   * The probability of mutating each variable, {@code mutationProbabilityFactor / variables},
   * checking that it is a probability.
   *
   * @param factor the value of {@code mutationProbabilityFactor}
   * @param variables the number of variables (or bits) of a solution of the problem
   * @param variablesName what the variables are, for the message ("variables", "bits")
   * @return the mutation probability
   * @throws JMetalException if the probability is not in [0, 1]
   */
  public static double mutationProbability(double factor, int variables, String variablesName) {
    double probability = factor / variables;
    if (!(probability >= 0.0 && probability <= 1.0)) {
      throw new JMetalException(
          "mutationProbabilityFactor ("
              + factor
              + ") divided by the number of "
              + variablesName
              + " of the problem ("
              + variables
              + ") gives a mutation probability of "
              + probability
              + ", which is not in [0, 1]: for this problem, mutationProbabilityFactor must be"
              + " in [0, "
              + variables
              + "]");
    }
    return probability;
  }
}
