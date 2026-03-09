package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.builder.MetaAsyncGeneticAlgorithmBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RWA3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedGeneticAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FitnessPlotObserver;

/**
 * Class for running an asynchronous Genetic Algorithm as meta-optimizer to configure
 * {@link DoubleNSGAII} using the RWA problems as training set.
 *
 * <p>This example uses the Epsilon (EP) quality indicator as the single objective to optimize.</p>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncGeneticAlgorithmOptimizingNSGAIIForBenchmarkRWA3D {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 3000;
  private static final int META_POPULATION_SIZE = 50;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 30;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 500;
  private static final int WRITE_FREQUENCY = 100;

  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.err.println(
          "Usage: AsyncGeneticAlgorithmOptimizingNSGAIIForBenchmarkRWA3D "
              + "<referenceFrontDirectory> <maximumNumberOfEvaluations> <numberOfCores> <resultsDirectory>");
      System.exit(1);
    }

    String referenceFrontDirectory = args[0];
    int baseMaxEvaluations = Integer.parseInt(args[1]);
    int numberOfCores = Integer.parseInt(args[2]);
    String resultsDirectory = args[3];

    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

    // Step 1: Select the target problem
    TrainingSet<DoubleSolution> trainingSetDescriptor =
        new RWA3DTrainingSet()
            .setReferenceFrontDirectory(referenceFrontDirectory)
            .setEvaluationsToOptimize(baseMaxEvaluations);

    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

    // Step 2: Set the parameters for the algorithm to be configured
    List<QualityIndicator> indicators = List.of(new Epsilon());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, parameterSpace);
    var maximumNumberOfEvaluations = trainingSetDescriptor.evaluationsToOptimize();
    int numberOfIndependentRuns = NUMBER_OF_INDEPENDENT_RUNS;

    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (Async Genetic Algorithm)
    AsynchronousMultiThreadedGeneticAlgorithm<DoubleSolution> geneticAlgorithm =
        new MetaAsyncGeneticAlgorithmBuilder(metaOptimizationProblem)
            .setNumberOfCores(numberOfCores)
            .setPopulationSize(META_POPULATION_SIZE)
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .setObjectiveIndex(0)
            .build();

    // Step 4: Create observers for the meta-optimizer
    String algorithmName = "AsyncGA";
    String problemName = trainingSetDescriptor.name();

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(algorithmName)
            .metaMaxEvaluations(META_MAX_EVALUATIONS)
            .metaPopulationSize(META_POPULATION_SIZE)
            .numberOfCores(numberOfCores)
            .baseLevelAlgorithmName("NSGA-II")
            .baseLevelPopulationSize(BASE_POPULATION_SIZE)
            .baseLevelMaxEvaluations(maximumNumberOfEvaluations.get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(yamlParameterSpaceFile)
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem, problemName, indicators, resultsDirectory, config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    String indicatorName = indicators.get(0).name();
    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var fitnessObserver =
        new FitnessPlotObserver<DoubleSolution>(
            indicatorName, "Evaluations", indicatorName, algorithmName, EVALUATION_OBSERVER_FREQUENCY);

    geneticAlgorithm.observable().register(evaluationObserver);
    geneticAlgorithm.observable().register(fitnessObserver);
    geneticAlgorithm.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    geneticAlgorithm.run();

    // Step 6: Write results
    outputResults.updateEvaluations(META_MAX_EVALUATIONS);
    outputResults.writeResultsToFiles(geneticAlgorithm.result());

    System.exit(0);
  }
}
