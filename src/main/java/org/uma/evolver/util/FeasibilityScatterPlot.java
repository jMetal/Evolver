package org.uma.evolver.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A scatter plot of some solutions on their first two objectives, those that a criterion accepts
 * in black and the others in red, optionally over a reference front in gray. {@link
 * ConstrainedFrontPlotObserver} uses it for the population of an algorithm (feasible or not), and
 * the live plot of a training for the configurations of the meta-optimizer (with or without runs
 * that found no feasible solutions).
 */
public class FeasibilityScatterPlot {

  static final String REFERENCE_FRONT = "Reference front";

  private final XYChart chart;
  private final String acceptedLegend;
  private final String rejectedLegend;
  private SwingWrapper<XYChart> swingWrapper;

  /**
   * Creates the plot; its window opens at the first {@link #display}.
   *
   * @param title the title of the plot
   * @param xAxisTitle the title of the x axis (the first objective)
   * @param yAxisTitle the title of the y axis (the second objective)
   * @param acceptedLegend the legend of the solutions that the criterion accepts (in black)
   * @param rejectedLegend the legend of the other solutions (in red)
   */
  public FeasibilityScatterPlot(
      String title,
      String xAxisTitle,
      String yAxisTitle,
      String acceptedLegend,
      String rejectedLegend) {
    this.acceptedLegend = acceptedLegend;
    this.rejectedLegend = rejectedLegend;
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
   * Draws a reference front, in light gray, behind the solutions.
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
   * Updates the series and the title with some solutions, without displaying them.
   *
   * @param solutions the solutions
   * @param accepted the criterion of the solutions in black
   * @param title the title of the plot
   * @param <S> the type of the solutions
   * @return the number of solutions that the criterion accepts
   */
  public <S extends Solution<?>> int plot(
      List<S> solutions, Predicate<? super S> accepted, String title) {
    List<S> inBlack = new ArrayList<>();
    List<S> inRed = new ArrayList<>();
    for (S solution : solutions) {
      (accepted.test(solution) ? inBlack : inRed).add(solution);
    }
    updateSeries(acceptedLegend, inBlack, Color.BLACK);
    updateSeries(rejectedLegend, inRed, Color.RED);
    chart.setTitle(title);
    return inBlack.size();
  }

  /** Opens the window of the plot the first time, and repaints it afterwards. */
  public void display() {
    if (swingWrapper == null) {
      swingWrapper = new SwingWrapper<>(chart);
      swingWrapper.displayChart();
    } else {
      swingWrapper.repaintChart();
    }
  }

  // XChart cannot create a series without points, but can empty one: each series is created the
  // first time it has points
  private <S extends Solution<?>> void updateSeries(String name, List<S> solutions, Color color) {
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
}
