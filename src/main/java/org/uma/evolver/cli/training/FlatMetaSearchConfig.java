package org.uma.evolver.cli.training;

import java.util.List;

/**
 * Meta-search configuration for the flat [0,1]^n encoding, resolved via
 * {@link MetaAlgorithmRegistry}.
 *
 * @param algorithm the meta-optimizer algorithm name (e.g. {@code "NSGA-II"})
 * @param metaMaxEvaluations the limit on the meta-evaluations, or 0 if bounded by computing time
 * @param metaMaxComputingTimeMinutes the limit on the computing time in minutes, or 0 if bounded
 *     by evaluations (exactly one of the two limits is given)
 * @param metaPopulationSize nullable; when null, {@link MetaAlgorithmRegistry}'s own default (50)
 *     is used
 * @param operatorFlags the meta-optimizer's own operator configuration (crossover, mutation,
 *     selection, ...), as {@code ["--flag", "value", ...]} pairs taken directly from the
 *     meta-optimizer configuration file (every key besides {@code algorithm}, {@code encoding},
 *     {@code metaMaxEvaluations}, {@code metaMaxComputingTimeMinutes}, {@code metaPopulationSize}
 *     and {@code numberOfCores}). Parsed
 *     against an internal, per-algorithm parameter space that {@link MetaAlgorithmRegistry} owns
 *     — not user-facing, since a meta-optimizer's legal operator catalogue does not vary between
 *     requests.
 */
public record FlatMetaSearchConfig(
    String algorithm,
    int metaMaxEvaluations,
    double metaMaxComputingTimeMinutes,
    Integer metaPopulationSize,
    int numberOfCores,
    List<String> operatorFlags)
    implements MetaSearchConfig {

  /** A configuration bounded by evaluations. */
  public FlatMetaSearchConfig(
      String algorithm,
      int metaMaxEvaluations,
      Integer metaPopulationSize,
      int numberOfCores,
      List<String> operatorFlags) {
    this(algorithm, metaMaxEvaluations, 0.0, metaPopulationSize, numberOfCores, operatorFlags);
  }
}
