package org.uma.evolver.util.constrainthandling;

import java.util.function.ToDoubleFunction;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.constrainthandling.EpsilonConstrained;
import org.uma.jmetal.util.constrainthandling.PushPullSearch;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * The violation of a solution as a constraint-handling technique judges it, for an algorithm that
 * selects the feasible solutions by a criterion of its own and the infeasible ones by their
 * violation, as C-RVEA does in each reference vector: zero for a solution that the technique treats
 * as feasible, and otherwise a positive value, lower for a preferred solution.
 *
 * <ul>
 *   <li>{@code cdp}: the overall constraint violation;
 *   <li>{@code violatedConstraints}: the number of violated constraints;
 *   <li>{@code epsilonConstrained}: the overall constraint violation, or zero if it is at most the
 *       current epsilon;
 *   <li>{@code pushPull}: zero during the push stage, which ignores the constraints, and then as
 *       {@code epsilonConstrained}.
 * </ul>
 *
 * <p>The stochastic ranking and the self-adaptive penalty rank whole populations, so they do not
 * reduce to such a value. jMetal's techniques do not give this value themselves, nor does {@code
 * ConstrainedDominance} tell which measure it compares by, so it is computed here from the name of
 * the technique (the value of the {@code constraintHandling} parameter) and what the techniques
 * expose (their current epsilon and stage), at the moment the function is applied.
 */
public final class EffectiveViolation {

  private EffectiveViolation() {}

  /**
   * The violation of a solution as a technique judges it.
   *
   * @param name the name of the technique, as the {@code constraintHandling} parameter gives it
   * @param technique the technique
   * @param <S> the type of the solutions
   * @return the function that gives the violation of a solution
   * @throws JMetalException for a technique without such a value
   */
  public static <S extends Solution<?>> ToDoubleFunction<S> of(
      String name, ConstraintHandlingTechnique<S> technique) {
    return switch (name) {
      case "cdp" -> EffectiveViolation::violation;
      case "violatedConstraints" -> ConstraintHandling::numberOfViolatedConstraints;
      case "epsilonConstrained" -> {
        var epsilonConstrained = (EpsilonConstrained<S>) technique;
        yield solution -> withinEpsilon(violation(solution), epsilonConstrained.epsilon());
      }
      case "pushPull" -> {
        var pushPull = (PushPullSearch<S>) technique;
        yield solution ->
            pushPull.pushing() ? 0.0 : withinEpsilon(violation(solution), pushPull.epsilon());
      }
      default ->
          throw new JMetalException(
              "The technique "
                  + name
                  + " does not judge the violation of a single solution: use cdp,"
                  + " violatedConstraints, epsilonConstrained or pushPull");
    };
  }

  /** The overall constraint violation of a solution, as a non-negative value. */
  static double violation(Solution<?> solution) {
    return Math.max(0.0, -ConstraintHandling.overallConstraintViolationDegree(solution));
  }

  /** A violation, or zero if it is at most epsilon. */
  static double withinEpsilon(double violation, double epsilon) {
    return violation <= epsilon ? 0.0 : violation;
  }
}
