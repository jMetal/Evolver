package org.uma.evolver.picasso;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.ParameterManagement;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIRunner {

  public static void main(String[] args) throws IOException {
    CrossoverOperator<DoubleSolution> crossover;
    MutationOperator<DoubleSolution> mutation;

    int maxEvaluationsExternalAlgorithm = 5000;
    int maxEvaluationsInternalAlgorithm = 5000;
    int numberOfIndependentRunsExternalAlgorithm = 5 ;
    int numberOfIndependentRunsInternalAlgorithm = 9 ;
    int numberOfCores = 128;
    String problemName = "org.uma.jmetal.problem.multiobjective.zdt.ZDT2" ;
    String referenceParetoFrontFileName = "resources/referenceFronts/ZDT2.csv" ;
    String outputDirectoryName = "results" ;

    Problem<DoubleSolution> problem = ProblemFactory.<DoubleSolution>loadProblem(problemName);

    JMetalLogger.logger.info("maxEvaluationsExternalAlgorithm: " + maxEvaluationsExternalAlgorithm);
    JMetalLogger.logger.info("maxEvaluationsInternalAlgorithm: " + maxEvaluationsInternalAlgorithm);
    JMetalLogger.logger.info("numberOfIndependentRunsInternalAlgorithm: " + numberOfIndependentRunsInternalAlgorithm);
    JMetalLogger.logger.info("numberOfCores: " + numberOfCores);
    JMetalLogger.logger.info("problemName: " + problemName);
    JMetalLogger.logger.info("referenceParetoFrontFileName: " + referenceParetoFrontFileName);
    JMetalLogger.logger.info("outputDirectoryName: " + outputDirectoryName);

    int populationSize = 50;
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = (DoubleProblem) problem;
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, maxEvaluationsInternalAlgorithm);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        referenceParetoFrontFileName,
        indicators, numberOfIndependentRunsInternalAlgorithm);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor, indicators,
        outputDirectoryName + "/"+ problem.name());
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluationsExternalAlgorithm));

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(
        List.of(1000, 2000, 3000, 4000), outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    nsgaii.run();

    outputResultsManagement.updateSuffix("." + maxEvaluationsExternalAlgorithm + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());

    System.exit(0);
  }
}
