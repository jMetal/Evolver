package org.uma.evolver.util;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * The additive epsilon indicator (EP) for problems with constraints, with the two phases of the
 * indicator of Vodopija, Tušar and Filipič (jMetal's {@code ConstrainedMultiobjectiveIndicator},
 * ICMOP): the EP of the feasible points, capped at a threshold of 1, or the least violation plus
 * the threshold if there are none. Any front with feasible points is therefore at least as good as
 * any without them.
 *
 * <p>As in ICMOP, the objectives are normalized with the ideal and nadir points of the reference
 * front; a front already normalized with them does not change. Their paper studies only ICMOP: this
 * indicator is a generalization of it.
 *
 * @author Antonio J. Nebro
 */
public class ConstrainedEpsilon extends QualityIndicator {

  /** The threshold between the values of the fronts with and without feasible points. */
  public static final double THRESHOLD = 1.0;

  private double[] idealPoint;
  private double[] ranges;
  private double[][] normalizedReferenceFront;

  @Override
  public void referenceFront(double[][] referenceFront) {
    Check.notNull(referenceFront);
    Check.that(referenceFront.length > 0, "The reference front is empty");
    super.referenceFront(referenceFront);
    int numberOfObjectives = referenceFront[0].length;
    idealPoint = new double[numberOfObjectives];
    ranges = new double[numberOfObjectives];
    for (int j = 0; j < numberOfObjectives; j++) {
      int objective = j;
      idealPoint[j] = Arrays.stream(referenceFront).mapToDouble(p -> p[objective]).min().orElseThrow();
      double nadir = Arrays.stream(referenceFront).mapToDouble(p -> p[objective]).max().orElseThrow();
      ranges[j] = nadir > idealPoint[j] ? nadir - idealPoint[j] : 1.0;
    }
    normalizedReferenceFront = normalize(referenceFront);
  }

  /**
   * Computes the indicator for a front whose points are all feasible.
   *
   * @param front the objective vectors
   * @return the EP of the front, capped at the threshold
   */
  @Override
  public double compute(double[][] front) {
    Check.notNull(front);
    return compute(front, new double[front.length]);
  }

  /**
   * Computes the indicator for a front given by its objective vectors and their overall constraint
   * violations.
   *
   * @param front the objective vectors
   * @param violations the overall constraint violation of each point, non-negative (0 for a
   *     feasible one)
   * @return the value of the indicator
   */
  public double compute(double[][] front, double[] violations) {
    Check.notNull(front);
    Check.notNull(violations);
    Check.that(front.length > 0, "The front is empty");
    Check.that(
        front.length == violations.length,
        "There must be one violation per point: " + front.length + " vs " + violations.length);
    Check.that(normalizedReferenceFront != null, "The indicator has no reference front");

    double[][] feasible =
        IntStream.range(0, front.length)
            .filter(i -> violations[i] == 0.0)
            .mapToObj(i -> front[i])
            .toArray(double[][]::new);
    if (feasible.length > 0) {
      double value = new Epsilon(normalizedReferenceFront).compute(normalize(feasible));
      return Math.min(value, THRESHOLD);
    }
    double leastViolation = Arrays.stream(violations).min().orElseThrow();
    Check.that(leastViolation >= 0.0, "The violations must be non-negative: " + leastViolation);
    return THRESHOLD + leastViolation;
  }

  private double[][] normalize(double[][] front) {
    double[][] normalized = new double[front.length][];
    for (int i = 0; i < front.length; i++) {
      Check.that(
          front[i].length == idealPoint.length,
          "The points of the front must have as many objectives as the reference front");
      normalized[i] = new double[idealPoint.length];
      for (int j = 0; j < idealPoint.length; j++) {
        normalized[i][j] = (front[i][j] - idealPoint[j]) / ranges[j];
      }
    }
    return normalized;
  }

  @Override
  public boolean isTheLowerTheIndicatorValueTheBetter() {
    return true;
  }

  @Override
  public QualityIndicator newInstance() {
    return new ConstrainedEpsilon();
  }

  @Override
  public String name() {
    return "CEP";
  }

  @Override
  public String description() {
    return "Additive epsilon indicator for problems with constraints";
  }
}
