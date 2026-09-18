package org.uma.evolver.cli.training;

/**
 * Prototype request describing a single meta-optimization training run: {@link BaseLevelConfig}
 * (what is being tuned, and on what training set — independent of the meta-level encoding),
 * {@link MetaSearchConfig} (how the meta-optimizer searches that parameter space, which differs
 * entirely between the flat and tree encodings — see {@link FlatMetaSearchConfig} and
 * {@link TreeMetaSearchConfig}), and three fields specific to monitoring *this* run rather than
 * to the algorithm itself: {@code outputDirectory} (where results are written), {@code
 * writeFrequency} (how often, in evaluations, {@code CONFIGURATIONS.csv}/{@code INDICATORS.csv}
 * are written — defaults to 100, roughly one generation, not every single evaluation) and {@code
 * statusFrequency} (how often {@code status.yaml}/the log are updated — also defaults to 100).
 * These three live here, not on {@link BaseLevelConfig} or {@link MetaSearchConfig}, precisely
 * because {@code baseLevel} and {@code metaSearch} are meant to be reused unchanged across many
 * requests (loaded by name — see {@link BaseLevelConfigurationReader}/
 * {@link MetaOptimizerConfigurationReader}), while how to observe one particular run is not part
 * of the algorithm's own recipe — two requests can share the exact same {@code baseLevel}/
 * {@code metaSearch} and still want different output locations or reporting cadence.
 *
 * <p>{@code frontPlotFrequency} is a fourth, optional, monitoring-only field: when present, {@link
 * TrainingRunner} registers a live {@code FrontPlotObserver} updated every that many evaluations;
 * when absent (the default), the run stays headless — deliberately opt-in, since {@code
 * TrainingRunner} is also driven by external processes (e.g. a GUI) that would not want a Swing
 * window popping up on their machine.
 *
 * <p>Scope note: this is a study prototype for {@link TrainingRunner}, exercised against seven
 * reference examples to avoid overfitting to a single case:
 * {@code NSGAIIOptimizingNSGAIIForProblemZDT4} (single problem, flat encoding), {@code
 * NSGAIIOptimizingNSGAIIForBenchmarkRE3D} (named multi-problem training set, flat encoding),
 * {@code NSGAIIOptimizingMOEADForProblemZDT4} (a base-level algorithm other than NSGA-II, flat
 * encoding), {@code TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D} (tree encoding), {@code
 * AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ} (a meta-optimizer engine other than {@code
 * NSGA-II}, asynchronous), {@code SMPSOOptimizingNSGAIIForProblemRE31} (a meta-optimizer
 * engine with no {@code EvolutionaryAlgorithm}/{@code AsynchronousMultiThreadedNSGAII} shape) and
 * {@code SPEA2OptimizingNSGAIIForProblemDTLZ3} (a second evolutionary meta-optimizer engine, with
 * its own hardcoded operators). The meta-optimizer algorithm is selected explicitly via
 * {@link MetaSearchConfig#algorithm()}, resolved by {@link MetaAlgorithmRegistry};
 * {@code "NSGA-II"}, {@code "SPEA2"}, {@code "AsyncNSGA-II"} and {@code "SMPSO"} are
 * registered so far.
 */
public record TrainingRequest(
    BaseLevelConfig baseLevel,
    MetaSearchConfig metaSearch,
    String outputDirectory,
    int writeFrequency,
    int statusFrequency,
    Integer frontPlotFrequency) {}
