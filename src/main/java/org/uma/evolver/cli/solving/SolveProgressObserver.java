package org.uma.evolver.cli.solving;

import java.util.Map;
import org.uma.evolver.cli.RunStatusWriter;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * Reports the progress of a solve run to a {@link RunStatusWriter} while the algorithm runs, so an
 * external process can show how far it is.
 *
 * <p>The evaluations are counted over all the independent runs: {@code evaluationsBefore} is what
 * the runs already finished performed, and the observed algorithm's own count is added to it. The
 * status is written whenever at least {@code frequency} evaluations have passed since the last
 * time, not only at exact multiples, since an algorithm that evaluates a whole offspring population
 * at a time reports its evaluations in steps of that size.
 */
final class SolveProgressObserver implements Observer<Map<String, Object>> {

  private final RunStatusWriter statusWriter;
  private final int totalEvaluations;
  private final int evaluationsBefore;
  private final int frequency;
  private int lastReported;

  SolveProgressObserver(
      RunStatusWriter statusWriter, int totalEvaluations, int evaluationsBefore, int frequency) {
    this.statusWriter = statusWriter;
    this.totalEvaluations = totalEvaluations;
    this.evaluationsBefore = evaluationsBefore;
    this.frequency = frequency;
    this.lastReported = evaluationsBefore;
  }

  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    int evaluations = evaluationsBefore + (int) data.get("EVALUATIONS");
    if (evaluations - lastReported >= frequency && evaluations < totalEvaluations) {
      lastReported = evaluations;
      statusWriter.write(RunStatusWriter.State.RUNNING, evaluations, totalEvaluations);
    }
  }

  @Override
  public String toString() {
    return "Observer that writes the progress of a solve run to a YAML status file";
  }
}
