package org.uma.evolver.analysis.ablation;

/**
 * Progress reporter that does nothing.
 */
public final class NoOpProgressReporter implements ProgressReporter {

  /**
   * No-op progress reporting.
   *
   * @param phase phase name
   * @param current current progress count
   * @param total total expected count
   * @param details additional details
   */
  @Override
  public void reportProgress(String phase, int current, int total, String details) {
    // no-op
  }
}
