package org.uma.evolver.cli.solving;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DominanceWithConstraintsComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * Writes the current solutions of the run in progress to a single file while the algorithm runs, so
 * an external process can show how the front evolves.
 *
 * <p>The file, {@value #FILE_NAME}, is overwritten each time (it holds only the latest solutions)
 * and is written to a temporary file that is then moved over it, so a reader never sees a
 * half-written one. It is a CSV with a header, {@code Run,Evaluations,NonDominated,F1,...,Fm}, and
 * a row for each solution: the run number, the evaluations of that run (both repeated in each
 * row), 1 if the solution is non-dominated within the current population and 0 if not, and its
 * objectives. By default only the non-dominated solutions are written (so {@code NonDominated} is
 * always 1); with {@code wholePopulation}, the whole population is, which shows the dominated ones
 * too.
 *
 * <p>It is written whenever at least {@code frequency} evaluations of the run have passed since the
 * last time. With a delay, the run then pauses that long after each write, as the display delay of
 * jMetal's chart observers does: a follower that polls the file sees every front, at the price of a
 * slower run.
 */
final class SolveFrontObserver implements Observer<Map<String, Object>> {

  /** The name of the file, in the output directory. */
  static final String FILE_NAME = "CURRENT_FRONT.csv";

  /** How many times the front file is replaced before skipping a front, and the wait between. */
  private static final int MOVE_ATTEMPTS = 20;

  private static final long MOVE_RETRY_MILLIS = 5;

  private final Path frontFile;
  private final int run;
  private final int frequency;
  private final boolean wholePopulation;
  private final Integer delayMillis;
  private int lastReported;

  SolveFrontObserver(
      Path frontFile, int run, int frequency, boolean wholePopulation, Integer delayMillis) {
    this.frontFile = frontFile;
    this.run = run;
    this.frequency = frequency;
    this.wholePopulation = wholePopulation;
    this.delayMillis = delayMillis;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    int evaluations = (int) data.get("EVALUATIONS");
    if (evaluations - lastReported < frequency) {
      return;
    }
    lastReported = evaluations;
    List<Solution<?>> population = (List<Solution<?>>) data.get("POPULATION");
    if (wholePopulation) {
      write(evaluations, population, nonDominatedFlags(population));
    } else {
      NonDominatedSolutionListArchive<Solution<?>> archive =
          new NonDominatedSolutionListArchive<>();
      archive.addAll(population);
      List<Solution<?>> front = archive.solutions();
      write(evaluations, front, new ArrayList<>(front.stream().map(solution -> true).toList()));
    }
    pause();
  }

  private void pause() {
    if (delayMillis == null) {
      return;
    }
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Whether each solution of a population is dominated by none of the others. */
  private static List<Boolean> nonDominatedFlags(List<Solution<?>> population) {
    // Deb's constrained-domination principle: a feasible solution dominates every infeasible one
    var comparator = new DominanceWithConstraintsComparator<Solution<?>>();
    List<Boolean> flags = new ArrayList<>();
    for (Solution<?> solution : population) {
      boolean dominated = false;
      for (Solution<?> other : population) {
        if (other != solution && comparator.compare(other, solution) < 0) {
          dominated = true;
          break;
        }
      }
      flags.add(!dominated);
    }
    return flags;
  }

  private void write(int evaluations, List<Solution<?>> solutions, List<Boolean> nonDominated) {
    Path temporaryFile = frontFile.resolveSibling(FILE_NAME + ".tmp");
    try {
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(temporaryFile))) {
        StringBuilder header = new StringBuilder("Run,Evaluations,NonDominated");
        for (int objective = 1; objective <= solutions.get(0).objectives().length; objective++) {
          header.append(",F").append(objective);
        }
        writer.println(header);
        for (int i = 0; i < solutions.size(); i++) {
          StringBuilder row =
              new StringBuilder(run + "," + evaluations + "," + (nonDominated.get(i) ? 1 : 0));
          for (double value : solutions.get(i).objectives()) {
            row.append(',').append(String.format(Locale.ROOT, "%s", value));
          }
          writer.println(row);
        }
      }
      replaceFrontFile(temporaryFile);
    } catch (IOException e) {
      throw new JMetalException(e);
    }
  }

  /**
   * Moves the temporary file over the front file. On Windows the replacement fails with an {@link
   * AccessDeniedException} while a reader has the front file open, as the process that follows the
   * run does when it polls it; the move is retried for a while and, if the file is still in use,
   * this front is skipped: the next one replaces it. On Linux and macOS the first attempt succeeds.
   */
  private void replaceFrontFile(Path temporaryFile) throws IOException {
    for (int attempt = 1; ; attempt++) {
      try {
        Files.move(
            temporaryFile,
            frontFile,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
        return;
      } catch (AccessDeniedException e) {
        if (attempt == MOVE_ATTEMPTS) {
          Files.deleteIfExists(temporaryFile);
          return;
        }
        try {
          Thread.sleep(MOVE_RETRY_MILLIS);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          Files.deleteIfExists(temporaryFile);
          return;
        }
      }
    }
  }

  @Override
  public String toString() {
    return "Observer that writes the current front of a solve run to a CSV file";
  }
}
