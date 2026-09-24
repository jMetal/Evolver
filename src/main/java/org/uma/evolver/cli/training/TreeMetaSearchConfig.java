package org.uma.evolver.cli.training;

import java.util.List;

/**
 * Meta-search configuration for the derivation tree encoding, resolved via {@link
 * MetaAlgorithmRegistry}. The meta-optimizer operates directly on derivations of the base-level
 * algorithm's own grammar, using {@code SubtreeCrossover} and {@code TreeMutation}.
 *
 * <p>There is no offspring size field: as for every meta-optimizer, the offspring population
 * size always equals {@code metaPopulationSize}, so that each generation's offspring is evaluated
 * in parallel as a whole.
 *
 * @param algorithm the meta-optimizer algorithm name, resolved via {@link MetaAlgorithmRegistry}
 *     (only algorithms whose descriptor declares {@code supportsTree})
 * @param metaPopulationSize ignored by meta-optimizers without a population ({@code RandomSearch})
 * @param operatorFlags the meta-optimizer's own operator configuration (crossover/mutation
 *     probabilities, mutation distribution index, selection, ...), as {@code ["--flag", "value",
 *     ...]} pairs taken directly from the meta-optimizer configuration file — same convention as
 *     {@link FlatMetaSearchConfig#operatorFlags()}, parsed against a tree-specific parameter space
 *     that {@link MetaAlgorithmRegistry} owns
 */
public record TreeMetaSearchConfig(
    String algorithm,
    int metaMaxEvaluations,
    int metaPopulationSize,
    int numberOfCores,
    List<String> operatorFlags)
    implements MetaSearchConfig {}
