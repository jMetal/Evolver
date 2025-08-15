package org.uma.evolver.util;

import org.uma.jmetal.qualityindicator.QualityIndicator;

/**
 * A specialized quality indicator that tracks and returns the number of evaluations performed by an algorithm.
 * 
 * <p>This class extends {@link QualityIndicator} but doesn't measure solution quality. Instead, it's designed
 * for meta-optimization scenarios where the number of function evaluations is a key parameter to be optimized.
 * 
 * <p>Example usage in meta-optimization:
 * <pre>
 * {@code
 * EvaluationsQualityIndicator indicator = new EvaluationsQualityIndicator();
 * indicator.setNumberOfEvaluations(10000);
 * double evaluations = indicator.compute(front);  // Returns 10000.0
 * }
 * </pre>
 * 
 * @see QualityIndicator
 */
public class EvaluationsQualityIndicator extends QualityIndicator {

  /** The number of evaluations to be returned by this indicator. */
  private int numberOfEvaluations;

  /**
   * Sets the number of evaluations that this indicator should return.
   * 
   * @param numberOfEvaluations the number of evaluations to be set
   */
  public void setNumberOfEvaluations(int numberOfEvaluations) {
    this.numberOfEvaluations = numberOfEvaluations;
  }

  @Override
  public double compute(double[][] front) {
    return numberOfEvaluations;
  }

  @Override
  public boolean isTheLowerTheIndicatorValueTheBetter() {
    return true;
  }

  @Override
  public QualityIndicator newInstance() {
    return new EvaluationsQualityIndicator();
  }

  @Override
  public String name() {
    return "Evaluations";
  }

  @Override
  public String description() {
    return "Tracks and returns the number of evaluations performed by an algorithm";
  }
}
