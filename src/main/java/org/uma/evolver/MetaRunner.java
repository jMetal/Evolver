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
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Class for running a meta-optimizer to auto-design an algorithm for a specific set of problems
 *
 * @author José Francisco Aldana Martín (jfaldanam@uma.es)
 */
public class MetaRunner {

  public static void main(String[] args) throws IOException {
    // Parameters
    // TODO: Must be parsed from main args
    String configurableAlgorithm = "NSGAII";
    String problemName = "ZDT1";
    String indicatorsNames = "Epsilon,Spread";
    String referenceFrontFileName = "/home/jfaldanam/Software/jfaldanam-phd/Evolver/resources/referenceFronts/ZDT1.csv";

    int maxNumberOfEvaluations = 50;
    int population = 100;
    int independentRuns = 1;
    int externalPopulation = 50;
    int externalMaxEvaluations = 3000;
    String externalAlgorithm = "GGA";
    String outputDirectory = "TEST";
    String weightVectorFilesDirectory = null;
    // End of Parameters

    List<QualityIndicator> indicators = Arrays.asList(QualityIndicatorFactory.getIndicators(indicatorsNames));

    DoubleProblem problem;
    if (problemName.contains(",")) {
      problem = ProblemFactory.getProblem("ZDT1"); // This is a dummy problem for the multi-problem cases
    } else {
      problem = ProblemFactory.getProblem(problemName);
    }

    ConfigurableAlgorithmBuilder configurableAlgorithmBuilder;
    if (weightVectorFilesDirectory == null)
      configurableAlgorithmBuilder= ConfigurableProblemFactory.getProblem(configurableAlgorithm,
              problem, population, maxNumberOfEvaluations);
    else
      configurableAlgorithmBuilder = ConfigurableProblemFactory.getProblem(configurableAlgorithm,
              problem, population, maxNumberOfEvaluations, weightVectorFilesDirectory);

    // Define configurable problem
    // Depends on the number of problems
    ConfigurableAlgorithmBaseProblem configurableProblem;
    if (problemName.contains(",")) {
      List<DoubleProblem> problems = List.of(ProblemFactory.getProblems(problemName));
      configurableProblem = new ConfigurableAlgorithmMultiProblem(configurableAlgorithmBuilder,
              problems,
              List.of(referenceFrontFileName.split(",")),
              indicators,
              List.of(8000, 8000), // TODO: Make configurable
              independentRuns);
    } else {
      configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithmBuilder,
              referenceFrontFileName,
              indicators, independentRuns);
    }

    // Create external optimization algorithm
    EvolutionaryAlgorithm<DoubleSolution> externalOptimizationAlgorithm = OptimizationAlgorithmFactory.getAlgorithm(externalAlgorithm, configurableProblem, externalPopulation, externalMaxEvaluations);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
            externalAlgorithm, configurableProblem,
            problemName,
            indicators,outputDirectory
        );

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(externalAlgorithm, indicators.get(0).name(),
            indicators.get(1).name(),
                problemName,
                50);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(25, externalMaxEvaluations, outputResultsManagement);

    externalOptimizationAlgorithm.observable().register(evaluationObserver);
    externalOptimizationAlgorithm.observable().register(frontChartObserver);
    externalOptimizationAlgorithm.observable().register(writeExecutionDataToFilesObserver);

    externalOptimizationAlgorithm.run();

    JMetalLogger.logger.info(() -> "Total computing time: " + externalOptimizationAlgorithm.totalComputingTime());

    outputResultsManagement.updateSuffix("." + externalMaxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(externalOptimizationAlgorithm.result());

    System.exit(0);
  }
}
