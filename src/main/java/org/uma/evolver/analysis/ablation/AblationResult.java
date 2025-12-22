package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores the results of an ablation analysis path.
 */
public class AblationResult {
  private final List<AblationStepResult> results = new ArrayList<>();

  public record AblationStepResult(String stepDescription, String modifiedParameter, double metricValue) {
  }

  public void addResult(String stepDescription, String modifiedParameter, double metricValue) {
    results.add(new AblationStepResult(stepDescription, modifiedParameter, metricValue));
  }

  public List<AblationStepResult> getResults() {
    return new ArrayList<>(results);
  }

  public String toCSV() {
    return "Step,ModifiedParameter,Metric\n" +
        results.stream()
            .map(r -> r.stepDescription() + "," + r.modifiedParameter() + "," + r.metricValue())
            .collect(Collectors.joining("\n"));
  }
}
