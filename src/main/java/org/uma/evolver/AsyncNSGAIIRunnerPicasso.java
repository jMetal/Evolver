package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIRunnerPicasso {

  public static void main(String[] args) throws IOException {
    CrossoverOperator<DoubleSolution> crossover;
    MutationOperator<DoubleSolution> mutation;

    int populationSize = 50;
    int maxEvaluations = 3000;
    int numberOfCores = 128;

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 15000);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        "resources/referenceFronts/ZDT4.csv",
        indicators, 3);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor, indicators,
        "asyncOutputFiles128");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    long initTime = System.currentTimeMillis();

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluations));

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(
        List.of(1000, 2000), outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    nsgaii.run();

    long endTime = System.currentTimeMillis();

    System.out.println("Total computing time: " + (endTime - initTime)) ;

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());
  }
}