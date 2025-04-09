package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class for running {@link AsynchronousMultiThreadedNSGAII} as meta-optimizer to configure {@link
 * ConfigurableNSGAII} using problem {@link ZCAT1} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForProblemZCAT1Bias {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem (ZCAT1)
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZCAT1(2, 30, false, 1, true, false);

    String referenceFrontFileName = "resources/referenceFrontsCSV/ZCAT1.2D.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm =
        new ConfigurableNSGAII(problemWhoseConfigurationIsSearchedFor, 100, 50000);
    var configurableProblem =
        new MetaOptimizationProblem(configurableAlgorithm, referenceFrontFileName, indicators, 1);

    // Step 3: Set the parameters for the meta-optimizer (NSGAII)
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int maxEvaluations = 2000;
    int numberOfCores = 8;

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores,
            configurableProblem,
            populationSize,
            crossover,
            mutation,
            new TerminationByEvaluations(maxEvaluations));

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters =
        new OutputResultsManagementParameters(
            "NSGA-II",
            configurableProblem,
            problemWhoseConfigurationIsSearchedFor.name(),
            indicators,
                "RESULTS/AsyncNSGAII/"+problemWhoseConfigurationIsSearchedFor.name()+"/bias");

    var evaluationObserver = new EvaluationObserver(50);

    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 100, null, indicators.get(0).name(), indicators.get(1).name());

    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(50, maxEvaluations, outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(runTimeChartObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    long initTime = System.currentTimeMillis();
    nsgaii.run();
    long endTime = System.currentTimeMillis();

    // Step 6: Write results
    System.out.println("Total computing time: " + (endTime - initTime));

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());

    System.exit(0);
  }
}
