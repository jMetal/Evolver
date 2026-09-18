package org.uma.evolver.cli.training;

import java.util.Map;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/** Observer that reports meta-optimizer progress to a {@link RunStatusWriter}, following the
 * same {@code Observer<Map<String, Object>>} pattern as
 * {@code org.uma.evolver.util.WriteExecutionDataToFilesObserver}. */
final class StatusFileObserver implements Observer<Map<String, Object>> {

  private final RunStatusWriter statusWriter;
  private final int maxEvaluations;
  private final int frequency;

  StatusFileObserver(RunStatusWriter statusWriter, int maxEvaluations, int frequency) {
    this.statusWriter = statusWriter;
    this.maxEvaluations = maxEvaluations;
    this.frequency = frequency;
  }

  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    int evaluations = (int) data.get("EVALUATIONS");
    if ((evaluations % frequency) == 0) {
      statusWriter.write(RunStatusWriter.State.RUNNING, evaluations, maxEvaluations);
    }
  }

  @Override
  public String toString() {
    return "Observer that writes meta-optimizer progress to a YAML status file";
  }
}
