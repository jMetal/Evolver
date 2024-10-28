package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MultiFocusMetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class for running {@link AsynchronousMultiThreadedNSGAII} as meta-optimizer to configure
 * {@link ConfigurableNSGAII} using problem {@link DTLZ1} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForProblemsWFG {

  public static void main(String[] args) throws IOException {

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    List<DoubleProblem> trainingSet = List.of(new WFG1(), new WFG2(), new WFG3(), new WFG4(),
            new WFG5(), new WFG6(), new WFG7(), new WFG8(), new WFG9());
    List<String> referenceFrontFileNames = List.of(
            "resources/referenceFronts/WFG1.2D.csv",
            "resources/referenceFronts/WFG2.2D.csv",
            "resources/referenceFronts/WFG3.2D.csv",
            "resources/referenceFronts/WFG4.2D.csv",
            "resources/referenceFronts/WFG5.2D.csv",
            "resources/referenceFronts/WFG6.2D.csv",
            "resources/referenceFronts/WFG7.2D.csv",
            "resources/referenceFronts/WFG8.2D.csv",
            "resources/referenceFronts/WFG9.2D.csv");
    List<Integer> maxEvaluationsPerProblem = List.of(10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000);

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableMOPSO})
    ConfigurableAlgorithmBuilder configurableAlgorithm =
            new ConfigurableNSGAII(new FakeDoubleProblem(), 100, 10000);
    var configurableProblem = new MultiFocusMetaOptimizationProblem(configurableAlgorithm,
            trainingSet, referenceFrontFileNames,
            indicators, maxEvaluationsPerProblem, 1);

    // Step 3: Set the parameters for the meta-optimizer (NSGAII)
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int maxEvaluations = 3000;
    int numberOfCores = 12 ;

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluations));

    // Step 4: Create observers for the meta-optimizer
    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
            "AsyncNSGA-II", configurableProblem, "ZDT", indicators,
            "RESULTS/AsyncNSGAII/WFG");

    var evaluationObserver = new EvaluationObserver(100);

    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "AsyncNSGA-II",
            80, 100, null, indicators.get(0).name(), indicators.get(1).name());

    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(100,
        maxEvaluations, outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(runTimeChartObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    long initTime = System.currentTimeMillis();
    nsgaii.run();
    long endTime = System.currentTimeMillis();

    // Step 6: Write results
    System.out.println("Total computing time: " + (endTime - initTime)) ;

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());

    System.exit(0);
  }
}
