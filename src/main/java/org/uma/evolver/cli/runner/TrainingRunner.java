package org.uma.evolver.cli.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeOutputResults;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.builder.MetaNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Runs a single NSGA-II-tunes-&lt;base-level-algorithm&gt; meta-optimization training job
 * described by a {@link TrainingRequest}, reusing the same builder/observer/output pipeline as
 * the {@code org.uma.evolver.example.training} reference examples — one pipeline per meta-level
 * encoding (see {@link FlatMetaSearchConfig}, {@link TreeMetaSearchConfig}).
 *
 * <p>Study prototype for uniformizing Evolver's training runners: this class replaces hardcoded
 * Java constants and an ad hoc {@code main(String[] args)} with a single structured input
 * ({@link TrainingRequest}) and a polled YAML status file, so it can be driven by an external
 * process (e.g. a GUI) without recompiling. It deliberately does not register a
 * {@code FrontPlotObserver}: this runner is meant to run headless.
 */
public class TrainingRunner {

  private record ResolvedTrainingSet(
      List<Problem<DoubleSolution>> problems,
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
          new YAMLParameterSpace(baseLevel.yamlParameterSpaceFile(), new DoubleParameterFactory());
      BaseLevelAlgorithm<DoubleSolution> baseAlgorithm =
          BaseAlgorithmRegistry.resolve(
              baseLevel.algorithmName(),
              baseLevel.populationSize(),
              baseLevelParameterSpace,
              baseLevel.extraConfig());
      EvaluationBudgetStrategy evaluationBudgetStrategy =
          new FixedEvaluationsStrategy(trainingSet.evaluationsToOptimize());

      return switch (metaSearch) {
        case FlatMetaSearchConfig flat ->
            runFlat(baseLevel, trainingSet, indicators, baseAlgorithm, evaluationBudgetStrategy, flat, statusWriter);
        case TreeMetaSearchConfig tree ->
            runTree(
                baseLevel,
                trainingSet,
                indicators,
                baseAlgorithm,
                baseLevelParameterSpace,
                evaluationBudgetStrategy,
                tree,
                statusWriter);
      };
    } catch (RuntimeException | IOException e) {
      statusWriter.write(RunStatusWriter.State.FAILED, 0, metaSearch.metaMaxEvaluations(), e.getMessage());
      throw e;
    }
  }

  private Path runFlat(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<DoubleSolution> baseAlgorithm,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      FlatMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter)
      throws IOException {
    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet.problems(),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns());

    MetaNSGAIIBuilder metaBuilder =
        new MetaNSGAIIBuilder(
                metaOptimizationProblem,
                new YAMLParameterSpace(metaSearch.metaYamlParameterSpaceFile(), new DoubleParameterFactory()))
            .setMaxEvaluations(metaSearch.metaMaxEvaluations())
            .setNumberOfCores(metaSearch.numberOfCores());
    if (metaSearch.metaPopulationSize() != null) {
      metaBuilder.setPopulationSize(metaSearch.metaPopulationSize());
    }
    if (metaSearch.mutationProbabilityFactor() != null) {
      metaBuilder.setMutationProbabilityFactor(metaSearch.mutationProbabilityFactor());
    }
    EvolutionaryAlgorithm<DoubleSolution> nsgaii = metaBuilder.build();

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName("NSGA-II")
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
            metaOptimizationProblem, trainingSet.label(), indicators, baseLevel.outputDirectory(), config);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(1, outputResults);
    var evaluationObserver = new EvaluationObserver(100);
    var statusFileObserver = new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), 100);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);
    nsgaii.observable().register(statusFileObserver);

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    nsgaii.run();

    outputResults.updateEvaluations(metaSearch.metaMaxEvaluations());
    outputResults.writeResultsToFiles(nsgaii.result());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(baseLevel.outputDirectory());
  }

  private Path runTree(
      BaseLevelConfig baseLevel,
      ResolvedTrainingSet trainingSet,
      List<QualityIndicator> indicators,
      BaseLevelAlgorithm<DoubleSolution> baseAlgorithm,
      YAMLParameterSpace baseLevelParameterSpace,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      TreeMetaSearchConfig metaSearch,
      RunStatusWriter statusWriter)
      throws IOException {
    var treeSolutionGenerator = new TreeSolutionGenerator(baseLevelParameterSpace);

    TreeMetaOptimizationProblem<DoubleSolution> metaProblem =
        new TreeMetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet.problems(),
            trainingSet.referenceFrontFileNames(),
            indicators,
            evaluationBudgetStrategy,
            baseLevel.numberOfIndependentRuns(),
            treeSolutionGenerator);

    var initialSolutionsCreation = new RandomSolutionsCreation<>(metaProblem, metaSearch.metaPopulationSize());
    var evaluation =
        new MultiThreadedEvaluation<DerivationTreeSolution>(metaSearch.numberOfCores(), metaProblem);
    var termination = new TerminationByEvaluations(metaSearch.metaMaxEvaluations());

    var crossover = new SubtreeCrossover(metaSearch.crossoverProbability());
    var mutation =
        new TreeMutation(
            metaSearch.mutationProbability(), metaSearch.mutationDistributionIndex(), treeSolutionGenerator);
    var variation = new CrossoverAndMutationVariation<>(metaSearch.metaOffspringSize(), crossover, mutation);

    var ranking = new FastNonDominatedSortRanking<DerivationTreeSolution>();
    var densityEstimator = new CrowdingDistanceDensityEstimator<DerivationTreeSolution>();
    var replacement = new RankingAndDensityEstimatorReplacement<>(ranking, densityEstimator);

    var rankingAndCrowdingComparator =
        new MultiComparator<>(
            List.of(
                Comparator.comparing(ranking::getRank),
                Comparator.comparing(densityEstimator::value).reversed()));
    var selection =
        new NaryTournamentSelection<DerivationTreeSolution>(
            2, variation.matingPoolSize(), rankingAndCrowdingComparator);

    EvolutionaryAlgorithm<DerivationTreeSolution> nsgaii =
        new EvolutionaryAlgorithm<>(
            "TreeNSGAII", initialSolutionsCreation, evaluation, termination, selection, variation, replacement);

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName("TreeNSGA-II")
            .metaMaxEvaluations(metaSearch.metaMaxEvaluations())
            .metaPopulationSize(metaSearch.metaPopulationSize())
            .numberOfCores(metaSearch.numberOfCores())
            .baseLevelAlgorithmName(baseLevel.algorithmName())
            .baseLevelPopulationSize(baseLevel.populationSize())
            .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(baseLevel.yamlParameterSpaceFile())
            .build();

    var outputResults =
        new TreeOutputResults(metaProblem, trainingSet.label(), indicators, baseLevel.outputDirectory(), config, 1);
    var evaluationObserver = new EvaluationObserver(100);
    var statusFileObserver = new StatusFileObserver(statusWriter, metaSearch.metaMaxEvaluations(), 100);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(outputResults);
    nsgaii.observable().register(statusFileObserver);

    statusWriter.write(RunStatusWriter.State.RUNNING, 0, metaSearch.metaMaxEvaluations());
    nsgaii.run();

    outputResults.writeFinalResults(nsgaii.result(), metaSearch.metaMaxEvaluations());

    statusWriter.write(
        RunStatusWriter.State.FINISHED, metaSearch.metaMaxEvaluations(), metaSearch.metaMaxEvaluations());

    return Path.of(baseLevel.outputDirectory());
  }

  private static ResolvedTrainingSet resolveTrainingSet(BaseLevelConfig baseLevel) {
    boolean hasNamedSet = baseLevel.trainingSetName() != null;
    boolean hasExplicitProblems = baseLevel.trainingProblemNames() != null;
    if (hasNamedSet == hasExplicitProblems) {
      throw new JMetalException(
          "Exactly one of trainingSetName or trainingProblemNames must be set in the training request");
    }

    if (hasNamedSet) {
      TrainingSet<DoubleSolution> namedSet = TrainingSetRegistry.resolve(baseLevel.trainingSetName());
      return new ResolvedTrainingSet(
          namedSet.problemList(), namedSet.referenceFronts(), namedSet.evaluationsToOptimize(), namedSet.name());
    }

    List<String> problemNames = baseLevel.trainingProblemNames();
    List<String> referenceFrontFileNames = baseLevel.trainingReferenceFrontFileNames();
    List<Integer> evaluations = baseLevel.trainingEvaluations();
    if (referenceFrontFileNames.size() != problemNames.size()
        || evaluations.size() != problemNames.size()) {
      throw new JMetalException(
          "trainingProblemNames, trainingReferenceFrontFileNames and trainingEvaluations must "
              + "have the same size");
    }

    return new ResolvedTrainingSet(
        problemNames.stream().map(ProblemRegistry::resolve).toList(),
        referenceFrontFileNames,
        evaluations,
        problemNames.size() == 1 ? problemNames.get(0) : "custom");
  }
}
