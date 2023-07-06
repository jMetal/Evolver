package org.uma.evolver;

import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.factory.ConfigurableProblemFactory;
import org.uma.evolver.factory.OptimizationAlgorithmFactory;
import org.uma.evolver.factory.ProblemFactory;
import org.uma.evolver.factory.QualityIndicatorFactory;
import org.uma.evolver.problem.ConfigurableAlgorithmBaseProblem;
import org.uma.evolver.problem.ConfigurableAlgorithmMultiProblem;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for running a meta-optimizer to auto-design an algorithm for a specific set of problems
 *
 * @author José Francisco Aldana Martín (jfaldanam@uma.es)
 */
public class MetaRunner {

  public static void main(String[] args) throws IOException {
    /*
Expected arguments:
 - External algorithm arguments:
   * 0 - meta-optimizer algorithm: The name of the meta-optimizer algorithm.
   * 1 - meta-optimizer population: The population size for the meta-optimizer algorithm.
   * 2 - meta-optimizer max evaluations: The maximum number of evaluations for the meta-optimizer algorithm.
   * 3 - independent runs: The number of independent runs for the meta-optimizer algorithm.
   * 4 - indicators names: The names of the indicators used as objectives by the meta-optimizer algorithm.
   * 5 - output directory: The directory where the output will be saved.
 - Internal algorithm arguments:
   * 6 - configurable algorithm: The name of the internal configurable algorithm.
   * 7 - population: The population size for the internal algorithm.
   * 8 - problem names: The names of the problems to be solved. It can be provided as a list of comma separated values E.g.: "ZDT1,ZDT4"
   * 9 - reference front file name: The file name of the reference front. It can be provided as a list of comma separated values E.g.: "ZDT1.csv,ZDT4.csv"
   * 10 - max number of evaluations: The maximum number of evaluations for the internal algorithm. It can be provided as a list of comma separated values E.g.: "8000,16000"
 - Optional specific arguments:
   * 11 - weight vector files directory: The directory containing weight vector files. Only used for the MOEAD internal algorithm.
     */

    String externalAlgorithm = args[0];
    String externalPopulationArg = args[1];
    String externalMaxEvaluationsArg = args[2];
    String independentRunsArg = args[3];
    String indicatorsNames = args[4];
    String outputDirectory = args[5];

    String configurableAlgorithm = args[6];
    String populationArg = args[7];
    String problemName = args[8];
    String referenceFrontFileName = args[9];
    String maxNumberOfEvaluations = args[10];
    String weightVectorFilesDirectory;
    if (args.length >= 12) {
      weightVectorFilesDirectory = args[11];
    } else {
      weightVectorFilesDirectory = null;
    }

    int population = Integer.parseInt(populationArg);
    int independentRuns = Integer.parseInt(independentRunsArg);
    int externalPopulation = Integer.parseInt(externalPopulationArg);
    int externalMaxEvaluations = Integer.parseInt(externalMaxEvaluationsArg);

    List<QualityIndicator> indicators = Arrays.asList(
        QualityIndicatorFactory.getIndicators(indicatorsNames));

    DoubleProblem problem;
    int internalMaxEvaluations;
    if (problemName.contains(",")) {
      problem = ProblemFactory.getProblem(
          "ZDT1"); // This is a dummy problem for the multi-problem cases
      internalMaxEvaluations = -1;
    } else {
      Check.that(!maxNumberOfEvaluations.contains(","),
          "If there is only one problem, you can't have several maxNumberOfEvaluations");
      Check.that(!referenceFrontFileName.contains(","),
          "If there is only one problem, you can't have several referenceFrontFileName");
      problem = ProblemFactory.getProblem(problemName);
      internalMaxEvaluations = Integer.parseInt(maxNumberOfEvaluations);
    }

    ConfigurableAlgorithmBuilder configurableAlgorithmBuilder;
    if (weightVectorFilesDirectory == null) {
      configurableAlgorithmBuilder = ConfigurableProblemFactory.getProblem(configurableAlgorithm,
          problem, population, internalMaxEvaluations);
    } else {
      configurableAlgorithmBuilder = ConfigurableProblemFactory.getProblem(configurableAlgorithm,
          problem, population, internalMaxEvaluations, weightVectorFilesDirectory);
    }

    // Define configurable problem
    // Depends on the number of problems
    ConfigurableAlgorithmBaseProblem configurableProblem;
    if (problemName.contains(",")) {
      List<DoubleProblem> problems = List.of(ProblemFactory.getProblems(problemName));

      List<Integer> maxNumberOfEvaluationsPerProblem = Arrays.stream(
              maxNumberOfEvaluations.split(","))
          .map(Integer::parseInt)
          .collect(Collectors.toList());
      configurableProblem = new ConfigurableAlgorithmMultiProblem(configurableAlgorithmBuilder,
          problems,
          List.of(referenceFrontFileName.split(",")),
          indicators,
          maxNumberOfEvaluationsPerProblem,
          independentRuns);
    } else {
      configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithmBuilder,
          referenceFrontFileName,
          indicators, independentRuns);
    }

    // Create external optimization algorithm
    MetaOptimizer externalOptimizationAlgorithm = OptimizationAlgorithmFactory.getAlgorithm(
        externalAlgorithm, configurableProblem, externalPopulation, externalMaxEvaluations);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        externalAlgorithm, configurableProblem,
        problemName,
        indicators, outputDirectory
    );

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(externalAlgorithm, indicators.get(0).name(),
            indicators.get(1).name(),
            problemName,
            50);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(25,
        externalMaxEvaluations, outputResultsManagement);

    externalOptimizationAlgorithm.observable().register(evaluationObserver);
    externalOptimizationAlgorithm.observable().register(frontChartObserver);
    externalOptimizationAlgorithm.observable().register(writeExecutionDataToFilesObserver);

    externalOptimizationAlgorithm.run();

    JMetalLogger.logger.info(
        () -> "Total computing time: " + externalOptimizationAlgorithm.totalComputingTime());

    outputResultsManagement.updateSuffix("." + externalMaxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(externalOptimizationAlgorithm.result());

    System.exit(0);
  }
}
