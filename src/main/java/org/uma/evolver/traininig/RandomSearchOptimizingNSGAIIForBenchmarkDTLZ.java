package org.uma.evolver.traininig;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.meta.MetaRandomSearchBuilder;
import org.uma.evolver.algorithm.meta.RandomSearch;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.util.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running Random Search as meta-optimizer to configure {@link DoubleNSGAII} using the
 * DTLZ problems as training set.
 *
 * @author Antonio J. Nebro
 */
public class RandomSearchOptimizingNSGAIIForBenchmarkDTLZ {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int NUMBER_OF_CORES = 8; // Parallel random search cores

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 100; // Notify every 100 meta-evals
  private static final int WRITE_FREQUENCY = 100;
  private static final int PLOT_UPDATE_FREQUENCY = 100;

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

    // Step 1: Select the target problem
    TrainingSet<DoubleSolution> trainingSetDescriptor = new DTLZ3DTrainingSet();

    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new InvertedGenerationalDistancePlus());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, parameterSpace);
    var maximumNumberOfEvaluations = trainingSetDescriptor.evaluationsToOptimize();

    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    // Step 3: Set up and configure the meta-optimizer (Random Search)
    RandomSearch<DoubleSolution> randomSearch =
        new MetaRandomSearchBuilder<>(metaOptimizationProblem)
            .setNumberOfCores(NUMBER_OF_CORES)
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .build();

    // Step 4: Create observers for the meta-optimizer
    String algorithmName = "RandomSearch";
    String problemName = trainingSetDescriptor.name();

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(algorithmName)
            .metaMaxEvaluations(META_MAX_EVALUATIONS)
            .metaPopulationSize(1) // RS doesn't have a population size per se
            .numberOfCores(NUMBER_OF_CORES)
            .baseLevelAlgorithmName("NSGA-II")
            .baseLevelPopulationSize(BASE_POPULATION_SIZE)
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(yamlParameterSpaceFile)
            .build();

    var outputResults =
        new ConsolidatedOutputResults(
            metaOptimizationProblem,
            problemName,
            indicators,
            "results/randomsearch/" + problemName,
            config);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "Random Search, " + trainingSetDescriptor.name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSetDescriptor.name(),
            PLOT_UPDATE_FREQUENCY);

    // Register observers!
    randomSearch.observable().register(evaluationObserver);
    randomSearch.observable().register(frontChartObserver);
    randomSearch.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    randomSearch.run();

    // Step 6: Write results
    outputResults.updateEvaluations(META_MAX_EVALUATIONS);
    outputResults.writeResultsToFiles(randomSearch.result());

    System.exit(0);
  }
}
