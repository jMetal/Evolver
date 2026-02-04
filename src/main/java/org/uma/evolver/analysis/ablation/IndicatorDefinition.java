package org.uma.evolver.analysis.ablation;

import java.util.Objects;
import java.util.function.Supplier;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;

/**
 * Defines how to build and interpret a quality indicator for ablation analyses.
 */
public record IndicatorDefinition(
    String name,
    Supplier<QualityIndicator> factory,
    boolean maximize) {

  /**
   * Creates a validated indicator definition.
   *
   * @throws IllegalArgumentException if the name is blank or the factory is null
   */
  public IndicatorDefinition {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Indicator name cannot be null or blank");
    }
    Objects.requireNonNull(factory, "Indicator factory cannot be null");
  }

  /**
   * Normalized Hypervolume (maximize).
   *
   * @return indicator definition
   */
  public static IndicatorDefinition normalizedHypervolume() {
    IndicatorDefinition result;
    result = new IndicatorDefinition("NormalizedHypervolume", NormalizedHypervolume::new, true);
    return result;
  }

  /**
   * Inverted Generational Distance Plus (minimize).
   *
   * @return indicator definition
   */
  public static IndicatorDefinition invertedGenerationalDistancePlus() {
    IndicatorDefinition result;
    result =
        new IndicatorDefinition(
            "InvertedGenerationalDistancePlus",
            InvertedGenerationalDistancePlus::new,
            false);
    return result;
  }

  /**
   * Epsilon indicator (minimize).
   *
   * @return indicator definition
   */
  public static IndicatorDefinition epsilon() {
    IndicatorDefinition result;
    result = new IndicatorDefinition("Epsilon", Epsilon::new, false);
    return result;
  }
}
