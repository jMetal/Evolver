package org.uma.evolver.cli.runner;

/**
 * Meta-search configuration for the derivation tree encoding. There is no YAML parameter space
 * for the meta level: the meta-optimizer operates directly on derivations of the base-level
 * algorithm's own grammar, using {@code SubtreeCrossover} and {@code TreeMutation}.
 *
 * @param algorithm the meta-optimizer algorithm name, resolved via {@link MetaAlgorithmRegistry}
 *     (only {@code "ParallelNSGA-II"} is supported for this encoding)
 */
public record TreeMetaSearchConfig(
    String algorithm,
    int metaMaxEvaluations,
    int metaPopulationSize,
    int metaOffspringSize,
    int numberOfCores,
    double crossoverProbability,
    double mutationProbability,
    double mutationDistributionIndex)
    implements MetaSearchConfig {}
