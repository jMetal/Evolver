package org.uma.evolver.cli.runner;

/**
 * Prototype request describing a single meta-optimization training run, split into two
 * independent parts: {@link BaseLevelConfig} (what is being tuned, and on what training set —
 * independent of the meta-level encoding) and {@link MetaSearchConfig} (how the meta-optimizer
 * searches that parameter space, which differs entirely between the flat and tree encodings —
 * see {@link FlatMetaSearchConfig} and {@link TreeMetaSearchConfig}).
 *
 * <p>Scope note: this is a study prototype for {@link TrainingRunner}, exercised against four
 * reference examples to avoid overfitting to a single case:
 * {@code NSGAIIOptimizingNSGAIIForProblemZDT4} (single problem, flat encoding), {@code
 * NSGAIIOptimizingNSGAIIForBenchmarkRE3D} (named multi-problem training set, flat encoding),
 * {@code NSGAIIOptimizingMOEADForProblemZDT4} (a base-level algorithm other than NSGA-II, flat
 * encoding) and {@code TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D} (tree encoding). The
 * meta-optimizer algorithm itself is still fixed to NSGA-II, as it is in all four examples.
 */
public record TrainingRequest(BaseLevelConfig baseLevel, MetaSearchConfig metaSearch) {}
