package org.uma.evolver.cli.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.builder.MetaNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;

/**
 * Runs a single NSGA-II-tunes-&lt;base-level-algorithm&gt; meta-optimization training job
 * described by a {@link TrainingRequest}, reusing the same builder/observer/output pipeline as
 * the {@code org.uma.evolver.example.training} reference examples.
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
    RunStatusWriter statusWriter = new RunStatusWriter(statusFile);

    try {
      ResolvedTrainingSet trainingSet = resolveTrainingSet(request);
      List<QualityIndicator> indicators =
          request.indicatorNames().stream().map(IndicatorRegistry::resolve).toList();

      var baseLevelParameterSpace =
          new YAMLParameterSpace(request.baseLevelYamlParameterSpaceFile(), new DoubleParameterFactory());
      BaseLevelAlgorithm<DoubleSolution> baseAlgorithm =
          BaseAlgorithmRegistry.resolve(
              request.baseLevelAlgorithmName(),
              request.baseLevelPopulationSize(),
              baseLevelParameterSpace,
              request.baseLevelExtraConfig());

      EvaluationBudgetStrategy evaluationBudgetStrategy =
          new FixedEvaluationsStrategy(trainingSet.evaluationsToOptimize());

      MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
          new MetaOptimizationProblem<>(
              baseAlgorithm,
              trainingSet.problems(),
              trainingSet.referenceFrontFileNames(),
              indicators,
              evaluationBudgetStrategy,
              request.numberOfIndependentRuns());

      MetaNSGAIIBuilder metaBuilder =
          new MetaNSGAIIBuilder(
                  metaOptimizationProblem,
                  new YAMLParameterSpace(request.metaYamlParameterSpaceFile(), new DoubleParameterFactory()))
              .setMaxEvaluations(request.metaMaxEvaluations())
              .setNumberOfCores(request.numberOfCores());
      if (request.metaPopulationSize() != null) {
        metaBuilder.setPopulationSize(request.metaPopulationSize());
      }
      if (request.mutationProbabilityFactor() != null) {
        metaBuilder.setMutationProbabilityFactor(request.mutationProbabilityFactor());
      }
      EvolutionaryAlgorithm<DoubleSolution> nsgaii = metaBuilder.build();

      MetaOptimizerConfig config =
          MetaOptimizerConfig.builder()
              .metaOptimizerName("NSGA-II")
              .metaMaxEvaluations(request.metaMaxEvaluations())
              .metaPopulationSize(request.metaPopulationSize() == null ? 0 : request.metaPopulationSize())
              .numberOfCores(request.numberOfCores())
              .baseLevelAlgorithmName(request.baseLevelAlgorithmName())
              .baseLevelPopulationSize(request.baseLevelPopulationSize())
              .baseLevelMaxEvaluations(trainingSet.evaluationsToOptimize().get(0))
              .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
              .yamlParameterSpaceFile(request.baseLevelYamlParameterSpaceFile())
              .build();

      var outputResults =
          new ConsolidatedOutputResults(
              metaOptimizationProblem,
              trainingSet.label(),
              indicators,
              request.outputDirectory(),
              config);

      var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(1, outputResults);
      var evaluationObserver = new EvaluationObserver(100);
      var statusFileObserver = new StatusFileObserver(statusWriter, request.metaMaxEvaluations(), 100);

      nsgaii.observable().register(evaluationObserver);
      nsgaii.observable().register(writeExecutionDataToFilesObserver);
      nsgaii.observable().register(statusFileObserver);

      statusWriter.write(RunStatusWriter.State.RUNNING, 0, request.metaMaxEvaluations());
      nsgaii.run();

      outputResults.updateEvaluations(request.metaMaxEvaluations());
      outputResults.writeResultsToFiles(nsgaii.result());

      statusWriter.write(
          RunStatusWriter.State.FINISHED, request.metaMaxEvaluations(), request.metaMaxEvaluations());

      return Path.of(request.outputDirectory());
    } catch (RuntimeException | IOException e) {
      statusWriter.write(RunStatusWriter.State.FAILED, 0, request.metaMaxEvaluations(), e.getMessage());
      throw e;
    }
  }

  private static ResolvedTrainingSet resolveTrainingSet(TrainingRequest request) {
    boolean hasNamedSet = request.trainingSetName() != null;
    boolean hasExplicitProblems = request.trainingProblemNames() != null;
    if (hasNamedSet == hasExplicitProblems) {
      throw new JMetalException(
          "Exactly one of trainingSetName or trainingProblemNames must be set in the training request");
    }

    if (hasNamedSet) {
      TrainingSet<DoubleSolution> namedSet = TrainingSetRegistry.resolve(request.trainingSetName());
      return new ResolvedTrainingSet(
          namedSet.problemList(), namedSet.referenceFronts(), namedSet.evaluationsToOptimize(), namedSet.name());
    }

    List<String> problemNames = request.trainingProblemNames();
    List<String> referenceFrontFileNames = request.trainingReferenceFrontFileNames();
    List<Integer> evaluations = request.trainingEvaluations();
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
