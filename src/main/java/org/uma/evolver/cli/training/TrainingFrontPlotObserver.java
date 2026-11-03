package org.uma.evolver.cli.training;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.evolver.meta.problem.ConstrainedIndicatorValue;
import org.uma.evolver.util.FeasibilityScatterPlot;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;
import org.uma.jmetal.util.plot.FrontScatterPlot;

/**
 * Live plot of the meta-optimizer's front during a training run. It works as jMetal's {@code
 * FrontPlotObserver}, but its title names the meta-optimizer and the base-level algorithm and shows
 * the progress against the stopping condition: {@code NSGA-II optimizing RVEA. Evaluations: 500 of
 * 2000} when the run is bounded by meta-evaluations, and {@code NSGA-II optimizing RVEA. Time: 12.3
 * of 60 min (500 evaluations)} when it is bounded by computing time.
 *
 * <p>The time is the {@code COMPUTING_TIME} published by the meta-optimizer or, if it publishes
 * none, the time since this observer was created.
 *
 * <p>When a training problem has constraints, the configurations with some run that found no
 * feasible solution ({@link ConstrainedIndicatorValue#FAILED_RUNS} above 0) are drawn in red and
 * the others in black ({@link FeasibilityScatterPlot}); otherwise, all of them in black, as before.
 */
final class TrainingFrontPlotObserver implements Observer<Map<String, Object>> {

  private final FrontScatterPlot chart;
  private final FeasibilityScatterPlot feasibilityChart;
  private final String algorithms;
  private final MetaSearchConfig metaSearch;
  private final int frequency;
  private final long creationMillis = System.currentTimeMillis();

  TrainingFrontPlotObserver(
      MetaSearchConfig metaSearch,
      String baseLevelAlgorithmName,
      String xAxisTitle,
      String yAxisTitle,
      String legend,
      int frequency) {
    this(metaSearch, baseLevelAlgorithmName, xAxisTitle, yAxisTitle, legend, frequency, false);
  }

  TrainingFrontPlotObserver(
      MetaSearchConfig metaSearch,
      String baseLevelAlgorithmName,
      String xAxisTitle,
      String yAxisTitle,
      String legend,
      int frequency,
      boolean constrained) {
    if (frequency < 1) {
      throw new IllegalArgumentException("Update frequency must be at least 1");
    }
    this.algorithms = metaSearch.algorithm() + " optimizing " + baseLevelAlgorithmName;
    this.metaSearch = metaSearch;
    this.frequency = frequency;
    this.chart =
        constrained ? null : new FrontScatterPlot(algorithms, xAxisTitle, yAxisTitle, legend);
    this.feasibilityChart =
        constrained
            ? new FeasibilityScatterPlot(
                algorithms, xAxisTitle, yAxisTitle, "Without failed runs", "With failed runs")
            : null;
  }

  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    Integer evaluations = (Integer) data.get("EVALUATIONS");
    @SuppressWarnings("unchecked")
    List<? extends Solution<?>> population = (List<? extends Solution<?>>) data.get("POPULATION");
    if (evaluations == null || population == null || evaluations % frequency != 0) {
      return;
    }
    long computingMillis =
        data.get("COMPUTING_TIME") instanceof Long time
            ? time
            : System.currentTimeMillis() - creationMillis;

    String title = title(algorithms, metaSearch, evaluations, computingMillis);
    if (feasibilityChart != null) {
      feasibilityChart.plot(population, TrainingFrontPlotObserver::withoutFailedRuns, title);
      feasibilityChart.display();
      return;
    }
    chart.chartTitle(title);
    chart.updateChart(
        population.stream().map(solution -> solution.objectives()[0]).toList(),
        population.stream().map(solution -> solution.objectives()[1]).toList());
  }

  /** Whether a configuration had no run without feasible solutions (or carries no diagnosis). */
  static boolean withoutFailedRuns(Solution<?> configuration) {
    return !(configuration.attributes().get(ConstrainedIndicatorValue.FAILED_RUNS)
            instanceof Integer failedRuns)
        || failedRuns == 0;
  }

  /** The chart of a training with constraints, or {@code null} without them (for the tests). */
  FeasibilityScatterPlot feasibilityChart() {
    return feasibilityChart;
  }

  /**
   * The title of the plot: the algorithms and the progress against the stopping condition. The
   * elapsed minutes have one decimal; the time limit is written as given.
   */
  static String title(
      String algorithms, MetaSearchConfig metaSearch, int evaluations, long computingMillis) {
    if (metaSearch.boundedByComputingTime()) {
      return String.format(
          Locale.ROOT,
          "%s. Time: %.1f of %s min (%d evaluations)",
          algorithms,
          computingMillis / 60_000.0,
          BigDecimal.valueOf(metaSearch.metaMaxComputingTimeMinutes())
              .stripTrailingZeros()
              .toPlainString(),
          evaluations);
    }
    return algorithms
        + ". Evaluations: "
        + evaluations
        + " of "
        + metaSearch.metaMaxEvaluations();
  }

  @Override
  public String toString() {
    return "Observer that plots the front of the meta-optimizer with its progress in the title";
  }
}
