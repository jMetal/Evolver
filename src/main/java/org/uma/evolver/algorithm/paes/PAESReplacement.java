package org.uma.evolver.algorithm.paes;

import java.util.Comparator;
import java.util.List;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.BoundedArchive;

/**
 * Replacement strategy for PAES (Pareto Archived Evolution Strategy).
 *
 * <p>Three-way logic on each call:
 * <ol>
 *   <li>Offspring dominates current → accept offspring, add to archive.</li>
 *   <li>Current dominates offspring → keep current (no change).</li>
 *   <li>Neither dominates → try adding offspring to archive; if accepted, use the archive's
 *       density comparator to choose between current and offspring.</li>
 * </ol>
 *
 * <p>The current solution is added to the archive on every call so that the initial solution is
 * tracked from the first iteration without requiring a separate initialization step.
 *
 * @param <S> the solution type
 */
public class PAESReplacement<S extends Solution<?>> implements Replacement<S> {
  private final BoundedArchive<S> paesArchive;
  private final Comparator<S> dominanceComparator;

  public PAESReplacement(BoundedArchive<S> paesArchive, Comparator<S> dominanceComparator) {
    this.paesArchive = paesArchive;
    this.dominanceComparator = dominanceComparator;
  }

  @Override
  public List<S> replace(List<S> population, List<S> offspringPopulation) {
    S current = population.get(0);
    S offspring = offspringPopulation.get(0);

    // Ensure current is tracked (handles the initial solution on the first call; no-op thereafter)
    paesArchive.add(current);

    int flag = dominanceComparator.compare(current, offspring);
    if (flag > 0) {
      // offspring dominates current
      paesArchive.add(offspring);
      population.set(0, offspring);
    } else if (flag == 0) {
      // neither dominates the other: use archive as tiebreaker
      if (paesArchive.add(offspring)) {
        // BoundedArchive.prune() only computes density estimators when the archive is full;
        // force computation here so density attributes are always set before the comparator reads them.
        paesArchive.computeDensityEstimator();
        if (paesArchive.comparator().compare(current, offspring) > 0) {
          population.set(0, offspring);
        }
      }
    }
    // flag < 0: current dominates offspring → no change
    return population;
  }
}
