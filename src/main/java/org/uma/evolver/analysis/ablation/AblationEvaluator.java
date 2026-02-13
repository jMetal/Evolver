package org.uma.evolver.analysis.ablation;

import java.util.Map;

/**
 * Evaluates a configuration and returns a scalar metric value.
 */
@FunctionalInterface
public interface AblationEvaluator {

  /**
   * Evaluates a configuration.
   *
   * @param configuration the configuration map
   * @return the metric value
   */
  double evaluate(Map<String, String> configuration);
}
