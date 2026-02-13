package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the results of an ablation analysis.
 */
public class AblationResult {

  /**
   * Represents a single ablation step.
   *
   * @param stepIndex the step index (starting at 0 for the baseline/optimized reference)
   * @param stepDescription human-readable step description
   * @param modifiedParameter the parameter modified at this step
   * @param metricValue the resulting metric value
   */
  public record AblationStepResult(
      int stepIndex,
      String stepDescription,
      String modifiedParameter,
      double metricValue) {
  }

  private final List<AblationStepResult> results;

  /**
   * Creates an empty result.
   */
  public AblationResult() {
    this.results = new ArrayList<>();
  }

  /**
   * Adds a new step result.
   *
   * @param stepDescription description of the step
   * @param modifiedParameter parameter modified
   * @param metricValue resulting metric value
   */
  public void addResult(String stepDescription, String modifiedParameter, double metricValue) {
    results.add(new AblationStepResult(results.size(), stepDescription, modifiedParameter, metricValue));
  }

  /**
   * Returns a copy of the results list.
   *
   * @return list of step results
   */
  public List<AblationStepResult> getResults() {
    List<AblationStepResult> result;
    result = List.copyOf(results);
    return result;
  }

  /**
   * Serializes the results to CSV.
   *
   * @return CSV string
   */
  public String toCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append("Step,ModifiedParameter,Metric").append(System.lineSeparator());
    for (AblationStepResult result : results) {
      builder.append(result.stepDescription())
          .append(',')
          .append(result.modifiedParameter())
          .append(',')
          .append(result.metricValue())
          .append(System.lineSeparator());
    }
    return builder.toString().trim();
  }
}
