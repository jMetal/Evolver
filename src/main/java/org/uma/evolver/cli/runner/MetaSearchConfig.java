package org.uma.evolver.cli.runner;

/**
 * How the meta-optimizer searches the base-level algorithm's parameter space — the part of a
 * {@link TrainingRequest} that depends on the meta-level encoding.
 *
 * <p>Evolver supports two meta-level encodings, and they are configured very differently:
 * <ul>
 *   <li>{@link FlatMetaSearchConfig}: the flat [0,1]^n encoding. The meta-optimizer's own
 *       crossover/mutation/selection are themselves categorical parameters defined in a YAML
 *       parameter space (reusing the same machinery as the base-level algorithm), built via
 *       {@code MetaNSGAIIBuilder}.
 *   <li>{@link TreeMetaSearchConfig}: the derivation tree encoding. There is no YAML parameter
 *       space for the meta level at all — the meta-optimizer operates directly on derivations of
 *       the base-level algorithm's own grammar, using two fixed operators
 *       ({@code SubtreeCrossover}, {@code TreeMutation}) configured by a handful of scalar
 *       hyperparameters.
 * </ul>
 */
public sealed interface MetaSearchConfig permits FlatMetaSearchConfig, TreeMetaSearchConfig {

  int metaMaxEvaluations();

  int numberOfCores();
}
