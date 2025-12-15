package org.uma.evolver.analysis.ablation;

/**
 * Represents the contribution of a single parameter change to the overall performance improvement.
 *
 * @param paramName the name of the parameter
 * @param optimizedValue the value in the optimized configuration
 * @param defaultValue the value in the default configuration
 * @param contribution the performance change when reverting to default (positive means the
 *     optimized value is better)
 * @author Antonio J. Nebro
 */
public record ParameterContribution(
    String paramName, String optimizedValue, String defaultValue, double[] contribution) {
  public double totalAbsoluteContribution() {
    double sum = 0;
    for (double c : contribution) {
      sum += Math.abs(c);
    }
    return sum;
  }
}
