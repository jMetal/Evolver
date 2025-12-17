package org.uma.evolver.analysis.ablation;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.analysis.ablation.AblationAnalyzer.ProblemWithReferenceFront;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Generic runner for ablation analysis experiments. Handles all the common logic for running
 * ablation analyses with different configurations.
 *
 * @author Antonio J. Nebro
 */
public class AblationRunner {

  private final AblationConfiguration config;

  /**
   * Creates a new ablation runner with the given configuration.
   *
   * @param config the configuration for the ablation analysis
   */
  public AblationRunner(AblationConfiguration config) {
    this.config = config;
  }

  /**
   * Runs the complete ablation analysis according to the configuration.
   *
   * @throws IOException if there are issues with file I/O operations
   */
  public void run() throws IOException {
    printHeader();

    // Create problems and algorithm
    List<ProblemWithReferenceFront<DoubleSolution>> problems = createProblems();
    AblationAnalyzer<DoubleSolution> analyzer = createAnalyzer(problems);

    // Parse configurations
    Map<String, String> defaultConfig = parseConfiguration(config.getDefaultConfigString());
    Map<String, String> optimizedConfig = parseConfiguration(config.getOptimizedConfigString());

    // Validate configurations if enabled
    if (config.isValidateConfigurations()) {
      validateConfiguration(defaultConfig, "default");
      validateConfiguration(optimizedConfig, "optimized");
    }

    printAnalysisInfo(problems, defaultConfig, optimizedConfig);

    // Run analyses
    AblationResults results = runAnalyses(analyzer, defaultConfig, optimizedConfig);

    // Export results
    exportResults(results);

    // Cleanup
    analyzer.cleanup();

    System.out.println("\n" + config.getAnalysisName() + " completed successfully!");
  }

  /** Container for ablation analysis results. */
  public static class AblationResults {
    private final AblationResult leaveOneOutResult;
    private final AblationResult forwardPathResult;
    private final long leaveOneOutTime;
    private final long forwardPathTime;

    public AblationResults(
        AblationResult leaveOneOutResult,
        AblationResult forwardPathResult,
        long leaveOneOutTime,
        long forwardPathTime) {
      this.leaveOneOutResult = leaveOneOutResult;
      this.forwardPathResult = forwardPathResult;
      this.leaveOneOutTime = leaveOneOutTime;
      this.forwardPathTime = forwardPathTime;
    }

    public AblationResult getLeaveOneOutResult() {
      return leaveOneOutResult;
    }

    public AblationResult getForwardPathResult() {
      return forwardPathResult;
    }

    public long getLeaveOneOutTime() {
      return leaveOneOutTime;
    }

    public long getForwardPathTime() {
      return forwardPathTime;
    }

    public long getTotalTime() {
      return leaveOneOutTime + forwardPathTime;
    }
  }

  private void printHeader() {
    System.out.println(
        config.getAnalysisName() + " using " + config.getNumberOfThreads() + " thread(s)");
    if (config.getNumberOfThreads() == 1) {
      System.out.println("Sequential execution mode");
    } else {
      System.out.println(
          "Parallel execution mode with " + config.getNumberOfThreads() + " threads");
    }

    if (config.isProgressReportingEnabled()) {
      System.out.println("Real-time progress reporting enabled");
    } else {
      System.out.println("Progress reporting disabled");
    }
  }

  private List<ProblemWithReferenceFront<DoubleSolution>> createProblems() throws IOException {
    return ProblemSuiteFactory.createProblemSuite(config.getProblemSuite());
  }

  private AblationAnalyzer<DoubleSolution> createAnalyzer(
      List<ProblemWithReferenceFront<DoubleSolution>> problems) {
    // Create template problem (use first problem from suite)
    DoubleProblem templateProblem = (DoubleProblem) problems.get(0).problem();

    // Initialize parameter space
    YAMLParameterSpace parameterSpace =
        new YAMLParameterSpace(config.getYamlParameterSpaceFile(), new DoubleParameterFactory());

    // Create algorithm
    DoubleNSGAII algorithm =
        new DoubleNSGAII(
            templateProblem,
            config.getPopulationSize(),
            config.getMaxEvaluations(),
            parameterSpace);

    // Create analyzer
    AblationAnalyzer<DoubleSolution> analyzer =
        new AblationAnalyzer<>(
            algorithm,
            problems,
            config.getIndicators(),
            config.getMaxEvaluations(),
            config.getNumberOfRuns(),
            parameterSpace,
            config.getNumberOfThreads());

    // Set progress reporter if enabled
    if (config.isProgressReportingEnabled()) {
      analyzer.setProgressReporter(
          new ConsoleProgressReporter(config.isShowProgressBars(), config.isShowTimestamps()));
    }

    return analyzer;
  }

  private void printAnalysisInfo(
      List<ProblemWithReferenceFront<DoubleSolution>> problems,
      Map<String, String> defaultConfig,
      Map<String, String> optimizedConfig) {
    System.out.println("\n=== " + config.getAnalysisName() + " ===");
    System.out.println("Problem suite: " + config.getProblemSuite());
    System.out.println(
        "Problems: " + ProblemSuiteFactory.getProblemNames(config.getProblemSuite()));
    System.out.println("Number of runs per configuration per problem: " + config.getNumberOfRuns());
    System.out.println("Max evaluations per run: " + config.getMaxEvaluations());
    System.out.println("Threads configured: " + config.getNumberOfThreads());
    System.out.println(
        "Total estimated algorithm executions: ~"
            + (problems.size()
                * config.getNumberOfRuns()
                * (defaultConfig.size() + optimizedConfig.size())));
    System.out.println();
  }

  private AblationResults runAnalyses(
      AblationAnalyzer<DoubleSolution> analyzer,
      Map<String, String> defaultConfig,
      Map<String, String> optimizedConfig) {

    // Leave-one-out analysis
    long startTime = System.currentTimeMillis();
    AblationResult looResult = analyzer.leaveOneOutAnalysis(defaultConfig, optimizedConfig);
    long looTime = System.currentTimeMillis() - startTime;

    System.out.println("=== LEAVE-ONE-OUT ANALYSIS RESULTS ===");
    System.out.println("Execution time: " + (looTime / 1000.0) + " seconds");
    System.out.println(looResult);

    // Forward path analysis
    startTime = System.currentTimeMillis();
    AblationResult forwardResult = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);
    long forwardTime = System.currentTimeMillis() - startTime;

    System.out.println("=== FORWARD PATH ANALYSIS RESULTS ===");
    System.out.println("Execution time: " + (forwardTime / 1000.0) + " seconds");
    System.out.println("Total analysis time: " + ((looTime + forwardTime) / 1000.0) + " seconds");
    System.out.println(forwardResult);

    return new AblationResults(looResult, forwardResult, looTime, forwardTime);
  }

  private void exportResults(AblationResults results) {
    // Export leave-one-out results
    String csvFilename = config.getOutputPrefix() + ".csv";
    try (FileWriter writer = new FileWriter(csvFilename, StandardCharsets.UTF_8)) {
      writer.write(results.getLeaveOneOutResult().toCSV());
      System.out.println("\nResults exported to: " + csvFilename);
    } catch (IOException e) {
      System.err.println("Error writing results to file: " + e.getMessage());
      System.out.println("\nResults could not be exported, but analysis completed successfully.");
    }

    // Export forward path results
    String pathCsvFilename = config.getOutputPrefix() + "_path.csv";
    try (FileWriter writer = new FileWriter(pathCsvFilename, StandardCharsets.UTF_8)) {
      writer.write(results.getForwardPathResult().pathToCSV());
      System.out.println("Path results exported to: " + pathCsvFilename);
    } catch (IOException e) {
      System.err.println("Error writing path results to file: " + e.getMessage());
    }
  }

  private Map<String, String> parseConfiguration(String configurationLine) {
    Map<String, String> config = new LinkedHashMap<>();
    String[] params = configurationLine.trim().split("\\s+");

    for (int i = 0; i < params.length; i += 2) {
      if (params[i].startsWith("--")) {
        String key = params[i].substring(2);
        if (i + 1 < params.length) {
          String value = params[i + 1];
          config.put(key, value);
        } else {
          throw new IllegalArgumentException("Missing value for parameter: " + key);
        }
      }
    }
    return config;
  }

  private void validateConfiguration(Map<String, String> config, String configName) {
    // Check for essential parameters
    String[] requiredParams = {
      "algorithmResult", "variation", "crossover", "mutation", "selection"
    };

    for (String param : requiredParams) {
      if (!config.containsKey(param)) {
        System.err.println("Warning: " + configName + " configuration missing parameter: " + param);
      }
    }

    // Validate numeric parameters
    validateNumericParameter(config, "crossoverProbability", 0.0, 1.0, configName);
    validateNumericParameter(config, "mutationProbabilityFactor", 0.0, 2.0, configName);

    // Validate population size parameters
    if (config.containsKey("populationSizeWithArchive")) {
      try {
        int popSize = Integer.parseInt(config.get("populationSizeWithArchive"));
        if (popSize < 10) {
          System.err.println(
              "Warning: " + configName + " has very small population size: " + popSize);
        }
      } catch (NumberFormatException e) {
        System.err.println("Warning: Invalid population size in " + configName + " configuration");
      }
    }
  }

  private void validateNumericParameter(
      Map<String, String> config, String paramName, double min, double max, String configName) {
    if (config.containsKey(paramName)) {
      try {
        double value = Double.parseDouble(config.get(paramName));
        if (value < min || value > max) {
          System.err.println(
              "Warning: "
                  + configName
                  + " parameter "
                  + paramName
                  + " value "
                  + value
                  + " outside expected range ["
                  + min
                  + ", "
                  + max
                  + "]");
        }
      } catch (NumberFormatException e) {
        System.err.println(
            "Warning: Invalid numeric value for "
                + paramName
                + " in "
                + configName
                + " configuration");
      }
    }
  }
}
