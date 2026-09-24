package org.uma.evolver.cli.training;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.algorithm.RandomSearch;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.output.ConsolidatedOutputResults;
import org.uma.evolver.meta.output.MetaOptimizerConfig;
import org.uma.evolver.meta.output.TreeOutputResults;
import org.uma.evolver.meta.output.WriteExecutionDataToFilesObserver;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Runs a single &lt;meta-optimizer&gt;-tunes-&lt;base-level-algorithm&gt; meta-optimization training job
 * described by a {@link TrainingRequest}, reusing the same builder/observer/output pipeline as
 * the {@code org.uma.evolver.example.training} reference examples — one pipeline per meta-level
 * encoding (see {@link FlatMetaSearchConfig}, {@link TreeMetaSearchConfig}).
 *
 * <p>Study prototype for uniformizing Evolver's training runners: this class replaces hardcoded
 * Java constants and an ad hoc {@code main(String[] args)} with a single structured input
 * ({@link TrainingRequest}) and a polled YAML status file, so it can be driven by an external
 * process (e.g. a GUI) without recompiling. It runs headless by default — a live
 * {@code FrontPlotObserver} is registered only when {@link TrainingRequest#frontPlotFrequency()}
 * is present, since an external process driving this runner would not want a Swing window
 * popping up on its machine.
 */
public class TrainingRunner {

  private record ResolvedTrainingSet(
      List<Problem<?>> problems,
      List<String> referenceFrontFileNames,
      List<Integer> evaluationsToOptimize,
      String label) {}

  /**
   * Runs the training job described by {@code request}, polling-friendly progress is written to
   * {@code statusFile} as the run progresses.
   *
   * @return the path to the output directory containing METADATA.txt, INDICATORS.csv and
   *     CONFIGURATIONS.csv
   */
  public Path run(TrainingRequest request, Path statusFile) throws IOException {
    BaseLevelConfig baseLevel = request.baseLevel();
    MetaSearchConfig metaSearch = request.metaSearch();
    RunStatusWriter statusWriter = new RunStatusWriter(statusFile);

    try {
      ResolvedTrainingSet trainingSet = resolveTrainingSet(baseLevel);
      List<QualityIndicator> indicators =
          baseLevel.indicatorNames().stream().map(IndicatorRegistry::resolve).toList();
      var baseLevelParameterSpace =
          BaseAlgorithmRegistry.resolveParameterSpace(
              baseLevel.encoding(), baseLevel.yamlParameterSpaceFile());
      BaseLevelAlgorithm<?> baseAlgorithm =
          BaseAlgorithmRegistry.resolve(
              baseLevel.algorithmName(),
              baseLevel.encoding(),
              baseLevel.populationSize(),
              baseLevelParameterSpace,
              baseLevel.extraConfig());
      EvaluationBudgetStrategy evaluationBudgetStrategy =
          new FixedEvaluationsStrategy(trainingSet.evaluationsToOptimize());

      return switch (metaSearch) {
        case FlatMetaSearchConfig flat ->
            switch (MetaAlgorithmRegistry.familyOf(flat.algorithm())) {
              case EVOLUTIONARY ->
                  runFlat(
                      baseLevel,
                      trainingSet,
                      indicators,
                      baseAlgorithm,
                      evaluationBudgetStrategy,
                      flat,
                      statusWriter,
                      request.outputDirectory(),
                      request.writeFrequency(),
                      request.statusFrequency(),
                      request.frontPlotFrequency());
              case ASYNCHRONOUS ->
                  runFlatAsync(
                      baseLevel,
                      trainingSet,
                      indicators,
                      baseAlgorithm,
                      evaluationBudgetStrategy,
                      flat,
                      statusWriter,
                      request.outputDirectory(),
                      request.writeFrequency(),
                      request.statusFrequency(),
                      request.frontPlotFrequency());
              case PARTICLE_SWARM ->
                  runFlatPso(
                      baseLevel,
                      trainingSet,
                      indicators,
                      baseAlgorithm,
                      evaluationBudgetStrategy,
                      flat,
                      statusWriter,
                      request.outputDirectory(),
                      request.writeFrequency(),
                      request.statusFrequency(),
                      request.frontPlotFrequency());
              case RANDOM_SEARCH ->
                  runFlatRandomSearch(
                      baseLevel,
                      trainingSet,
                      indicators,
                      baseAlgorithm,
                      evaluationBudgetStrategy,
                      flat,
                      statusWriter,
                      request.outputDirectory(),
                      request.writeFrequency(),
                      request.statusFrequency(),
                      request.frontPlotFrequency());
            };
        case TreeMetaSearchConfig tree ->
            runTree(
                baseLevel,
                trainingSet,
                indicators,
                baseAlgorithm,
                baseLevelParameterSpace,
                evaluationBudgetStrategy,
                tree,
                statusWriter,
                request.outputDirectory(),
                request.writeFrequency(),
                request.statusFrequency(),
                request.frontPlotFrequency());
      };
    } catch (RuntimeException | IOException e) {
      statusWriter.write(RunStatusWriter.State.FAILED, 0, metaSearch.metaMaxEvaluations(), e.getMessage());
      throw e;
    }
  }

  private <S extends Solution<?>> Path runFlat(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<S> baseAlgorithm,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      FlatMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency)
      throws IOException {
    MetaOptimizationProblem<S> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            problemsOf(trainingSet),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns());

    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        MetaAlgorithmRegistry.resolveFlat(metaSearch.algorithm(), metaOptimizationProblem, metaSearch);

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(metaSearch.algorithm())
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            .metaPopulationSize(metaSearch.metaPopulationSize() == null ? 0 : metaSearch.metaPopulationSize())
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem, trainingSet.label(), indicators, outputDirectory, config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(writeFrequency, outputResults);
    var evaluationObserver = new EvaluationObserver(statusFrequency);
    var statusFileObserver =
        new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), statusFrequency);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);
    nsgaii.observable().register(statusFileObserver);
    registerFrontPlotObserverIfRequested(
        nsgaii.observable(), frontPlotFrequency, metaSearch.algorithm(), indicators, trainingSet.label());

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    nsgaii.run();

    outputResults.updateEvaluations(metaSearch.metaMaxEvaluations());
    outputResults.writeResultsToFiles(nsgaii.result());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(outputDirectory);
  }

  private <S extends Solution<?>> Path runFlatAsync(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<S> baseAlgorithm,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      FlatMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency)
      throws IOException {
    MetaOptimizationProblem<S> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            problemsOf(trainingSet),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns());

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        MetaAlgorithmRegistry.resolveFlatAsync(
            metaSearch.algorithm(), metaOptimizationProblem, metaSearch);

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(metaSearch.algorithm())
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            .metaPopulationSize(metaSearch.metaPopulationSize() == null ? 0 : metaSearch.metaPopulationSize())
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem, trainingSet.label(), indicators, outputDirectory, config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(writeFrequency, outputResults);
    var evaluationObserver = new EvaluationObserver(statusFrequency);
    var statusFileObserver =
        new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), statusFrequency);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);
    nsgaii.observable().register(statusFileObserver);
    registerFrontPlotObserverIfRequested(
        nsgaii.observable(), frontPlotFrequency, metaSearch.algorithm(), indicators, trainingSet.label());

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    nsgaii.run();

    outputResults.updateEvaluations(metaSearch.metaMaxEvaluations());
    outputResults.writeResultsToFiles(nsgaii.result());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(outputDirectory);
  }

  private <S extends Solution<?>> Path runFlatPso(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<S> baseAlgorithm,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      FlatMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency)
      throws IOException {
    MetaOptimizationProblem<S> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            problemsOf(trainingSet),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns());

    ParticleSwarmOptimizationAlgorithm smpso =
        MetaAlgorithmRegistry.resolveFlatPso(
            metaSearch.algorithm(), metaOptimizationProblem, metaSearch);

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(metaSearch.algorithm())
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            .metaPopulationSize(metaSearch.metaPopulationSize() == null ? 0 : metaSearch.metaPopulationSize())
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem, trainingSet.label(), indicators, outputDirectory, config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(writeFrequency, outputResults);
    var evaluationObserver = new EvaluationObserver(statusFrequency);
    var statusFileObserver =
        new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), statusFrequency);

    smpso.observable().register(evaluationObserver);
    smpso.observable().register(writeExecutionDataToFilesObserver);
    smpso.observable().register(statusFileObserver);
    registerFrontPlotObserverIfRequested(
        smpso.observable(), frontPlotFrequency, metaSearch.algorithm(), indicators, trainingSet.label());

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    smpso.run();

    outputResults.updateEvaluations(metaSearch.metaMaxEvaluations());
    outputResults.writeResultsToFiles(smpso.result());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(outputDirectory);
  }

  private <S extends Solution<?>> Path runFlatRandomSearch(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<S> baseAlgorithm,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      FlatMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency)
      throws IOException {
    MetaOptimizationProblem<S> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            problemsOf(trainingSet),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns());

    RandomSearch<DoubleSolution> randomSearch =
        MetaAlgorithmRegistry.resolveFlatRandomSearch(
            metaSearch.algorithm(), metaOptimizationProblem, metaSearch);

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(metaSearch.algorithm())
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            .metaPopulationSize(0) // RandomSearch has no population concept
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem, trainingSet.label(), indicators, outputDirectory, config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(writeFrequency, outputResults);
    var evaluationObserver = new EvaluationObserver(statusFrequency);
    var statusFileObserver =
        new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), statusFrequency);

    randomSearch.observable().register(evaluationObserver);
    randomSearch.observable().register(writeExecutionDataToFilesObserver);
    randomSearch.observable().register(statusFileObserver);
    registerFrontPlotObserverIfRequested(
        randomSearch.observable(),
        frontPlotFrequency,
        metaSearch.algorithm(),
        indicators,
        trainingSet.label());

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    randomSearch.run();

    outputResults.updateEvaluations(metaSearch.metaMaxEvaluations());
    outputResults.writeResultsToFiles(randomSearch.result());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(outputDirectory);
  }

  private <S extends Solution<?>> Path runTree(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<S> baseAlgorithm,
      YAMLParameterSpace baseLevelParameterSpace,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      TreeMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter,
      String outputDirectory,
      int writeFrequency,
      int statusFrequency,
      Integer frontPlotFrequency)
      throws IOException {
    MetaAlgorithmRegistry.validateTreeAlgorithm(metaSearch.algorithm());
    var treeSolutionGenerator = new TreeSolutionGenerator(baseLevelParameterSpace);

    TreeMetaOptimizationProblem<S> metaProblem =
        new TreeMetaOptimizationProblem<>(
            baseAlgorithm,
            problemsOf(trainingSet),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns(),
            treeSolutionGenerator);

    TreeEngine engine = resolveTreeEngine(metaSearch, metaProblem);
    boolean randomSearch =
        MetaAlgorithmRegistry.familyOf(metaSearch.algorithm())
            == MetaAlgorithmRegistry.Family.RANDOM_SEARCH;

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName("Tree" + metaSearch.algorithm())
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            // RandomSearch has no population concept
            .metaPopulationSize(randomSearch ? 0 : metaSearch.metaPopulationSize())
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new TreeOutputResults(
            metaProblem, trainingSet.label(), indicators, outputDirectory, config, writeFrequency);
    var evaluationObserver = new EvaluationObserver(statusFrequency);
    var statusFileObserver =
        new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), statusFrequency);

    engine.observable().register(evaluationObserver);
    engine.observable().register(outputResults);
    engine.observable().register(statusFileObserver);
    registerFrontPlotObserverIfRequested(
        engine.observable(), frontPlotFrequency, metaSearch.algorithm(), indicators, trainingSet.label());

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    engine.run().run();

    outputResults.writeFinalResults(engine.result().get(), metaSearch.metaMaxEvaluations());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(outputDirectory);
  }

  /**
   * The parts of a tree-encoding meta-optimizer {@link #runTree} needs. The registered engines
   * ({@link EvolutionaryAlgorithm}, {@link RandomSearch}) share no common supertype exposing
   * {@code run()}/{@code result()}/{@code observable()}, so they are adapted to this record.
   */
  private record TreeEngine(
      Runnable run,
      Supplier<List<DerivationTreeSolution>> result,
      Observable<Map<String, Object>> observable) {}

  private static TreeEngine resolveTreeEngine(
      TreeMetaSearchConfig metaSearch, TreeMetaOptimizationProblem<?> metaProblem) {
    return switch (MetaAlgorithmRegistry.familyOf(metaSearch.algorithm())) {
      case EVOLUTIONARY -> {
        EvolutionaryAlgorithm<DerivationTreeSolution> algorithm =
            MetaAlgorithmRegistry.resolveTree(metaSearch.algorithm(), metaProblem, metaSearch);
        yield new TreeEngine(algorithm::run, algorithm::result, algorithm.observable());
      }
      case RANDOM_SEARCH -> {
        RandomSearch<DerivationTreeSolution> algorithm =
            MetaAlgorithmRegistry.resolveTreeRandomSearch(
                metaSearch.algorithm(), metaProblem, metaSearch);
        yield new TreeEngine(algorithm::run, algorithm::result, algorithm.observable());
      }
      default ->
          throw new JMetalException(
              "Meta-optimizer algorithm " + metaSearch.algorithm() + " does not support tree");
    };
  }

  /**
   * Registers a live {@link FrontPlotObserver} only when {@code frontPlotFrequency} is present —
   * opt-in, see {@link TrainingRequest#frontPlotFrequency()}. Works the same regardless of the
   * meta-optimizer's solution type ({@code DoubleSolution} or {@code DerivationTreeSolution}):
   * {@code FrontPlotObserver} only needs {@code Solution<?>}, and every registered engine exposes
   * the same {@code Observable<Map<String, Object>>} shape (see {@link MetaAlgorithmRegistry}).
   */
  private static void registerFrontPlotObserverIfRequested(
      Observable<Map<String, Object>> observable,
      Integer frontPlotFrequency,
      String title,
      List<QualityIndicator> indicators,
      String legend) {
    if (frontPlotFrequency != null) {
      observable.register(
          new FrontPlotObserver<Solution<?>>(
              title, indicators.get(0).name(), indicators.get(1).name(), legend, frontPlotFrequency));
    }
  }

  private static ResolvedTrainingSet resolveTrainingSet(BaseLevelConfig baseLevel) {
    List<ProblemSpec> problemSpecs = baseLevel.trainingProblemNames();
    List<String> referenceFrontFileNames = baseLevel.trainingReferenceFrontFileNames();
    List<Integer> evaluations = baseLevel.trainingEvaluations();
    if (referenceFrontFileNames.size() != problemSpecs.size()
        || evaluations.size() != problemSpecs.size()) {
      throw new JMetalException(
          "trainingProblemNames, trainingReferenceFrontFileNames and trainingEvaluations must "
              + "have the same size");
    }

    return new ResolvedTrainingSet(
        problemSpecs.stream().map(ProblemRegistry::resolve).toList(),
        referenceFrontFileNames,
        evaluations,
        problemSpecs.size() == 1 ? problemSpecs.get(0).displayName() : "custom");
  }

  /**
   * Casts a resolved training set's problems to the base-level algorithm's own solution type
   * {@code S}, trusting that {@link BaseLevelConfig#encoding()} is coherent with the problems
   * listed in {@link BaseLevelConfig#trainingProblemNames()} — same trust model {@link
   * ProblemRegistry} already uses for reflective problem resolution: a mismatch surfaces as a
   * {@code ClassCastException} once a base-level run actually evaluates a solution, not here.
   */
  @SuppressWarnings("unchecked")
  private static <S extends Solution<?>> List<Problem<S>> problemsOf(
      ResolvedTrainingSet trainingSet) {
    return (List<Problem<S>>) (List<?>) trainingSet.problems();
  }
}
