package org.uma.evolver.meta.encoding.util;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * The initial population of a meta-optimizer that starts from known configurations: the seeds
 * (configurations encoded with {@link ConfigurationEncoding}), and random solutions of the problem
 * up to the size of the population.
 *
 * @param <S> the encoding of the meta-optimizer (flat or tree)
 */
public class SeededSolutionsCreation<S extends Solution<?>> implements SolutionsCreation<S> {

  private final Problem<S> problem;
  private final int populationSize;
  private final List<S> seeds;

  /**
   * @param problem the meta-optimization problem, which creates the random solutions
   * @param populationSize the size of the initial population
   * @param seeds the encoded configurations, at most {@code populationSize}
   */
  public SeededSolutionsCreation(Problem<S> problem, int populationSize, List<S> seeds) {
    Check.notNull(problem);
    Check.that(
        seeds.size() <= populationSize,
        "There are "
            + seeds.size()
            + " initial configurations, more than the population of the meta-optimizer ("
            + populationSize
            + ")");
    this.problem = problem;
    this.populationSize = populationSize;
    this.seeds = List.copyOf(seeds);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<S> create() {
    List<S> population = new ArrayList<>(populationSize);
    for (S seed : seeds) {
      population.add((S) seed.copy());
    }
    while (population.size() < populationSize) {
      population.add(problem.createSolution());
    }
    return population;
  }
}
