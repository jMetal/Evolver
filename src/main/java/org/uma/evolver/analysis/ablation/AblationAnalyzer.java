package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;

/**
 * Performs ablation analysis across multiple problems to understand the contribution of individual
 * parameter changes from a default configuration to an optimized configuration.
 *
 * <p>This analyzer evaluates each configuration on all problems in the training set, normalizes the
 * indicator values per problem to ensure comparable scales, and aggregates the results (using the
 * mean) to obtain a single performance value.
 *
 * <p>Multi-problem ablation analysis is useful when the optimized configuration was obtained by
 * training on a set of problems, and we want to understand which parameter changes contribute most
 * across the entire training set.
 *
 * @param <S> The type of solutions used by the algorithm being analyzed
 * @author Antonio J. Nebro
 */
public class AblationAnalyzer<S extends Solution<?>> {

  private final BaseLevelAlgorithm<S> algorithm;
  private final List<ProblemWithReferenceFront<S>> problems;
  private final List<QualityIndicator> indicators;
  private final int numberOfRuns;
  private final int maxEvaluations;
  private final ParameterSpace parameterSpace;

  // Cache normalized reference fronts to avoid repeated normalization
  private final Map<String, double[][]> normalizedReferenceFronts = new HashMap<>();

  /**
   * Represents a problem along with its reference front for normalization.
   *
   * @param <S> The type of solutions
   */
  public static class ProblemWithReferenceFront<S extends Solution<?>> {
    private final Problem<S> problem;
    private final double[][] referenceFront;
    private final String name;

    public ProblemWithReferenceFront(Problem<S> problem, double[][] referenceFront, String name) {
      this.problem = problem;
      this.referenceFront = referenceFront;
      this.name = name;
    }

    public Problem<S> problem() {
      return problem;
    }

    public double[][] referenceFront() {
      return referenceFront;
    }

    public String name() {
      return name;
    }
  }

  // Number of threads for parallel execution (1 = sequential)
  private final int numberOfThreads;
  
  // Progress reporter for real-time feedback
  private ProgressReporter progressReporter;

  /**
   * Creates a new AblationAnalyzer with automatic thread count (uses available processors).
   *
   * @param algorithm the configurable algorithm to analyze
   * @param problems the list of problems with their reference fronts
   * @param indicators quality indicators for measuring performance
   * @param maxEvaluations maximum evaluations for each algorithm run
   * @param numberOfRuns number of independent runs per configuration per problem
   * @param parameterSpace the parameter space definition to resolve conditional parameters
   */
  public AblationAnalyzer(
      BaseLevelAlgorithm<S> algorithm,
      List<ProblemWithReferenceFront<S>> problems,
      List<QualityIndicator> indicators,
      int maxEvaluations,
      int numberOfRuns,
      ParameterSpace parameterSpace) {
    this(algorithm, problems, indicators, maxEvaluations, numberOfRuns, parameterSpace, 
         Runtime.getRuntime().availableProcessors());
  }

  /**
   * Creates a new AblationAnalyzer with configurable thread count.
   *
   * @param algorithm the configurable algorithm to analyze
   * @param problems the list of problems with their reference fronts
   * @param indicators quality indicators for measuring performance
   * @param maxEvaluations maximum evaluations for each algorithm run
   * @param numberOfRuns number of independent runs per configuration per problem
   * @param parameterSpace the parameter space definition to resolve conditional parameters
   * @param numberOfThreads number of threads to use (1 = sequential, >1 = parallel)
   */
  public AblationAnalyzer(
      BaseLevelAlgorithm<S> algorithm,
      List<ProblemWithReferenceFront<S>> problems,
      List<QualityIndicator> indicators,
      int maxEvaluations,
      int numberOfRuns,
      ParameterSpace parameterSpace,
      int numberOfThreads) {

    // Input validation
    Objects.requireNonNull(algorithm, "Algorithm cannot be null");
    Objects.requireNonNull(problems, "Problems list cannot be null");
    Objects.requireNonNull(indicators, "Indicators list cannot be null");
    Objects.requireNonNull(parameterSpace, "Parameter space cannot be null");

    if (problems.isEmpty()) {
      throw new IllegalArgumentException("Problems list cannot be empty");
    }
    if (indicators.isEmpty()) {
      throw new IllegalArgumentException("Indicators list cannot be empty");
    }
    if (maxEvaluations <= 0) {
      throw new IllegalArgumentException("Max evaluations must be positive");
    }
    if (numberOfRuns <= 0) {
      throw new IllegalArgumentException("Number of runs must be positive");
    }
    if (numberOfThreads <= 0) {
      throw new IllegalArgumentException("Number of threads must be positive");
    }

    this.algorithm = algorithm;
    this.problems = new ArrayList<>(problems);
    this.indicators = new ArrayList<>(indicators);
    this.maxEvaluations = maxEvaluations;
    this.numberOfRuns = numberOfRuns;
    this.parameterSpace = parameterSpace;
    this.numberOfThreads = numberOfThreads;
    this.progressReporter = null; // No progress reporting by default

    // Configure ForkJoinPool parallelism
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                      String.valueOf(numberOfThreads));

    // Initialize ThreadLocal indicators (always needed for thread safety)
    if (numberOfThreads > 1) {
      this.threadLocalIndicators = ThreadLocal.withInitial(() -> {
        List<QualityIndicator> localIndicators = new ArrayList<>();
        for (QualityIndicator indicator : this.indicators) {
          try {
            // Create a new instance for this thread
            QualityIndicator copy = indicator.getClass().getDeclaredConstructor().newInstance();
            localIndicators.add(copy);
          } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create thread-local indicator: " + indicator.getClass().getSimpleName(), e);
          }
        }
        return localIndicators;
      });
    } else {
      this.threadLocalIndicators = null;
    }

    // Pre-compute normalized reference fronts with defensive copying
    for (ProblemWithReferenceFront<S> problemData : this.problems) {
      double[][] originalReferenceFront = problemData.referenceFront();
      // Create a defensive copy before normalization to avoid modifying original data
      double[][] referenceFrontCopy = new double[originalReferenceFront.length][];
      for (int i = 0; i < originalReferenceFront.length; i++) {
        referenceFrontCopy[i] =
            Arrays.copyOf(originalReferenceFront[i], originalReferenceFront[i].length);
      }
      normalizedReferenceFronts.put(
          problemData.name(), NormalizeUtils.normalize(referenceFrontCopy));
    }
  }

  /**
   * Gets the set of all conditional sub-parameter names for a given categorical parameter value.
   */
  private List<String> getConditionalSubParameters(String parameterName, String value) {
    try {
      Parameter<?> parameter = parameterSpace.get(parameterName);
      if (parameter instanceof CategoricalParameter) {
        return parameter.findConditionalParameters(value).stream().map(Parameter::name).toList();
      }
    } catch (IllegalArgumentException e) {
      // Parameter not found in space, ignore
    }
    return List.of();
  }

  private Set<String> getAllConditionalSubParameters() {
    Set<String> subParams = new HashSet<>();
    for (Parameter<?> param : parameterSpace.parameters().values()) {
      param
          .conditionalParameters()
          .forEach(condParam -> subParams.add(condParam.parameter().name()));
    }
    return subParams;
  }

  private void applyConditionalParameters(
      Map<String, String> config,
      String paramName,
      String targetValue,
      Map<String, String> sourceConfig,
      Map<String, String> otherConfig) {

    String otherValue = otherConfig.get(paramName);
    if (otherValue != null) {
      List<String> otherSubParams = getConditionalSubParameters(paramName, otherValue);
      for (String subParam : otherSubParams) {
        config.remove(subParam);
      }
    }

    List<String> targetSubParams = getConditionalSubParameters(paramName, targetValue);
    for (String subParam : targetSubParams) {
      String value = sourceConfig.get(subParam);
      if (value != null) {
        config.put(subParam, value);
      }
    }
  }

  public AblationResult leaveOneOutAnalysis(
      Map<String, String> defaultConfig, Map<String, String> optimizedConfig) {

    // Validate input configurations
    Objects.requireNonNull(defaultConfig, "Default configuration cannot be null");
    Objects.requireNonNull(optimizedConfig, "Optimized configuration cannot be null");

    if (defaultConfig.isEmpty()) {
      throw new IllegalArgumentException("Default configuration cannot be empty");
    }
    if (optimizedConfig.isEmpty()) {
      throw new IllegalArgumentException("Optimized configuration cannot be empty");
    }

    AblationResult result = new AblationResult(indicators);

    // Count total steps for progress reporting
    Set<String> conditionalSubParams = getAllConditionalSubParameters();
    int totalParameters = (int) optimizedConfig.keySet().stream()
        .filter(paramName -> !conditionalSubParams.contains(paramName))
        .filter(paramName -> {
          String optValue = optimizedConfig.get(paramName);
          String defValue = defaultConfig.getOrDefault(paramName, null);
          return defValue != null && !Objects.equals(optValue, defValue);
        })
        .count();
    
    int totalSteps = totalParameters + 2; // +2 for baseline evaluations
    
    if (progressReporter != null) {
      progressReporter.startPhase("Leave-One-Out Analysis", totalSteps);
    }

    long stepStartTime = System.currentTimeMillis();
    double[] optimizedPerformance = evaluateConfigurationAcrossProblems(optimizedConfig);
    result.setOptimizedPerformance(optimizedPerformance);
    
    if (progressReporter != null) {
      progressReporter.stepCompleted(1, totalSteps, "Optimized configuration baseline", 
                                   System.currentTimeMillis() - stepStartTime);
    }

    stepStartTime = System.currentTimeMillis();
    double[] defaultPerformance = evaluateConfigurationAcrossProblems(defaultConfig);
    result.setDefaultPerformance(defaultPerformance);
    
    if (progressReporter != null) {
      progressReporter.stepCompleted(2, totalSteps, "Default configuration baseline", 
                                   System.currentTimeMillis() - stepStartTime);
    }

    int currentStep = 2;
    for (String paramName : optimizedConfig.keySet()) {
      if (conditionalSubParams.contains(paramName)) {
        continue;
      }

      String optValue = optimizedConfig.get(paramName);
      String defValue = defaultConfig.getOrDefault(paramName, null);

      if (defValue != null && !Objects.equals(optValue, defValue)) {
        currentStep++;
        stepStartTime = System.currentTimeMillis();
        
        Map<String, String> revertedConfig = new HashMap<>(optimizedConfig);
        revertedConfig.put(paramName, defValue);

        applyConditionalParameters(
            revertedConfig, paramName, defValue, defaultConfig, optimizedConfig);

        double[] revertedPerformance = evaluateConfigurationAcrossProblems(revertedConfig);

        double[] contribution = new double[indicators.size()];
        for (int i = 0; i < indicators.size(); i++) {
          contribution[i] = revertedPerformance[i] - optimizedPerformance[i];
        }

        result.addParameterContribution(
            new ParameterContribution(paramName, optValue, defValue, contribution));
        
        if (progressReporter != null) {
          progressReporter.stepCompleted(currentStep, totalSteps, 
                                       "Parameter: " + paramName + " (" + optValue + " → " + defValue + ")", 
                                       System.currentTimeMillis() - stepStartTime);
        }
      }
    }

    if (progressReporter != null) {
      progressReporter.phaseCompleted("Leave-One-Out Analysis", System.currentTimeMillis() - stepStartTime);
    }

    return result;
  }

  public AblationResult forwardPathAnalysis(
      Map<String, String> defaultConfig, Map<String, String> optimizedConfig) {

    // Validate input configurations
    Objects.requireNonNull(defaultConfig, "Default configuration cannot be null");
    Objects.requireNonNull(optimizedConfig, "Optimized configuration cannot be null");

    if (defaultConfig.isEmpty()) {
      throw new IllegalArgumentException("Default configuration cannot be empty");
    }
    if (optimizedConfig.isEmpty()) {
      throw new IllegalArgumentException("Optimized configuration cannot be empty");
    }

    AblationResult result = new AblationResult(indicators);

    Set<String> conditionalSubParams = getAllConditionalSubParameters();

    List<String> changedParams = new ArrayList<>();
    for (String param : optimizedConfig.keySet()) {
      if (conditionalSubParams.contains(param)) {
        continue;
      }
      String defValue = defaultConfig.getOrDefault(param, null);
      if (defValue != null && !Objects.equals(optimizedConfig.get(param), defValue)) {
        changedParams.add(param);
      }
    }

    int totalSteps = changedParams.size() + 1; // +1 for baseline evaluation
    
    if (progressReporter != null) {
      progressReporter.startPhase("Forward Path Analysis", totalSteps);
    }

    long stepStartTime = System.currentTimeMillis();
    Map<String, String> currentConfig = new HashMap<>(defaultConfig);
    double[] currentPerformance = evaluateConfigurationAcrossProblems(currentConfig);
    // Store a copy of the default performance without unnecessary cloning
    result.setDefaultPerformance(Arrays.copyOf(currentPerformance, currentPerformance.length));

    if (progressReporter != null) {
      progressReporter.stepCompleted(1, totalSteps, "Default configuration baseline", 
                                   System.currentTimeMillis() - stepStartTime);
    }

    Set<String> remaining = new HashSet<>(changedParams);
    List<AblationStep> path = new ArrayList<>();
    int currentStep = 1;

    while (!remaining.isEmpty()) {
      currentStep++;
      stepStartTime = System.currentTimeMillis();
      
      String bestParam = null;
      double bestImprovement = Double.NEGATIVE_INFINITY;
      double[] bestPerformance = null;
      Map<String, String> bestConfig = null;

      for (String param : remaining) {
        Map<String, String> testConfig = new HashMap<>(currentConfig);
        testConfig.put(param, optimizedConfig.get(param));

        applyConditionalParameters(
            testConfig, param, optimizedConfig.get(param), optimizedConfig, currentConfig);

        double[] testPerformance = evaluateConfigurationAcrossProblems(testConfig);
        double improvement = aggregateImprovement(currentPerformance, testPerformance);

        if (improvement > bestImprovement) {
          bestImprovement = improvement;
          bestParam = param;
          bestPerformance = testPerformance;
          bestConfig = testConfig;
        }
      }

      if (bestParam != null) {
        path.add(
            new AblationStep(
                bestParam,
                currentConfig.get(bestParam),
                optimizedConfig.get(bestParam),
                Arrays.copyOf(currentPerformance, currentPerformance.length),
                Arrays.copyOf(bestPerformance, bestPerformance.length)));

        currentConfig = bestConfig;
        currentPerformance = bestPerformance;
        remaining.remove(bestParam);
        
        if (progressReporter != null) {
          progressReporter.stepCompleted(currentStep, totalSteps, 
                                       "Added parameter: " + bestParam + " (improvement: " + 
                                       String.format("%.6f", bestImprovement) + ")", 
                                       System.currentTimeMillis() - stepStartTime);
        }
      } else {
        // Safety check: if no improvement found, break to avoid infinite loop
        System.err.println(
            "Warning: No parameter improvement found in forward path analysis. "
                + "Remaining parameters: "
                + remaining);
        break;
      }
    }

    result.setAblationPath(path);
    result.setOptimizedPerformance(currentPerformance);

    if (progressReporter != null) {
      progressReporter.phaseCompleted("Forward Path Analysis", System.currentTimeMillis() - stepStartTime);
    }

    return result;
  }

  private double[] evaluateConfigurationAcrossProblems(Map<String, String> config) {
    double[] aggregatedIndicators = new double[indicators.size()];

    // Use parallel streams (when numberOfThreads = 1, parallel streams use single thread)
    double[][] problemResults = problems.parallelStream()
            .map(problemData -> evaluateConfigurationOnProblem(config, problemData))
            .toArray(double[][]::new);

    // Aggregate results from all problems
    for (double[] problemResult : problemResults) {
      for (int i = 0; i < indicators.size(); i++) {
        aggregatedIndicators[i] += problemResult[i];
      }
    }

    for (int i = 0; i < indicators.size(); i++) {
      aggregatedIndicators[i] /= problems.size();
    }

    return aggregatedIndicators;
  }

  private double[] evaluateConfigurationOnProblem(
      Map<String, String> config, ProblemWithReferenceFront<S> problemData) {

    String[] args = configToArgs(config);
    double[] avgIndicators = new double[indicators.size()];

    // Use pre-computed normalized reference front
    double[][] normalizedReferenceFront = normalizedReferenceFronts.get(problemData.name());

    // Use parallel streams for runs (when numberOfThreads = 1, uses single thread)
    double[][] allRunResults = java.util.stream.IntStream.range(0, numberOfRuns)
            .parallel()
            .mapToObj(
                run -> {
                  BaseLevelAlgorithm<S> instance =
                      algorithm.createInstance(problemData.problem(), maxEvaluations);
                  instance.parse(args);

                  Algorithm<List<S>> alg = instance.build();
                  alg.run();

                  List<S> front = alg.result();
                  double[][] frontMatrix = solutionsToMatrix(front);

                  if (frontMatrix.length == 0) {
                    // Handle empty solution set - use worst possible values
                    double[] worstValues = new double[indicators.size()];
                    Arrays.fill(worstValues, Double.MAX_VALUE);
                    return worstValues;
                  }

                  // Normalize the front using the same bounds as the reference front
                  double[][] normalizedFront =
                      normalizeWithReferenceBounds(frontMatrix, problemData.referenceFront());

                  // Use thread-local indicators to avoid shared state modification
                  List<QualityIndicator> threadSafeIndicators = getThreadSafeIndicators();
                  double[] runResults = new double[indicators.size()];
                  for (int i = 0; i < indicators.size(); i++) {
                    QualityIndicator indicator = threadSafeIndicators.get(i);
                    indicator.referenceFront(normalizedReferenceFront);
                    runResults[i] = indicator.compute(normalizedFront);
                  }
                  return runResults;
                })
            .toArray(double[][]::new);

    // Aggregate results from all runs
    for (double[] runResult : allRunResults) {
      for (int i = 0; i < indicators.size(); i++) {
        avgIndicators[i] += runResult[i];
      }
    }

    for (int i = 0; i < indicators.size(); i++) {
      avgIndicators[i] /= numberOfRuns;
    }

    return avgIndicators;
  }

  /**
   * Normalizes a front using the same bounds as the reference front to ensure consistent scaling.
   */
  private double[][] normalizeWithReferenceBounds(double[][] front, double[][] referenceFront) {
    if (front.length == 0 || referenceFront.length == 0) {
      return front;
    }

    int objectives = front[0].length;
    double[] minValues = new double[objectives];
    double[] maxValues = new double[objectives];

    // Find bounds from reference front
    for (int obj = 0; obj < objectives; obj++) {
      minValues[obj] = Double.MAX_VALUE;
      maxValues[obj] = Double.MIN_VALUE;

      for (double[] point : referenceFront) {
        minValues[obj] = Math.min(minValues[obj], point[obj]);
        maxValues[obj] = Math.max(maxValues[obj], point[obj]);
      }
    }

    // Extend bounds to include the current front if necessary
    for (double[] point : front) {
      for (int obj = 0; obj < objectives; obj++) {
        minValues[obj] = Math.min(minValues[obj], point[obj]);
        maxValues[obj] = Math.max(maxValues[obj], point[obj]);
      }
    }

    // Normalize the front
    double[][] normalizedFront = new double[front.length][objectives];
    for (int i = 0; i < front.length; i++) {
      for (int obj = 0; obj < objectives; obj++) {
        double range = maxValues[obj] - minValues[obj];
        if (range > 1e-10) {
          normalizedFront[i][obj] = (front[i][obj] - minValues[obj]) / range;
        } else {
          normalizedFront[i][obj] = 0.0; // All values are the same
        }
      }
    }

    return normalizedFront;
  }

  /**
   * Creates thread-local indicators to avoid shared state issues in parallel execution.
   * Uses ThreadLocal to efficiently manage indicator instances per thread.
   * Only initialized when parallel execution is enabled.
   */
  private final ThreadLocal<List<QualityIndicator>> threadLocalIndicators;

  private List<QualityIndicator> getThreadSafeIndicators() {
    if (numberOfThreads > 1) {
      return threadLocalIndicators.get();
    } else {
      // For single-threaded execution, reuse the original indicators
      return indicators;
    }
  }

  private double aggregateImprovement(double[] before, double[] after) {
    double improvement = 0;
    for (int i = 0; i < before.length; i++) {
      improvement += (before[i] - after[i]);
    }
    return improvement;
  }

  private String[] configToArgs(Map<String, String> config) {
    List<String> args = new ArrayList<>();
    for (Map.Entry<String, String> entry : config.entrySet()) {
      args.add("--" + entry.getKey());
      args.add(entry.getValue());
    }
    return args.toArray(new String[0]);
  }

  private double[][] solutionsToMatrix(List<S> solutions) {
    if (solutions.isEmpty()) {
      return new double[0][0];
    }
    double[][] matrix = new double[solutions.size()][solutions.get(0).objectives().length];
    for (int i = 0; i < solutions.size(); i++) {
      // Use System.arraycopy for better performance than clone()
      double[] objectives = solutions.get(i).objectives();
      System.arraycopy(objectives, 0, matrix[i], 0, objectives.length);
    }
    return matrix;
  }

  public List<ProblemWithReferenceFront<S>> getProblems() {
    return new ArrayList<>(problems);
  }

  /**
   * Gets the number of threads configured for this analyzer.
   */
  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  /**
   * Sets the progress reporter for real-time feedback.
   * 
   * @param progressReporter the progress reporter to use, or null to disable progress reporting
   */
  public void setProgressReporter(ProgressReporter progressReporter) {
    this.progressReporter = progressReporter;
  }

  /**
   * Cleanup method to remove ThreadLocal instances and prevent memory leaks.
   * Should be called when the analyzer is no longer needed.
   */
  public void cleanup() {
    if (numberOfThreads > 1 && threadLocalIndicators != null) {
      threadLocalIndicators.remove();
    }
  }
}
