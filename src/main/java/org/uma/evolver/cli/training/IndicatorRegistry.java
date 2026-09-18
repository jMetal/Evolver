package org.uma.evolver.cli.training;

import java.util.Map;
import java.util.function.Supplier;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Maps indicator names used in a {@link TrainingRequest} to jMetal quality indicator instances.
 *
 * <p>Prototype scope: only the indicators needed to reproduce the reference example
 * ({@code NSGAIIOptimizingNSGAIIForProblemZDT4}) are registered.
 */
final class IndicatorRegistry {

  private static final Map<String, Supplier<QualityIndicator>> INDICATORS =
      Map.of(
          "Epsilon", Epsilon::new,
          "NormalizedHypervolume", NormalizedHypervolume::new,
          "InvertedGenerationalDistancePlus", InvertedGenerationalDistancePlus::new);

  private IndicatorRegistry() {}

  static QualityIndicator resolve(String indicatorName) {
    Supplier<QualityIndicator> supplier = INDICATORS.get(indicatorName);
    if (supplier == null) {
      throw new JMetalException(
          "Unknown indicator: " + indicatorName + ". Supported indicators: " + INDICATORS.keySet());
    }
    return supplier.get();
  }
}
