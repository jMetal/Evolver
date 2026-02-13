package org.uma.evolver.analysis.ablation;

import java.text.DecimalFormat;

/**
 * Console-based progress reporter with an ASCII progress bar.
 */
public class ConsoleProgressReporter implements ProgressReporter {

  private static final int DEFAULT_BAR_WIDTH = 30;
  private final int barWidth;

  /**
   * Creates a reporter with the default bar width.
   */
  public ConsoleProgressReporter() {
    this(DEFAULT_BAR_WIDTH);
  }

  /**
   * Creates a reporter with a custom bar width.
   *
   * @param barWidth the bar width
   */
  public ConsoleProgressReporter(int barWidth) {
    this.barWidth = Math.max(10, barWidth);
  }

  /**
   * Reports progress by printing an ASCII bar to stdout.
   *
   * @param phase phase name
   * @param current current progress count
   * @param total total expected count
   * @param details additional details
   */
  @Override
  public synchronized void reportProgress(String phase, int current, int total, String details) {
    int boundedTotal = Math.max(total, 1);
    int boundedCurrent = Math.min(Math.max(current, 0), boundedTotal);
    double ratio = boundedCurrent / (double) boundedTotal;

    int filled = (int) Math.round(ratio * barWidth);
    int empty = barWidth - filled;

    StringBuilder bar = new StringBuilder();
    bar.append('[');
    for (int i = 0; i < filled; i++) {
      bar.append('#');
    }
    for (int i = 0; i < empty; i++) {
      bar.append('-');
    }
    bar.append(']');

    DecimalFormat format = new DecimalFormat("0.0");
    String percent = format.format(ratio * 100);

    String message = String.format(
        "\r%s %s %s%% (%d/%d) %s",
        phase,
        bar,
        percent,
        boundedCurrent,
        boundedTotal,
        details == null ? "" : details);

    System.out.print(message);

    if (boundedCurrent == boundedTotal) {
      System.out.println();
    }
  }
}
