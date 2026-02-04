package org.uma.evolver.analysis.ablation;

/**
 * Reports progress for long-running ablation analyses.
 */
public interface ProgressReporter {

  /**
   * Reports progress for a given phase.
   *
   * @param phase the phase name
   * @param current the current progress count
   * @param total the total expected count
   * @param details extra details for reporting
   */
  void reportProgress(String phase, int current, int total, String details);
}
