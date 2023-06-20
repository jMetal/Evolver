package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmMultiProblem;
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
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIMultiProblemRunner {

  public static void main(String[] args) throws IOException {
    CrossoverOperator<DoubleSolution> crossover;
    MutationOperator<DoubleSolution> mutation;

    int populationSize = 50;
    int maxEvaluations = 3000;
    int numberOfCores = 128;

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT1();
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 8000);
    var configurableProblem = new ConfigurableAlgorithmMultiProblem(configurableAlgorithm,
        List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6()),
        List.of("resources/referenceFronts/ZDT1.csv",
            "resources/referenceFronts/ZDT2.csv",
            "resources/referenceFronts/ZDT3.csv",
            "resources/referenceFronts/ZDT4.csv",
            "resources/referenceFronts/ZDT6.csv"),
        indicators,
        List.of(8000, 8000, 8000, 8000, 8000), 3);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor, indicators,
        "asyncOutputFilesZDT");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    long initTime = System.currentTimeMillis();

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluations));

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);

    /*
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II",
            80, 100, null, indicators.get(0).name(), indicators.get(1).name());
    */
    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(
        List.of(500, 1000, 1500, 2000, 2500), outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    //nsgaii.getObservable().register(runTimeChartObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    nsgaii.run();

    long endTime = System.currentTimeMillis();

    System.out.println("Total computing time: " + (endTime - initTime)) ;

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());
  }
}
