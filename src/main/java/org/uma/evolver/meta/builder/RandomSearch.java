package org.uma.evolver.meta.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

public class RandomSearch<S extends Solution<?>> implements Algorithm<List<S>> {
  private Problem<S> problem;
  private int maxEvaluations;
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

  public int maxEvaluations() {
    return maxEvaluations;
  }

  public int numberOfCores() {
    return numberOfCores;
  }

  public Observable<Map<String, Object>> observable() {
    return observable;
  }

  @Override
  public void run() {
    AtomicInteger evaluations = new AtomicInteger(0);
    IntStream.range(0, maxEvaluations)
        .parallel()
        .forEach(
            i -> {
              S newSolution = problem.createSolution();
              problem.evaluate(newSolution);

              int currentEvaluations = evaluations.incrementAndGet();

              synchronized (nonDominatedArchive) {
                nonDominatedArchive.add(newSolution);
              }

              synchronized (observable) {
                observable.setChanged();
                Map<String, Object> data = new HashMap<>();
                data.put("EVALUATIONS", currentEvaluations);
                data.put("POPULATION", result());
                data.put("ALGORITHM_NAME", name());
                data.put("PROBLEM_NAME", problem.name());
                // Optional: STRATEGY_PARAMETERS equivalent if needed?
                // Usually 'POPULATION' is enough for standard observers.
                observable.notifyObservers(data);
              }
            });
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
