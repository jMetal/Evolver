package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Executes greedy path and leave-one-out ablation analyses.
 */
public class AblationAnalysis {

  private final AblationEvaluator evaluator;
  private final boolean maximize;
  private final ProgressReporter reporter;

  /**
   * Creates a new ablation analysis.
   *
   * @param evaluator evaluation function
   * @param maximize true if larger metric values are better
   * @param reporter progress reporter (use {@link NoOpProgressReporter} to disable)
   */
  public AblationAnalysis(AblationEvaluator evaluator, boolean maximize, ProgressReporter reporter) {
    if (evaluator == null) {
      throw new IllegalArgumentException("Evaluator cannot be null");
    }
    this.evaluator = evaluator;
    this.maximize = maximize;
    this.reporter = reporter == null ? new NoOpProgressReporter() : reporter;
  }

  /**
   * Performs a greedy path ablation from a baseline to an optimized configuration.
   *
   * @param startConfigStr baseline configuration string
   * @param endConfigStr optimized configuration string
   * @return ablation results
   */
  public AblationResult performPathAblation(String startConfigStr, String endConfigStr) {
    var startConfig = ConfigurationParser.parse(startConfigStr);
    var endConfig = ConfigurationParser.parse(endConfigStr);
    var currentConfig = new LinkedHashMap<>(startConfig);

    AblationResult results = new AblationResult();

    double startScore = evaluator.evaluate(currentConfig);
    results.addResult("Base", "N/A", startScore);

    Set<String> differingKeys = computeDifferingKeys(startConfig, endConfig);
    int totalSteps = differingKeys.size();
    int stepIndex = 0;

    while (!differingKeys.isEmpty()) {
      stepIndex++;
      String bestKey = null;
      double bestScore = maximize ? -Double.MAX_VALUE : Double.MAX_VALUE;
      boolean foundValidCandidate = false;
      int candidateIndex = 0;
      int candidateTotal = differingKeys.size();

      for (String key : differingKeys) {
        candidateIndex++;
        Map<String, String> candidateConfig = new LinkedHashMap<>(currentConfig);
        applyChange(candidateConfig, key, endConfig.get(key));

        reporter.reportProgress(
            "Path",
            candidateIndex,
            candidateTotal,
            "Step " + stepIndex + "/" + totalSteps);

        try {
          double score = evaluator.evaluate(candidateConfig);
          foundValidCandidate = true;
          if (isBetter(score, bestScore)) {
            bestScore = score;
            bestKey = key;
          }
        } catch (RuntimeException e) {
          // Skip invalid configuration
        }
      }

      if (!foundValidCandidate || bestKey == null) {
        break;
      }

      applyChange(currentConfig, bestKey, endConfig.get(bestKey));
      differingKeys.remove(bestKey);
      String label = formatChange(bestKey, endConfig.get(bestKey));
      results.addResult("Step " + stepIndex, label, bestScore);
    }

    return results;
  }

  /**
   * Performs a leave-one-out ablation from an optimized configuration.
   *
   * @param optimizedConfigStr optimized configuration string
   * @param baselineConfigStr baseline configuration string
   * @return ablation results
   */
  public AblationResult performLeaveOneOut(String optimizedConfigStr, String baselineConfigStr) {
    var optimizedConfig = ConfigurationParser.parse(optimizedConfigStr);
    var baselineConfig = ConfigurationParser.parse(baselineConfigStr);

    AblationResult results = new AblationResult();
    double optimizedScore = evaluator.evaluate(optimizedConfig);
    results.addResult("Optimized", "N/A", optimizedScore);

    List<String> keysToTest = new ArrayList<>();
    for (String key : optimizedConfig.keySet()) {
      String optimizedValue = optimizedConfig.get(key);
      String baselineValue = baselineConfig.get(key);
      if (!Objects.equals(optimizedValue, baselineValue)) {
        keysToTest.add(key);
      }
    }

    int total = keysToTest.size();
    int index = 0;
    for (String key : keysToTest) {
      index++;
      Map<String, String> candidate = new LinkedHashMap<>(optimizedConfig);
      applyChange(candidate, key, baselineConfig.get(key));
      reporter.reportProgress("LOO", index, total, "Evaluating");

      double score;
      try {
        score = evaluator.evaluate(candidate);
      } catch (RuntimeException e) {
        score = Double.NaN;
      }
      String label = formatChange(key, baselineConfig.get(key));
      results.addResult("LOO " + index, label, score);
    }

    return results;
  }

  private Set<String> computeDifferingKeys(
      Map<String, String> startConfig, Map<String, String> endConfig) {
    Set<String> result = new LinkedHashSet<>();
    Set<String> allKeys = new LinkedHashSet<>(startConfig.keySet());
    allKeys.addAll(endConfig.keySet());

    for (String key : allKeys) {
      String startValue = startConfig.get(key);
      String endValue = endConfig.get(key);
      if (!Objects.equals(startValue, endValue)) {
        result.add(key);
      }
    }
    return result;
  }

  private void applyChange(Map<String, String> config, String key, String value) {
    if (value == null) {
      config.remove(key);
    } else {
      config.put(key, value);
    }
  }

  private boolean isBetter(double candidateScore, double bestScore) {
    boolean result;
    if (maximize) {
      result = candidateScore > bestScore;
    } else {
      result = candidateScore < bestScore;
    }
    return result;
  }

  private String formatChange(String key, String value) {
    String result;
    if (value == null) {
      result = key + "=REMOVED";
    } else {
      result = key + "=" + value;
    }
    return result;
  }
}
