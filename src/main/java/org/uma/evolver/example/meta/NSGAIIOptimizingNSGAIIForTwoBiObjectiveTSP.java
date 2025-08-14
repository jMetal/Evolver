package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.PermutationNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationstrategy.FixedEvaluationsStrategy;
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
    var maximumNumberOfEvaluations = List.of(15000, 15000);
    ParameterSpace parameterSpace = new NSGAIIPermutationParameterSpace();
    var configurableAlgorithm = new PermutationNSGAII(100, parameterSpace);
    int numberOfIndependentRuns = 1;

    EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations) ;

    var metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            configurableAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double builder
    int maxEvaluations = 2000;
    int numberOfCores = 8;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
            new MetaNSGAIIBuilder(metaOptimizationProblem, parameterSpace)
                    .setMaxEvaluations(maxEvaluations)
                    .setNumberOfCores(numberOfCores)
                    .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "NSGA-II",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "RESULTS/NSGAII/" + "MOTSPs");

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(1, maxEvaluations, outputResults);

    var evaluationObserver = new EvaluationObserver(50);

    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "NSGA-II, " + "MOTSPs",
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
            1);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResults.updateEvaluations(maxEvaluations);
    outputResults.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
