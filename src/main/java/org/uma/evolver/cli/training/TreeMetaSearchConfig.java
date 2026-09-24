package org.uma.evolver.cli.training;

/**
 * Meta-search configuration for the derivation tree encoding. There is no YAML parameter space
 * for the meta level: the meta-optimizer operates directly on derivations of the base-level
 * algorithm's own grammar, using {@code SubtreeCrossover} and {@code TreeMutation}.
 *
 * <p>There is no offspring size field: as for every meta-optimizer, the offspring population
 * size always equals {@code metaPopulationSize}, so that each generation's offspring is evaluated
 * in parallel as a whole.
 *
 * @param algorithm the meta-optimizer algorithm name, resolved via {@link MetaAlgorithmRegistry}
 *     (only {@code "NSGA-II"} is supported for this encoding)
 */
public record TreeMetaSearchConfig(
    String algorithm,
    int metaMaxEvaluations,
    int metaPopulationSize,
    int numberOfCores,
    double crossoverProbability,
    double mutationProbability,
    double mutationDistributionIndex)
    implements MetaSearchConfig {}
