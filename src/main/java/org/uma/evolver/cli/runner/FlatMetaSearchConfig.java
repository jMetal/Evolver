package org.uma.evolver.cli.runner;

/**
 * Meta-search configuration for the flat [0,1]^n encoding, built via {@code MetaNSGAIIBuilder}.
 *
 * @param metaPopulationSize nullable; when null, the builder's own default is used
 * @param mutationProbabilityFactor nullable; when null, the builder's own default is used
 * @param metaYamlParameterSpaceFile the meta-optimizer's own parameter space (crossover, mutation
 *     and selection operators are themselves categorical parameters defined here)
 */
public record FlatMetaSearchConfig(
    int metaMaxEvaluations,
    Integer metaPopulationSize,
    int numberOfCores,
    Double mutationProbabilityFactor,
    String metaYamlParameterSpaceFile)
    implements MetaSearchConfig {}
