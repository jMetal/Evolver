package org.uma.evolver.cli;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GeneralizedSpread;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Maps indicator names used in a training or solve request to jMetal quality indicator instances.
 *
 * <p>All of them are minimized and computed against the (normalized) reference front of each
 * problem. {@code Spread} only applies to bi-objective problems: {@link #checkApplicable} rejects
 * it for any other.
 */
public final class IndicatorRegistry {

  private static final Map<String, Supplier<QualityIndicator>> INDICATORS =
      Map.of(
          "Epsilon", Epsilon::new,
          "NormalizedHypervolume", NormalizedHypervolume::new,
          "InvertedGenerationalDistancePlus", InvertedGenerationalDistancePlus::new,
          "HypervolumeMinus", HypervolumeMinus::new,
          "Spread", Spread::new,
          "GeneralizedSpread", GeneralizedSpread::new);

  /** Indicators defined only for bi-objective problems; see {@link #checkApplicable}. */
  private static final Set<String> BI_OBJECTIVE_ONLY = Set.of("Spread");

  private IndicatorRegistry() {}

  /** Names registered in {@link #INDICATORS}, for {@code DescribeMain}. */
  public static Set<String> registeredNames() {
    return INDICATORS.keySet();
  }

  public static QualityIndicator resolve(String indicatorName) {
    Supplier<QualityIndicator> supplier = INDICATORS.get(indicatorName);
    if (supplier == null) {
      throw new JMetalException(
          "Unknown indicator: " + indicatorName + ". Supported indicators: " + INDICATORS.keySet());
    }
    return supplier.get();
  }

  /**
   * Checks that an indicator can measure the fronts of a problem with the given number of
   * objectives: {@code Spread}, Deb's diversity indicator, is defined only for two objectives (it
   * sorts the front and measures consecutive distances), so a training or solve request using it
   * on any other problem is rejected before it starts, instead of producing meaningless values.
   *
   * @param indicatorName a name registered in {@link #INDICATORS}
   * @param numberOfObjectives the number of objectives of a problem the indicator will measure
   * @throws JMetalException if the indicator is not defined for that number of objectives
   */
  public static void checkApplicable(String indicatorName, int numberOfObjectives) {
    if (BI_OBJECTIVE_ONLY.contains(indicatorName) && numberOfObjectives != 2) {
      throw new JMetalException(
          indicatorName
              + " is defined only for bi-objective problems, but a problem has "
              + numberOfObjectives
              + " objectives; use GeneralizedSpread instead");
    }
  }
}
