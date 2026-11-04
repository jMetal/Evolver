package org.uma.evolver.util.constrainthandling;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.AngleArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.HypervolumeArchive;
import org.uma.jmetal.util.archive.impl.KNNDistanceArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.archive.impl.SpatialSpreadDeviationArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DominanceWithConstraintsComparator;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;

/**
 * jMetal's bounded archives compare solutions with plain Pareto dominance unless they are given a
 * comparator, and some of them cannot be given one, so on a problem with constraints they keep an
 * infeasible solution that dominates a feasible one and drop the feasible one. The archives built
 * here compare with Deb's constrained-domination principle, which is plain Pareto dominance on
 * problems without constraints. The archives without a constructor that takes a comparator get the
 * constrained non-dominated archive in place of their own, empty, one when they are built.
 */
public final class ConstrainedBoundedArchives {

  private ConstrainedBoundedArchives() {}

  /**
   * A crowding distance archive with the constrained-domination principle.
   *
   * @param size the maximum size of the archive
   * @param <S> the type of the solutions
   * @return the archive
   */
  public static <S extends Solution<?>> CrowdingDistanceArchive<S> crowdingDistance(int size) {
    return new CrowdingDistanceArchive<>(size, new DominanceWithConstraintsComparator<>());
  }

  /**
   * An angle archive with the constrained-domination principle.
   *
   * @param size the maximum size of the archive
   * @param normalize whether the objectives are normalized
   * @param <S> the type of the solutions
   * @return the archive
   */
  public static <S extends Solution<?>> AngleArchive<S> angle(int size, boolean normalize) {
    return new AngleArchive<>(size, null, normalize, 2, new DominanceWithConstraintsComparator<>());
  }

  /**
   * A spatial spread deviation archive with the constrained-domination principle.
   *
   * @param size the maximum size of the archive
   * @param <S> the type of the solutions
   * @return the archive
   */
  public static <S extends Solution<?>> SpatialSpreadDeviationArchive<S> spatialSpreadDeviation(
      int size) {
    return new SpatialSpreadDeviationArchive<>(size) {
      {
        archive = constrainedArchive();
      }
    };
  }

  /**
   * A k-nearest-neighbour distance archive with the constrained-domination principle.
   *
   * @param size the maximum size of the archive
   * @param k the number of neighbours of the density estimator
   * @param <S> the type of the solutions
   * @return the archive
   */
  public static <S extends Solution<?>> KNNDistanceArchive<S> knnDistance(int size, int k) {
    return new KNNDistanceArchive<>(size, k) {
      {
        archive = constrainedArchive();
      }
    };
  }

  /**
   * A hypervolume archive with the constrained-domination principle.
   *
   * @param size the maximum size of the archive
   * @param <S> the type of the solutions
   * @return the archive
   */
  public static <S extends Solution<?>> HypervolumeArchive<S> hypervolume(int size) {
    return new HypervolumeArchive<>(size, new WFGHypervolume<>()) {
      {
        archive = constrainedArchive();
      }
    };
  }

  private static <S extends Solution<?>> NonDominatedSolutionListArchive<S> constrainedArchive() {
    return new NonDominatedSolutionListArchive<>(new DominanceWithConstraintsComparator<>());
  }
}
