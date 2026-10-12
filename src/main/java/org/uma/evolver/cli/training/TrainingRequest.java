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
 * <p>{@code writePopulation} is a fifth, optional, monitoring-only field: at every checkpoint, the
 * output files keep only the non-dominated configurations of the meta-optimizer's population; when
 * {@code writePopulation} is true, the whole population is also written, to {@code
 * POPULATION_INDICATORS.csv} and {@code POPULATION_CONFIGURATIONS.csv}, so that its diversity can
 * be analyzed afterwards. It is false by default.
 *
 * <p>The meta-optimizer is selected by {@link MetaSearchConfig#algorithm()} and resolved by {@link
 * MetaAlgorithmRegistry}. The request file is part of the command-line contract of Evolver, stable
 * since 3.0 (see {@code docs/utilities/cli_tools.rst}): within 3.x it only gains optional fields.
 */
public record TrainingRequest(
    BaseLevelConfig baseLevel,
    MetaSearchConfig metaSearch,
    String outputDirectory,
    int writeFrequency,
    int statusFrequency,
    Integer frontPlotFrequency,
    boolean writePopulation) {

  /** A request that writes only the non-dominated configurations of each checkpoint. */
  public TrainingRequest(
      BaseLevelConfig baseLevel,
      MetaSearchConfig metaSearch,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency) {
    this(
        baseLevel,
        metaSearch,
        outputDirectory,
        writeFrequency,
        statusFrequency,
        frontPlotFrequency,
        false);
  }

  /**
   * The same request writing to another directory: what {@code TrainingRunnerMain --output-dir}
   * uses so that the independent replications of a training share one request file.
   *
   * @param outputDirectory where the results of the training are written
   * @return a copy of this request with that output directory
   */
  public TrainingRequest withOutputDirectory(String outputDirectory) {
    return new TrainingRequest(
        baseLevel,
        metaSearch,
        outputDirectory,
        writeFrequency,
        statusFrequency,
        frontPlotFrequency,
        writePopulation);
  }
}
