package org.uma.evolver.analysis.ablation;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.*;
import java.util.function.Function;

/**
 * Orchestrates the ablation analysis.
 */
public class AblationAnalysis {

  private final Function<Map<String, String>, Double> evaluationFunction;
  private final boolean isMaximization;

  /**
   * Generic constructor for any evaluation logic (e.g., Training Sets).
   *
   * @param evaluationFunction A function that takes a configuration map and
   *                           returns a metric value (score).
   * @param isMaximization     True if the metric should be maximized (e.g.,
   *                           Hypervolume), false for minimization.
   */
  public AblationAnalysis(Function<Map<String, String>, Double> evaluationFunction, boolean isMaximization) {
    this.evaluationFunction = evaluationFunction;
    this.isMaximization = isMaximization;
  }

  /**
   * Backward-compatible constructor for single-problem analysis.
   */
  public AblationAnalysis(
      ParameterSpace baseParameterSpace,
      Function<ParameterSpace, List<DoubleSolution>> algorithmRunner,
      QualityIndicator qualityIndicator,
      int numberOfRuns) {

    this.isMaximization = qualityIndicator.name().contains("Hypervolume");
    this.evaluationFunction = config -> {
      List<Double> runResults = new ArrayList<>();

      for (int i = 0; i < numberOfRuns; i++) {
        List<DoubleSolution> population = AblationRunner.run(baseParameterSpace, config, algorithmRunner);
        double[][] front = population.stream()
            .map(DoubleSolution::objectives)
            .toArray(double[][]::new);
        double value = qualityIndicator.compute(front);
        runResults.add(value);
      }

      runResults.sort(Double::compareTo);
      double median = runResults.get(runResults.size() / 2);
      if (runResults.size() % 2 == 0) {
        median = (runResults.get(runResults.size() / 2 - 1) + runResults.get(runResults.size() / 2)) / 2.0;
      }
      return median;
    };
  }

  /**
   * Performs a greedy path ablation from startConfig to endConfig.
   * Iteratively chooses the parameter change that produces the best metric value.
   */
  public AblationResult performPathAblation(String startConfigStr, String endConfigStr) {
    var startConfig = AblationConfigParser.parse(startConfigStr);
    var endConfig = AblationConfigParser.parse(endConfigStr);
    var currentConfig = new LinkedHashMap<>(startConfig);

    AblationResult results = new AblationResult();

    // 1. Evaluate Start Configuration
    System.out.println("Evaluating base configuration...");
    double startScore = evaluationFunction.apply(currentConfig);
    results.addResult("Base", "N/A", startScore);
    System.out.printf("Base Score: %.6f%n", startScore);

    // 2. Identify all differences
    Set<String> differingKeys = new HashSet<>();
    Set<String> allKeys = new HashSet<>(startConfig.keySet());
    allKeys.addAll(endConfig.keySet());

    for (String key : allKeys) {
      String startVal = startConfig.get(key);
      String endVal = endConfig.get(key);
      if (!Objects.equals(startVal, endVal)) {
        differingKeys.add(key);
      }
    }

    int initialDiffCount = differingKeys.size();
    System.out.println("Found " + initialDiffCount + " parameter differences.");

    // 3. Greedy Loop
    while (!differingKeys.isEmpty()) {
      int currentStep = initialDiffCount - differingKeys.size() + 1;
      System.out.printf("Step %d/%d: Evaluating %d candidates ", currentStep, initialDiffCount, differingKeys.size());

      String bestKey = null;
      double bestScore = isMaximization ? -Double.MAX_VALUE : Double.MAX_VALUE;

      boolean foundValidStep = false;

      for (String key : differingKeys) {
        // Construct candidate
        var candidateConfig = new LinkedHashMap<>(currentConfig);
        String endVal = endConfig.get(key);
        if (endVal != null) {
          candidateConfig.put(key, endVal);
        } else {
          candidateConfig.remove(key);
        }

        try {
          System.out.print(".");
          double score = evaluationFunction.apply(candidateConfig);

          if (isMaximization) {
            if (score > bestScore) {
              bestScore = score;
              bestKey = key;
            }
          } else {
            if (score < bestScore) {
              bestScore = score;
              bestKey = key;
            }
          }
          foundValidStep = true;
        } catch (Exception e) {
          // Invalid config (dependency missing), skip this candidate
          System.out.print("x");
        }
      }
      System.out.println(); // Newline after progress dots

      if (!foundValidStep || bestKey == null) {
        System.err.println(
            "Warning: No valid next step found among " + differingKeys.size() + " candidates. Stopping ablation.");
        break;
      }

      // Commit the best step
      String val = endConfig.get(bestKey);
      if (val != null) {
        currentConfig.put(bestKey, val);
      } else {
        currentConfig.remove(bestKey);
      }
      String stepLabel = bestKey + "=" + formatValue(val);
      results.addResult(stepLabel, stepLabel, bestScore);
      differingKeys.remove(bestKey);

      System.out.printf("  Selected: %s (Metric: %.6f)%n", stepLabel, bestScore);
    }

    return results;
  }

  /**
   * Performs a Leave-One-Out ablation analysis.
   * Base is optimizedConfig. For each parameter, revert to baselineConfig value.
   */
  public AblationResult performLeaveOneOut(String optimizedConfigStr, String baselineConfigStr) {
    var optimizedConfig = AblationConfigParser.parse(optimizedConfigStr);
    var baselineConfig = AblationConfigParser.parse(baselineConfigStr);

    AblationResult results = new AblationResult();

    // 1. Evaluate Optimized Configuration (Reference)
    System.out.println("Evaluating optimized configuration (baseline for LOO)...");
    double optimizedScore = evaluationFunction.apply(optimizedConfig);
    results.addResult("Optimized", "N/A", optimizedScore);
    System.out.printf("Optimized Score: %.6f%n", optimizedScore);

    // 2. Identify keys to test
    List<String> keysToTest = new ArrayList<>();
    for (String key : optimizedConfig.keySet()) {
      String optVal = optimizedConfig.get(key);
      String baseVal = baselineConfig.get(key);
      if (!Objects.equals(optVal, baseVal)) {
        keysToTest.add(key);
      }
    }

    System.out.printf("Performing LOO analysis on %d parameters...%n", keysToTest.size());

    for (String key : keysToTest) {
      Map<String, String> looConfig = new LinkedHashMap<>(optimizedConfig);
      String baseVal = baselineConfig.get(key);
      if (baseVal != null) {
        looConfig.put(key, baseVal);
      } else {
        looConfig.remove(key);
      }

      double score;
      try {
        System.out.print(".");
        score = evaluationFunction.apply(looConfig);
      } catch (Exception e) {
        System.out.print("x");
        score = Double.NaN;
      }
      String diffLabel = key + "=" + formatValue(baseVal);
      results.addResult(diffLabel, diffLabel, score);
    }
    System.out.println(); // Newline
    System.out.println("LOO Analysis Complete.");

    return results;
  }

  private String formatValue(String value) {
    if (value == null) {
      return "REMOVED";
    }
    try {
      double d = Double.parseDouble(value);
      return String.format(java.util.Locale.US, "%.4f", d);
    } catch (NumberFormatException e) {
      return value;
    }
  }
}
