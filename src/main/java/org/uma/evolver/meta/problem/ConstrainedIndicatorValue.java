package org.uma.evolver.meta.problem;

import java.util.Arrays;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GeneralizedSpread;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * The value of a quality indicator for a configuration on a training set with constraints, after
 * the two-phase indicator of Vodopija, Tušar and Filipič ("Characterization of Constrained
 * Continuous Multiobjective Optimization Problems: A Performance Space Perspective", IEEE TEVC
 * 29(1), 2025): a feasible result is always better than an infeasible one, and infeasible results
 * are graded by their constraint violation.
 *
 * <p>Over the runs of a configuration on all the problems, the value of an indicator is {@code 4k +
 * 2v + Q}, where
 *
 * <ul>
 *   <li>{@code k} is the number of runs whose result has no feasible solution: each one adds 4,
 *       more than the other two terms together, so a configuration with fewer such runs is always
 *       better;
 *   <li>{@code v} is the mean scaled violation of those runs (see {@link #scaledViolation}), in [0,
 *       1): with the same number of failed runs, the one closer to the feasible region is better;
 *   <li>{@code Q} is the mean over the problems of the median of the indicator over their feasible
 *       runs, capped at the {@link #threshold} of the indicator, which is also the value of a
 *       problem without feasible runs: the quality, which only decides when the rest tie.
 * </ul>
 *
 * <p>With a single problem and run, this orders results as the indicator of Vodopija et al.: a
 * feasible result below the threshold, an infeasible one above it.
 */
public final class ConstrainedIndicatorValue {

  /** What each run without a feasible solution adds to the value. */
  static final double FAILED_RUN_WEIGHT = 4.0;

  /** The weight of the mean scaled violation of the runs without feasible solutions. */
  static final double VIOLATION_WEIGHT = 2.0;

  private ConstrainedIndicatorValue() {}

  /**
   * The threshold τ* of an indicator: its worst sensible value on fronts normalized with the
   * bounds of the reference front, at which its values are capped and which a problem without
   * feasible runs takes.
   *
   * @param indicator the indicator
   * @return 0 for HV- (no hypervolume) and 1 for NHV, EP, IGD+, Spread and GeneralizedSpread
   * @throws JMetalException for an indicator without a known threshold
   */
  public static double threshold(QualityIndicator indicator) {
    Check.notNull(indicator);
    if (indicator instanceof HypervolumeMinus) {
      return 0.0;
    }
    if (indicator instanceof NormalizedHypervolume
        || indicator instanceof Epsilon
        || indicator instanceof InvertedGenerationalDistancePlus
        || indicator instanceof Spread
        || indicator instanceof GeneralizedSpread) {
      return 1.0;
    }
    throw new JMetalException(
        "The indicator "
            + indicator.name()
            + " has no threshold for problems with constraints; use EP, NHV, IGD+, HV-, Spread or"
            + " GeneralizedSpread");
  }

  /**
   * The scale of the constraint violation of a problem, after Vodopija et al.: the power of ten
   * above the median violation of a sample of solutions, so that violations of problems of
   * different magnitudes become comparable.
   *
   * @param violations the (non-negative) violations of a sample of solutions of the problem
   * @return {@code 10^ceil(log10(median))}, or 1 if the median is 0
   */
  public static double scale(double[] violations) {
    Check.notNull(violations);
    Check.that(violations.length > 0, "The sample of violations is empty");
    double[] sorted = violations.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    double median = n % 2 == 0 ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 : sorted[n / 2];
    return median > 0.0 ? Math.pow(10.0, Math.ceil(Math.log10(median))) : 1.0;
  }

  /**
   * A violation divided by the scale of its problem and mapped to [0, 1), so that the large
   * violations of some problems do not dominate the mean over runs.
   *
   * @param violation the (non-negative) violation
   * @param scale the scale of the problem (see {@link #scale})
   * @return {@code x / (1 + x)} with {@code x = violation / scale}
   */
  public static double scaledViolation(double violation, double scale) {
    Check.that(violation >= 0.0, "The violation must be non-negative: " + violation);
    Check.that(scale > 0.0, "The scale must be positive: " + scale);
    double x = violation / scale;
    return x / (1.0 + x);
  }

  /**
   * The value of an indicator for a configuration.
   *
   * @param failedRuns the number of runs whose result has no feasible solution
   * @param sumOfScaledViolations the sum of the scaled violations of those runs
   * @param quality the mean over the problems of the capped median of the indicator over their
   *     feasible runs (the threshold for a problem without them)
   * @return {@code 4k + 2v + Q}
   */
  public static double value(int failedRuns, double sumOfScaledViolations, double quality) {
    Check.that(failedRuns >= 0, "The number of failed runs must be non-negative: " + failedRuns);
    double meanViolation = failedRuns == 0 ? 0.0 : sumOfScaledViolations / failedRuns;
    return FAILED_RUN_WEIGHT * failedRuns + VIOLATION_WEIGHT * meanViolation + quality;
  }
}
