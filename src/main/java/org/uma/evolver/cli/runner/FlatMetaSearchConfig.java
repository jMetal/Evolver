package org.uma.evolver.cli.runner;

import java.util.List;

/**
 * Meta-search configuration for the flat [0,1]^n encoding, resolved via
 * {@link MetaAlgorithmRegistry}.
 *
 * @param algorithm the meta-optimizer algorithm name (e.g. {@code "ParallelNSGA-II"})
 * @param metaPopulationSize nullable; when null, {@link MetaAlgorithmRegistry}'s own default is
 *     used
 * @param operatorFlags the meta-optimizer's own operator configuration (crossover, mutation,
 *     selection, ...), as {@code ["--flag", "value", ...]} pairs taken directly from the
 *     meta-optimizer configuration file (every key besides {@code algorithm}, {@code encoding},
 *     {@code metaMaxEvaluations}, {@code metaPopulationSize} and {@code numberOfCores}). Parsed
 *     against an internal, per-algorithm parameter space that {@link MetaAlgorithmRegistry} owns
 *     — not user-facing, since a meta-optimizer's legal operator catalogue does not vary between
 *     requests.
 */
public record FlatMetaSearchConfig(
    String algorithm,
    int metaMaxEvaluations,
    Integer metaPopulationSize,
    int numberOfCores,
    List<String> operatorFlags)
    implements MetaSearchConfig {}
