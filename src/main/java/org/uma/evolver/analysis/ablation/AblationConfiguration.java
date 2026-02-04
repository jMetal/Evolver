package org.uma.evolver.analysis.ablation;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable configuration for ablation analyses.
 *
 * <p>This class provides a fluent API for customizing ablation runs while keeping configuration
 * instances immutable. Each "setter" returns a new instance with the updated value.</p>
 */
public record AblationConfiguration(
    String analysisName,
    ProblemSuite problemSuite,
    int numberOfRuns,
    int maxEvaluations,
    int numberOfThreads,
    int populationSize,
    String yamlParameterSpaceFile,
    IndicatorDefinition indicatorDefinition,
    boolean enableProgressReporting,
    boolean validateConfigurations,
    String outputPrefix,
    Path outputDirectory,
    String baselineConfiguration,
    String optimizedConfiguration) {

  /**
   * Creates a new validated configuration record.
   *
   * @throws IllegalArgumentException if any required field is invalid
   */
  public AblationConfiguration {
    validateString("analysisName", analysisName);
    Objects.requireNonNull(problemSuite, "problemSuite cannot be null");
    validatePositive("numberOfRuns", numberOfRuns);
    validateNonNegative("maxEvaluations", maxEvaluations);
    validatePositive("numberOfThreads", numberOfThreads);
    validatePositive("populationSize", populationSize);
    validateString("yamlParameterSpaceFile", yamlParameterSpaceFile);
    Objects.requireNonNull(indicatorDefinition, "indicatorDefinition cannot be null");
    validateString("outputPrefix", outputPrefix);
    Objects.requireNonNull(outputDirectory, "outputDirectory cannot be null");
    validateString("baselineConfiguration", baselineConfiguration);
    validateString("optimizedConfiguration", optimizedConfiguration);
  }

  /**
   * Creates a pre-configured ablation configuration for ZDT problems.
   *
   * @return a configuration with sensible defaults for ZDT
   */
  public static AblationConfiguration forZDTProblems() {
    AblationConfiguration result;
    result = defaultConfigurationFor(ProblemSuite.ZDT);
    return result;
  }

  /**
   * Creates a pre-configured ablation configuration for DTLZ problems (3D).
   *
   * @return a configuration with sensible defaults for DTLZ
   */
  public static AblationConfiguration forDTLZProblems() {
    AblationConfiguration result;
    result = defaultConfigurationFor(ProblemSuite.DTLZ);
    return result;
  }

  /**
   * Creates a pre-configured ablation configuration for WFG problems (2D).
   *
   * @return a configuration with sensible defaults for WFG
   */
  public static AblationConfiguration forWFGProblems() {
    AblationConfiguration result;
    result = defaultConfigurationFor(ProblemSuite.WFG);
    return result;
  }

  /**
   * Sets the analysis name.
   *
   * @param value the new analysis name
   * @return a new configuration instance
   */
  public AblationConfiguration analysisName(String value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        value,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the problem suite.
   *
   * @param value the new problem suite
   * @return a new configuration instance
   */
  public AblationConfiguration problemSuite(ProblemSuite value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        value,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the problem suite using a string identifier (e.g., "ZDT", "DTLZ", "WFG").
   *
   * @param value the suite identifier
   * @return a new configuration instance
   */
  public AblationConfiguration problemSuite(String value) {
    AblationConfiguration result;
    result = problemSuite(ProblemSuite.fromString(value));
    return result;
  }

  /**
   * Sets the number of independent runs per configuration.
   *
   * @param value the number of runs
   * @return a new configuration instance
   */
  public AblationConfiguration numberOfRuns(int value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        value,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the maximum evaluations per run.
   *
   * @param value the number of evaluations (0 to keep training-set defaults)
   * @return a new configuration instance
   */
  public AblationConfiguration maxEvaluations(int value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        value,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the number of threads for parallel execution.
   *
   * @param value the number of threads
   * @return a new configuration instance
   */
  public AblationConfiguration numberOfThreads(int value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        value,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the YAML parameter space file.
   *
   * @param value the YAML file path
   * @return a new configuration instance
   */
  public AblationConfiguration yamlParameterSpaceFile(String value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        value,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Enables or disables progress reporting.
   *
   * @param value true to enable reporting
   * @return a new configuration instance
   */
  public AblationConfiguration enableProgressReporting(boolean value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        value,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Enables or disables configuration validation.
   *
   * @param value true to validate configurations
   * @return a new configuration instance
   */
  public AblationConfiguration validateConfigurations(boolean value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        value,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the output file prefix.
   *
   * @param value the prefix to use for output CSV files
   * @return a new configuration instance
   */
  public AblationConfiguration outputPrefix(String value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        value,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the output directory.
   *
   * @param value the output directory path
   * @return a new configuration instance
   */
  public AblationConfiguration outputDirectory(Path value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        value,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the baseline configuration string.
   *
   * @param value the baseline configuration
   * @return a new configuration instance
   */
  public AblationConfiguration baselineConfiguration(String value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        value,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the optimized configuration string.
   *
   * @param value the optimized configuration
   * @return a new configuration instance
   */
  public AblationConfiguration optimizedConfiguration(String value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        value);
    return result;
  }

  /**
   * Sets the population size for the base algorithm.
   *
   * @param value the population size
   * @return a new configuration instance
   */
  public AblationConfiguration populationSize(int value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        value,
        yamlParameterSpaceFile,
        indicatorDefinition,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  /**
   * Sets the indicator definition for metric evaluation.
   *
   * @param value the indicator definition
   * @return a new configuration instance
   */
  public AblationConfiguration indicatorDefinition(IndicatorDefinition value) {
    AblationConfiguration result;
    result = new AblationConfiguration(
        analysisName,
        problemSuite,
        numberOfRuns,
        maxEvaluations,
        numberOfThreads,
        populationSize,
        yamlParameterSpaceFile,
        value,
        enableProgressReporting,
        validateConfigurations,
        outputPrefix,
        outputDirectory,
        baselineConfiguration,
        optimizedConfiguration);
    return result;
  }

  private static AblationConfiguration defaultConfigurationFor(ProblemSuite suite) {
    AblationConfiguration result;
    var defaults = AblationDefaults.forSuite(suite);
    result = new AblationConfiguration(
        defaults.analysisName(),
        suite,
        defaults.numberOfRuns(),
        defaults.maxEvaluations(),
        defaults.numberOfThreads(),
        defaults.populationSize(),
        defaults.yamlParameterSpaceFile(),
        defaults.indicatorDefinition(),
        defaults.enableProgressReporting(),
        defaults.validateConfigurations(),
        defaults.outputPrefix(),
        defaults.outputDirectory(),
        defaults.baselineConfiguration(),
        defaults.optimizedConfiguration());
    return result;
  }

  private static void validateString(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " cannot be null or blank");
    }
  }

  private static void validatePositive(String fieldName, int value) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive: " + value);
    }
  }

  private static void validateNonNegative(String fieldName, int value) {
    if (value < 0) {
      throw new IllegalArgumentException(fieldName + " must be >= 0: " + value);
    }
  }
}
