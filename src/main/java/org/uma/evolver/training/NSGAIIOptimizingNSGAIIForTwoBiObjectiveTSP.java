package org.uma.evolver.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.PermutationNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAC100TSP;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link PermutationNSGAII} using
 * problem {@link KroAB100TSP} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingNSGAIIForTwoBiObjectiveTSP {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int NUMBER_OF_CORES = 8;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;
  private static final int BASE_MAX_EVALUATIONS = 50000;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
  private static final int WRITE_FREQUENCY = 1;
  private static final int PLOT_UPDATE_FREQUENCY = 1;

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem
    List<Problem<PermutationSolution<Integer>>> trainingSet =
        List.of(new KroAB100TSP(), new KroAC100TSP());
    List<String> referenceFrontFileNames =
        List.of(
            "resources/referenceFrontsTSP/KroAB100TSP.csv",
            "resources/referenceFrontsTSP/KroAC100TSP.csv");

    // Step 2: Set the parameters for the algorithm to be configured
    List<QualityIndicator> indicators = List.of(new HypervolumeMinus(), new Epsilon());
    var maximumNumberOfEvaluations = List.of(BASE_MAX_EVALUATIONS, BASE_MAX_EVALUATIONS);
    ParameterSpace parameterSpace = new NSGAIIPermutationParameterSpace();
    var configurableAlgorithm = new PermutationNSGAII(BASE_POPULATION_SIZE, parameterSpace);

    EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<PermutationSolution<Integer>> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            configurableAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double builder
    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        new MetaNSGAIIBuilder(metaOptimizationProblem, new NSGAIIDoubleParameterSpace())
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .setNumberOfCores(NUMBER_OF_CORES)
            .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "NSGA-II",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "results/nsgaii/" + "MOTSPs");

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);

    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "NSGA-II, " + "MOTSPs",
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
            PLOT_UPDATE_FREQUENCY);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResults.updateEvaluations(META_MAX_EVALUATIONS);
    outputResults.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
