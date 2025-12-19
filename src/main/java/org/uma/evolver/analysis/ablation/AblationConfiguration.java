package org.uma.evolver.analysis.ablation;

import java.util.List;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;

/**
 * Configuration class for ablation analysis experiments. Provides a fluent API for configuring all
 * aspects of an ablation analysis.
 *
 * @author Antonio J. Nebro
 */
public class AblationConfiguration {

  // Analysis parameters
  private String analysisName = "Ablation Analysis";
  private String problemSuite = "ZDT";
  private int numberOfRuns = 10;
  private int maxEvaluations = 25000;
  private int populationSize = 100;
  private int numberOfThreads = Runtime.getRuntime().availableProcessors();

  // Algorithm configuration
  private String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
  private String defaultConfigString = getDefaultNSGAIIConfig();
  private String optimizedConfigString = "";

  // Quality indicators
  private List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

  // Progress and output
  private boolean enableProgressReporting = true;
  private boolean showProgressBars = true;
  private boolean showTimestamps = true;
  private String outputPrefix = "ablation_results";

  // Validation
  private boolean validateConfigurations = true;

  /** Creates a new configuration with default values. */
  public AblationConfiguration() {}

  /** Sets the analysis name for display purposes. */
  public AblationConfiguration analysisName(String name) {
    this.analysisName = name;
    return this;
  }

  /** Sets the problem suite to use (e.g., "ZDT", "DTLZ", "WFG"). */
  public AblationConfiguration problemSuite(String suite) {
    this.problemSuite = suite;
    return this;
  }

  /** Sets the number of independent runs per configuration per problem. */
  public AblationConfiguration numberOfRuns(int runs) {
    this.numberOfRuns = runs;
    return this;
  }

  /** Sets the maximum number of evaluations per run. */
  public AblationConfiguration maxEvaluations(int evaluations) {
    this.maxEvaluations = evaluations;
    return this;
  }

  /** Sets the population size for the algorithm. */
  public AblationConfiguration populationSize(int size) {
    this.populationSize = size;
    return this;
  }

  /** Sets the number of threads for parallel execution. */
  public AblationConfiguration numberOfThreads(int threads) {
    this.numberOfThreads = threads;
    return this;
  }

  /** Sets the YAML parameter space file. */
  public AblationConfiguration yamlParameterSpaceFile(String file) {
    this.yamlParameterSpaceFile = file;
    return this;
  }

  /** Sets the default configuration string. */
  public AblationConfiguration defaultConfiguration(String config) {
    this.defaultConfigString = config;
    return this;
  }

  /** Sets the optimized configuration string. */
  public AblationConfiguration optimizedConfiguration(String config) {
    this.optimizedConfigString = config;
    return this;
  }

  /** Sets the quality indicators to use. */
  public AblationConfiguration indicators(List<QualityIndicator> indicators) {
    this.indicators = List.copyOf(indicators);
    return this;
  }

  /** Enables or disables progress reporting. */
  public AblationConfiguration enableProgressReporting(boolean enable) {
    this.enableProgressReporting = enable;
    return this;
  }

  /** Enables or disables progress bars in console output. */
  public AblationConfiguration showProgressBars(boolean show) {
    this.showProgressBars = show;
    return this;
  }

  /** Enables or disables timestamps in console output. */
  public AblationConfiguration showTimestamps(boolean show) {
    this.showTimestamps = show;
    return this;
  }

  /** Sets the output file prefix for CSV exports. */
  public AblationConfiguration outputPrefix(String prefix) {
    this.outputPrefix = prefix;
    return this;
  }

  /** Enables or disables configuration validation. */
  public AblationConfiguration validateConfigurations(boolean validate) {
    this.validateConfigurations = validate;
    return this;
  }

  // Getters
  public String getAnalysisName() {
    return analysisName;
  }

  public String getProblemSuite() {
    return problemSuite;
  }

  public int getNumberOfRuns() {
    return numberOfRuns;
  }

  public int getMaxEvaluations() {
    return maxEvaluations;
  }

  public int getPopulationSize() {
    return populationSize;
  }

  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  public String getYamlParameterSpaceFile() {
    return yamlParameterSpaceFile;
  }

  public String getDefaultConfigString() {
    return defaultConfigString;
  }

  public String getOptimizedConfigString() {
    return optimizedConfigString;
  }

  public List<QualityIndicator> getIndicators() {
    return indicators;
  }

  public boolean isProgressReportingEnabled() {
    return enableProgressReporting;
  }

  public boolean isShowProgressBars() {
    return showProgressBars;
  }

  public boolean isShowTimestamps() {
    return showTimestamps;
  }

  public String getOutputPrefix() {
    return outputPrefix;
  }

  public boolean isValidateConfigurations() {
    return validateConfigurations;
  }

  /** Returns the default NSGA-II configuration string. */
  private static String getDefaultNSGAIIConfig() {
    return "--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";
  }

  /** Creates a configuration for ZDT problems with optimized parameters. */
  public static AblationConfiguration forZDTProblems() {
    return new AblationConfiguration()
        .analysisName("ZDT Ablation Analysis")
        .problemSuite("ZDT")
        .maxEvaluations(20000)
        .outputPrefix("ablation_results_ZDT")
        .optimizedConfiguration(
            "--algorithmResult externalArchive --populationSizeWithArchive 20 --archiveType crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6729893472234677 --crossoverRepairStrategy random --sbxDistributionIndex 16.497600223910695 --mutation linkedPolynomial --mutationProbabilityFactor 0.9913896382476491 --mutationRepairStrategy bounds --linkedPolynomialMutationDistributionIndex 9.247660639092558 --selection tournament --selectionTournamentSize 3");
  }

  /** Creates a configuration for DTLZ problems with optimized parameters. */
  public static AblationConfiguration forDTLZProblems() {
    return new AblationConfiguration()
        .analysisName("DTLZ Ablation Analysis")
        .problemSuite("DTLZ")
        .maxEvaluations(25000)
        .outputPrefix("ablation_results_DTLZ")
        .optimizedConfiguration(
            "--algorithmResult externalArchive --populationSizeWithArchive 117 --archiveType unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7508401473188827 --crossoverRepairStrategy random --sbxDistributionIndex 50.1629616234419 --mutation uniform --mutationProbabilityFactor 0.8987004036508917 --mutationRepairStrategy bounds --uniformMutationPerturbation 0.21357323319678467 --selection tournament --selectionTournamentSize 7");
  }

  @Override
  public String toString() {
    return String.format(
        "AblationConfiguration{name='%s', suite='%s', runs=%d, evaluations=%d, threads=%d}",
        analysisName, problemSuite, numberOfRuns, maxEvaluations, numberOfThreads);
  }
}
