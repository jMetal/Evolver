package org.uma.evolver.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * Live plot of the population of an algorithm on a problem with constraints: the feasible solutions
 * in black and the infeasible ones in red, on the first two objectives. It works as jMetal's {@code
 * FrontPlotObserver}, and its title also gives how many solutions of the population are feasible,
 * so that it shows when the search reaches the feasible region. On a problem without constraints
 * every solution is feasible, and the plot is the same as jMetal's.
 *
 * <p>A reference front can be drawn behind the population ({@link #setReferenceFront}), to see, for
 * instance, infeasible solutions beyond the true front.
 *
 * <pre>{@code
 * var observer =
 *     new ConstrainedFrontPlotObserver<DoubleSolution>("NSGA-II, DASCMOP5", "f1", "f2", 100);
 * observer.setReferenceFront(
 *     VectorUtils.readVectors("resources/referenceFronts/DASCMOP5.csv", ","));
 * observer.delay(100);
 * algorithm.observable().register(observer);
 * }</pre>
 *
 * @param <S> the type of the solutions
 */
public class ConstrainedFrontPlotObserver<S extends Solution<?>>
    implements Observer<Map<String, Object>> {

  static final String FEASIBLE = "Feasible";
  static final String INFEASIBLE = "Infeasible";
  static final String REFERENCE_FRONT = "Reference front";

  private final XYChart chart;
  private final String title;
  private final int plotUpdateFrequency;
  private long delayMillis;
  private SwingWrapper<XYChart> swingWrapper;

  /**
   * Creates the observer; its window opens at the first update.
   *
   * @param title the title of the plot
   * @param xAxisTitle the title of the x axis (the first objective)
   * @param yAxisTitle the title of the y axis (the second objective)
   * @param plotUpdateFrequency how often the plot is updated, in evaluations
   */
  public ConstrainedFrontPlotObserver(
      String title, String xAxisTitle, String yAxisTitle, int plotUpdateFrequency) {
    Check.that(plotUpdateFrequency >= 1, "The update frequency must be at least 1");
    this.title = title;
    this.plotUpdateFrequency = plotUpdateFrequency;
    chart =
        new XYChartBuilder()
            .width(800)
            .height(600)
            .title(title)
            .xAxisTitle(xAxisTitle)
            .yAxisTitle(yAxisTitle)
            .build();
    chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
    chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
    chart.getStyler().setMarkerSize(6);
  }

  /**
   * Draws a reference front, in light gray, behind the population.
   *
   * @param referenceFront the points of the front, with at least two objectives
   */
  public void setReferenceFront(double[][] referenceFront) {
    Check.notNull(referenceFront);
    Check.that(referenceFront.length > 0, "The reference front is empty");
    double[] x = new double[referenceFront.length];
    double[] y = new double[referenceFront.length];
    for (int i = 0; i < referenceFront.length; i++) {
      x[i] = referenceFront[i][0];
      y[i] = referenceFront[i][1];
    }
    XYSeries series = chart.addSeries(REFERENCE_FRONT, x, y);
    series.setMarker(SeriesMarkers.CIRCLE);
    series.setMarkerColor(Color.LIGHT_GRAY);
  }

  /**
   * Pauses the run after each update of the plot, so that a fast run can be followed (0, no pause,
   * by default).
   *
   * @param delayMillis the pause, in milliseconds
   */
  public void delay(long delayMillis) {
    Check.that(delayMillis >= 0, "The delay must be non-negative: " + delayMillis);
    this.delayMillis = delayMillis;
  }

  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    Integer evaluations = (Integer) data.get("EVALUATIONS");
    @SuppressWarnings("unchecked")
    List<S> population = (List<S>) data.get("POPULATION");
    if (evaluations == null || population == null || evaluations % plotUpdateFrequency != 0) {
      return;
    }
    plot(population, evaluations);
    if (swingWrapper == null) {
      swingWrapper = new SwingWrapper<>(chart);
      swingWrapper.displayChart();
    } else {
      swingWrapper.repaintChart();
    }
    if (delayMillis > 0) {
      try {
        Thread.sleep(delayMillis);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Updates the series and the title of the chart with a population, without displaying it.
   *
   * @param population the population
   * @param evaluations the evaluations done so far
   */
  void plot(List<S> population, int evaluations) {
    List<S> feasible = new ArrayList<>();
    List<S> infeasible = new ArrayList<>();
    for (S solution : population) {
      (ConstraintHandling.isFeasible(solution) ? feasible : infeasible).add(solution);
    }
    updateSeries(FEASIBLE, feasible, Color.BLACK);
    updateSeries(INFEASIBLE, infeasible, Color.RED);
    chart.setTitle(
        String.format(
            Locale.ROOT,
            "%s. Evaluations: %d. Feasible: %d of %d",
            title,
            evaluations,
            feasible.size(),
            population.size()));
  }

  // XChart cannot create a series without points, but can empty one: each series is created the
  // first time it has points
  private void updateSeries(String name, List<S> solutions, Color color) {
    List<Double> x = solutions.stream().map(solution -> solution.objectives()[0]).toList();
    List<Double> y = solutions.stream().map(solution -> solution.objectives()[1]).toList();
    if (chart.getSeriesMap().containsKey(name)) {
      chart.updateXYSeries(name, x, y, null);
    } else if (!solutions.isEmpty()) {
      XYSeries series = chart.addSeries(name, x, y);
      series.setMarker(SeriesMarkers.CIRCLE);
      series.setMarkerColor(color);
    }
  }

  /**
   * The chart, for customizations or for saving it as an image.
   *
   * @return the chart
   */
  public XYChart chart() {
    return chart;
  }

  @Override
  public String toString() {
    return "Observer that plots the feasible and infeasible solutions of the population";
  }
}
