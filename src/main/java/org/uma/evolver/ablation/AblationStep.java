package org.uma.evolver.ablation;

/**
 * Represents a single step in the ablation path analysis.
 *
 * @param parameterChanged the parameter that was changed in this step
 * @param fromValue the previous value
 * @param toValue the new value
 * @param performanceBefore performance before the change
 * @param performanceAfter performance after the change
 * @author Antonio J. Nebro
 */
public record AblationStep(
    String parameterChanged,
    String fromValue,
    String toValue,
    double[] performanceBefore,
    double[] performanceAfter) {
  /** Calculates the improvement achieved by this step. */
  public double[] improvement() {
    double[] imp = new double[performanceBefore.length];
    for (int i = 0; i < imp.length; i++) {
      imp[i] = performanceBefore[i] - performanceAfter[i];
    }
    return imp;
  }
}
