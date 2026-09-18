package org.uma.evolver.cli.training;

/**
 * How the meta-optimizer searches the base-level algorithm's parameter space — the part of a
 * {@link TrainingRequest} that depends on the meta-level encoding, loaded from a named,
 * reusable meta-optimizer configuration file via {@link MetaOptimizerConfigurationReader} (e.g.
 * {@code MetaParallelNSGAIIFlatConfiguration.yaml}) — never inlined in the request itself.
 *
 * <p>Evolver supports two meta-level encodings, and they are configured very differently:
 * <ul>
 *   <li>{@link FlatMetaSearchConfig}: the flat [0,1]^n encoding. The meta-optimizer's own
 *       crossover/mutation/selection are set via {@link FlatMetaSearchConfig#operatorFlags()}
 *       against an internal, per-algorithm parameter space (reusing the same machinery as a
 *       base-level algorithm's, restricted to what a meta-optimizer needs), built via
 *       {@link MetaAlgorithmRegistry}.
 *   <li>{@link TreeMetaSearchConfig}: the derivation tree encoding. There is no parameter space
 *       for the meta level at all — the meta-optimizer operates directly on derivations of the
 *       base-level algorithm's own grammar, using two fixed operators ({@code SubtreeCrossover},
 *       {@code TreeMutation}) configured by a handful of scalar hyperparameters.
 * </ul>
 */
public sealed interface MetaSearchConfig permits FlatMetaSearchConfig, TreeMetaSearchConfig {

  /** The meta-optimizer algorithm to use, resolved via {@link MetaAlgorithmRegistry}. */
  String algorithm();

  int metaMaxEvaluations();

  int numberOfCores();
}
