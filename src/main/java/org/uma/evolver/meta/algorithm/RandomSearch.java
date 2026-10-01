package org.uma.evolver.meta.algorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.uma.evolver.meta.builder.ComputingTimeLimit;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

/**
 * Random search, bounded either by a number of evaluations or by computing time (see {@link
 * #byComputingTime}).
 *
 * <p>With a time limit the evaluations are done in batches of {@code numberOfCores} solutions and
 * the limit is checked before each batch, so the batch in progress is always completed and at least
 * one batch is evaluated.
 */
public class RandomSearch<S extends Solution<?>> implements Algorithm<List<S>> {
  private Problem<S> problem;
  private int maxEvaluations;
  private long maxComputingTimeMillis = 0;
  private int evaluationsDone = 0;
  private long computingTimeMillis = 0;
  private long runStartMillis;
  private NonDominatedSolutionListArchive<S> nonDominatedArchive;
  private int numberOfCores;
  private Observable<Map<String, Object>> observable;

  /** Constructor */
  public RandomSearch(Problem<S> problem, int maxEvaluations) {
    this(problem, maxEvaluations, 1);
  }

  /** Constructor */
  public RandomSearch(Problem<S> problem, int maxEvaluations, int numberOfCores) {
    this.problem = problem;
    this.maxEvaluations = maxEvaluations;
    this.numberOfCores = numberOfCores;
    nonDominatedArchive = new NonDominatedSolutionListArchive<S>();
    observable = new DefaultObservable<>("Random Search Observable");
  }

  /**
   * Creates a random search bounded by computing time.
   *
   * @param problem the problem
   * @param maxComputingTimeMinutes the limit in minutes, with decimals (greater than zero)
   * @param numberOfCores the cores, which are also the size of each batch of evaluations
   */
  public static <S extends Solution<?>> RandomSearch<S> byComputingTime(
      Problem<S> problem, double maxComputingTimeMinutes, int numberOfCores) {
    var randomSearch = new RandomSearch<>(problem, 0, numberOfCores);
    randomSearch.maxComputingTimeMillis =
        ComputingTimeLimit.toDuration(maxComputingTimeMinutes).toMillis();
    return randomSearch;
  }

  public int maxEvaluations() {
    return maxEvaluations;
  }

  /** The limit on the computing time in milliseconds, or 0 if the search is bounded by evaluations. */
  public long maxComputingTimeMillis() {
    return maxComputingTimeMillis;
  }

  /** The number of evaluations done by the last run. */
  public int numberOfEvaluations() {
    return evaluationsDone;
  }

  /** The computing time of the last run in milliseconds. */
  public long totalComputingTime() {
    return computingTimeMillis;
  }

  public int numberOfCores() {
    return numberOfCores;
  }

  public Observable<Map<String, Object>> observable() {
    return observable;
  }

  @Override
  public void run() {
    long start = System.currentTimeMillis();
    runStartMillis = start;
    AtomicInteger evaluations = new AtomicInteger(0);
    ForkJoinPool pool = new ForkJoinPool(numberOfCores);
    try {
      if (maxComputingTimeMillis > 0) {
        do {
          evaluateBatch(pool, numberOfCores, evaluations);
        } while (System.currentTimeMillis() - start < maxComputingTimeMillis);
      } else {
        evaluateBatch(pool, maxEvaluations, evaluations);
      }
    } finally {
      pool.shutdown();
      evaluationsDone = evaluations.get();
      computingTimeMillis = System.currentTimeMillis() - start;
    }
  }

  private void evaluateBatch(ForkJoinPool pool, int numberOfSolutions, AtomicInteger evaluations) {
    pool.submit(
            () ->
                IntStream.range(0, numberOfSolutions)
                    .parallel()
                    .forEach(
                        i -> {
                          S newSolution = problem.createSolution();
                          problem.evaluate(newSolution);

                          int currentEvaluations = evaluations.incrementAndGet();
                          List<S> populationSnapshot;
                          synchronized (nonDominatedArchive) {
                            nonDominatedArchive.add(newSolution);
                            populationSnapshot = result();
                          }

                          synchronized (observable) {
                            observable.setChanged();
                            Map<String, Object> data = new HashMap<>();
                            data.put("EVALUATIONS", currentEvaluations);
                            data.put("COMPUTING_TIME", System.currentTimeMillis() - runStartMillis);
                            data.put("POPULATION", populationSnapshot);
                            data.put("ALGORITHM_NAME", name());
                            data.put("PROBLEM_NAME", problem.name());
                            observable.notifyObservers(data);
                          }
                        }))
        .join();
  }

  @Override
  public List<S> result() {
    return nonDominatedArchive.solutions();
  }

  @Override
  public String name() {
    return "RS";
  }

  @Override
  public String description() {
    return "Multi-objective random search algorithm";
  }
}
