package org.uma.evolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.factory.ConfigurableProblemFactory;
import org.uma.evolver.factory.OptimizationAlgorithmFactory;
import org.uma.evolver.problem.ConfigurableAlgorithmBaseProblem;
import org.uma.evolver.problem.ConfigurableAlgorithmMultiProblem;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running a meta-optimizer to auto-design an algorithm for a specific set of problems
 *
 * @author José Francisco Aldana Martín (jfaldanam@uma.es)
 */
public class MetaRunnerV2 {

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

    checkCommandLineArguments(args);
    String filePath = args[0];

    String configurationParameters = readConfigurationParameters(filePath);
    JMetalLogger.logger.info("Executing with the following configuration:\n" + configurationParameters);

    // Extract the parameters from the file using regex
    // Each regex matches the key, a space and a word (\w), a number (\d) and some of them a series or special characters (",/.)
    String externalAlgorithm = extractValue(configurationParameters, "meta_optimizer_algorithm:\\s+([\\w,\"]+)");
    String externalPopulationArg = extractValue(configurationParameters, "meta_optimizer_population_size:\\s+(\\d+)");
    String externalMaxEvaluationsArg = extractValue(configurationParameters, "meta_optimizer_max_evaluations:\\s+(\\d+)");
    String independentRunsArg = extractValue(configurationParameters, "independent_runs:\\s+(\\d+)");
    String indicatorsNames = extractValue(configurationParameters, "indicators_names:\\s+([\\w,\"]+)");
    String outputDirectory = extractValue(configurationParameters, "output_directory:\\s+([\\w/\"]+)");

    String configurableAlgorithm = extractValue(configurationParameters, "configurable_algorithm:\\s+([\\w,\"]+)");
    String populationArg = extractValue(configurationParameters, "internal_population_size:\\s+(\\d+)");
    String problemName = extractValue(configurationParameters, "problem_names:\\s+([\\w,\"\\.]+)");
    String referenceFrontFileName = extractValue(configurationParameters, "reference_front_file_name:\\s+([\\w.,/\"]+)");
    String maxNumberOfEvaluations = extractValue(configurationParameters, "max_number_of_evaluations:\\s+([\\d,\"]+)");

    String weightVectorFilesDirectory = extractOptionalValue(configurationParameters, "weight_vector_files_directory:\\s+([\\w.,/\"]+");

    int population = Integer.parseInt(populationArg);
    int independentRuns = Integer.parseInt(independentRunsArg);
    int externalPopulation = Integer.parseInt(externalPopulationArg);
    int externalMaxEvaluations = Integer.parseInt(externalMaxEvaluationsArg);

    List<QualityIndicator> indicators = QualityIndicatorUtils.getIndicatorsFromNames(List.of(indicatorsNames.split(",")));

    DoubleProblem problem;
    int internalMaxEvaluations;
    if (problemName.contains(",")) {
      problem = (DoubleProblem) ProblemFactory.<DoubleSolution>loadProblem("org.uma.jmetal.problem.multiobjective.zdt.ZDT1"); // This is a dummy problem for the multi-problem cases
      internalMaxEvaluations = -1; // This is a dummy value for the multi-problem cases
    } else {
      Check.that(!maxNumberOfEvaluations.contains(","),
          "If there is only one problem, you can't have several maxNumberOfEvaluations");
      Check.that(!referenceFrontFileName.contains(","),
          "If there is only one problem, you can't have several referenceFrontFileName");
      problem = (DoubleProblem) ProblemFactory.<DoubleSolution>loadProblem(problemName);
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
      List<DoubleProblem> problems = Arrays
          .stream(problemName.split(","))
          .map(name -> (DoubleProblem) ProblemFactory.<DoubleSolution>loadProblem(name))
          .toList();

      List<Integer> maxNumberOfEvaluationsPerProblem = Arrays.stream(
              maxNumberOfEvaluations.split(","))
          .map(Integer::parseInt).toList() ;
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
        externalAlgorithm, configurableProblem, externalPopulation, externalMaxEvaluations, 8);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        externalAlgorithm, configurableProblem,
        getProblemNamesFromString(problemName),
        indicators, outputDirectory
    );

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(externalAlgorithm, indicators.get(0).name(),
            indicators.get(1).name(),
            getProblemNamesFromString(problemName),
            50);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(25,
        externalMaxEvaluations, outputResultsManagement);

    externalOptimizationAlgorithm.observable().register(evaluationObserver);
    externalOptimizationAlgorithm.observable().register(frontChartObserver);
    externalOptimizationAlgorithm.observable().register(writeExecutionDataToFilesObserver);

    externalOptimizationAlgorithm.run();

    JMetalLogger.logger.info("Total computing time: " + externalOptimizationAlgorithm.totalComputingTime());

    outputResultsManagement.updateSuffix("." + externalMaxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(externalOptimizationAlgorithm.result());

    System.exit(0);
  }

  private static String readConfigurationParameters(String filePath) throws IOException {
    String configurationParameters;
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }

      configurationParameters = stringBuilder.toString();
    }
    return configurationParameters;
  }

  private static void checkCommandLineArguments(String[] args) {
    if (args.length != 1) {
      System.err.println("Missing configuration file.");
      System.exit(1);
    }
  }

  private static String extractValue(String input, String pattern) {
    Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
    Matcher matcher = regex.matcher(input);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Missing or malformed parameter " + pattern + ".");
    }

    return matcher.group(1).replace("\"", "");
  }

  private static String extractOptionalValue(String input, String pattern) {
    try {
      return extractValue(input, pattern);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Given a string containing one or more problem names separated by a comma, it returns a string
   * containing all the problem names (their qualified packages are removed)
   *
   * @param problemNames
   * @return
   */
  private static String getProblemNamesFromString(String problemNames) {
    String names = "" ;
    String[] namesArray = problemNames.split(",") ;

    for (String name : namesArray) {
      String[] packageNames = name.split("\\.");
      if (packageNames.length > 1) {
        names = names + packageNames[packageNames.length - 1] + ",";
      } else {
        names = names + packageNames[0] ;
      }
    }

    return names.substring(0, names.length()-1) ;
  }
}
