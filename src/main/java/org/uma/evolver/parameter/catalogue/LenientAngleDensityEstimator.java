package org.uma.evolver.parameter.catalogue;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.impl.AngleDensityEstimator;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * jMetal's {@link AngleDensityEstimator}, whose {@link #value} gives 0.0, the worst density, to a
 * solution that has none, as the crowding, kNN and shifted estimators do.
 *
 * <p>In jMetal 7.7, {@code AngleDensityEstimator.value} throws an exception ({@code The parameter
 * 'object' is null}) for a solution whose density has not been computed. The algorithms of Evolver
 * that take a density estimator as a parameter (RDEMOEA and SSMOEA) compare the population by
 * density in their selection before the replacement has computed it, so any configuration that
 * chose the angle estimator failed. jMetal 7.8 gives it the same 0.0.
 *
 * <p>Remove this class, and use {@code AngleDensityEstimator} in {@link DensityEstimatorParameter},
 * when Evolver depends on jMetal 7.8.
 *
 * @param <S> the type of the solutions
 */
final class LenientAngleDensityEstimator<S extends Solution<?>> extends AngleDensityEstimator<S> {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the estimator with the objectives always normalized: angles are measured from the
   * origin, which only corresponds to the ideal point in the [0,1] normalized objective space.
   *
   * @param numberOfNeighbors the number of nearest angular neighbors used to estimate the density
   */
  LenientAngleDensityEstimator(int numberOfNeighbors) {
    super(null, true, numberOfNeighbors);
  }

  @Override
  public Double value(S solution) {
    Check.notNull(solution);
    Object value = solution.attributes().get(attributeId());
    return value == null ? 0.0 : (Double) value;
  }
}
