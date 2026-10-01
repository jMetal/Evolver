package org.uma.evolver.meta.builder;

import java.time.Duration;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByComputingTime;

/**
 * Computing-time limit of a meta-optimizer, the alternative to the limit on the number of
 * meta-evaluations. The two limits are mutually exclusive: a meta-optimizer is bounded by one or the
 * other.
 *
 * <p>The limit is given in minutes, with decimals (for example {@code 0.5}). It is checked at the
 * beginning of each iteration of the algorithm, so the generation in progress when the limit is
 * reached is always completed (the real time can exceed the limit by up to one generation), and the
 * initial population is always evaluated, even if that takes longer than the limit. The asynchronous
 * meta-optimizers have no generations: they use {@link #asynchronousTermination}, checked after every
 * evaluation.
 *
 * @see <a href="../../../../../../../../docs/proposals/meta-termination-by-time.md">proposal</a>
 */
public final class ComputingTimeLimit {

  private ComputingTimeLimit() {}

  /**
   * Checks the minutes of a limit.
   *
   * @return the minutes, if they are a finite number greater than zero
   * @throws IllegalArgumentException otherwise
   */
  public static double checkMinutes(double minutes) {
    if (!Double.isFinite(minutes) || minutes <= 0.0) {
      throw new IllegalArgumentException(
          "The maximum computing time must be a number of minutes greater than zero: " + minutes);
    }
    return minutes;
  }

  /** The limit as a {@link Duration} (rounded to the millisecond). */
  public static Duration toDuration(double minutes) {
    return Duration.ofMillis(Math.round(checkMinutes(minutes) * 60_000.0));
  }

  /** A termination condition that is met when the computing time reaches the limit. */
  public static Termination termination(double minutes) {
    return new TerminationByComputingTime(toDuration(minutes).toMillis());
  }

  /**
   * A termination condition for the asynchronous meta-optimizers, which are checked after every
   * evaluation and have no initial population evaluated as a block: it is met when the computing
   * time reaches the limit, but not before {@code populationSize} evaluations have been performed,
   * so the initial population is always evaluated. The real time exceeds the limit by up to one
   * evaluation; the evaluations in progress when it is met are discarded.
   */
  public static Termination asynchronousTermination(double minutes, int populationSize) {
    Termination byTime = termination(minutes);
    return data -> (int) data.get("EVALUATIONS") >= populationSize && byTime.isMet(data);
  }

  /**
   * Checks that the limit on the evaluations and the limit on the time are not both set.
   *
   * @param maxEvaluationsSet whether the number of meta-evaluations has been set
   * @param maxComputingTimeMinutes the limit on the time, or {@code null} if it has not been set
   * @throws IllegalStateException if both are set
   */
  public static void checkExclusive(boolean maxEvaluationsSet, Double maxComputingTimeMinutes) {
    if (maxEvaluationsSet && maxComputingTimeMinutes != null) {
      throw new IllegalStateException(
          "The maximum number of evaluations and the maximum computing time are mutually"
              + " exclusive: set only one of them");
    }
  }
}
