package org.uma.evolver.analysis.ablation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AblationAnalysis Tests")
class AblationAnalysisTest {

  private final AblationEvaluator evaluator =
      config -> config.values().stream().mapToDouble(Double::parseDouble).sum();

  @Nested
  @DisplayName("Greedy Path")
  class GreedyPath {

    @Test
    @DisplayName("Given configs, when running path ablation, then selects best steps")
    void givenConfigs_whenPathAblation_thenSelectsBestSteps() {
      String baseline = "--a 1 --b 1 --c 1";
      String optimized = "--a 3 --b 2 --c 1";

      var analysis = new AblationAnalysis(evaluator, true, new NoOpProgressReporter());
      var result = analysis.performPathAblation(baseline, optimized);

      var steps = result.getResults();
      assertEquals(3, steps.size());
      assertEquals("Base", steps.get(0).stepDescription());
      assertEquals("a=3", steps.get(1).modifiedParameter());
      assertEquals("b=2", steps.get(2).modifiedParameter());
    }
  }

  @Nested
  @DisplayName("Leave-One-Out")
  class LeaveOneOut {

    @Test
    @DisplayName("Given configs, when running LOO, then evaluates differing keys")
    void givenConfigs_whenLeaveOneOut_thenEvaluatesDifferingKeys() {
      String baseline = "--a 1 --b 1 --c 1";
      String optimized = "--a 3 --b 2 --c 1";

      var analysis = new AblationAnalysis(evaluator, true, new NoOpProgressReporter());
      var result = analysis.performLeaveOneOut(optimized, baseline);

      var steps = result.getResults();
      assertEquals(3, steps.size());
      assertEquals("Optimized", steps.get(0).stepDescription());
      assertEquals("a=1", steps.get(1).modifiedParameter());
      assertEquals("b=1", steps.get(2).modifiedParameter());
    }
  }
}
