package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.util.constrainthandling.ConstrainedBoundedArchives;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.AngleArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.HypervolumeArchive;
import org.uma.jmetal.util.archive.impl.KNNDistanceArchive;
import org.uma.jmetal.util.archive.impl.SpatialSpreadDeviationArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;

/**
 * A categorical parameter representing the PAES density archive type.
 *
 * <p>Returns a {@link BoundedArchive} (not just {@link org.uma.jmetal.util.archive.Archive}),
 * which is required by {@link org.uma.jmetal.component.catalogue.ea.replacement.impl.PAESReplacement}
 * to access the archive's density comparator for non-dominated tiebreaking.
 *
 * <p>The {@code unboundedArchive} option uses a very large {@link CrowdingDistanceArchive} and is
 * intended for problems with three or more objectives where a bounded archive is impractical.
 *
 * @param <S> the solution type
 */
public class PAESArchiveParameter<S extends Solution<?>> extends CategoricalParameter {
  private int size = 100;

  public PAESArchiveParameter(String parameterName, List<String> archiveTypes) {
    super(parameterName, archiveTypes);
  }

  public BoundedArchive<S> getBoundedArchive() {
    return switch (value()) {
      case "crowdingDistanceArchive" -> new CrowdingDistanceArchive<>(size);
      case "hypervolumeArchive" -> new HypervolumeArchive<>(size, new WFGHypervolume<>());
      case "spatialSpreadDeviationArchive" -> new SpatialSpreadDeviationArchive<>(size);
      case "knnDistanceArchive" -> {
        int k = (Integer) findConditionalParameter("knnDistanceArchiveK").value();
        yield new KNNDistanceArchive<>(size, k);
      }
      case "angleArchive" -> new AngleArchive<>(size, true);
      default -> throw new JMetalException("Unknown PAES archive type: " + value());
    };
  }

  /**
   * The archive of the current value, comparing solutions with the constrained-domination
   * principle (see {@link ConstrainedBoundedArchives}), for PAES with a constraint-handling
   * technique: the archive is the result of PAES, so it must not keep an infeasible solution that
   * dominates a feasible one.
   *
   * @return the archive
   */
  public BoundedArchive<S> getConstrainedBoundedArchive() {
    return switch (value()) {
      case "crowdingDistanceArchive" -> ConstrainedBoundedArchives.crowdingDistance(size);
      case "hypervolumeArchive" -> ConstrainedBoundedArchives.hypervolume(size);
      case "spatialSpreadDeviationArchive" ->
          ConstrainedBoundedArchives.spatialSpreadDeviation(size);
      case "knnDistanceArchive" -> {
        int k = (Integer) findConditionalParameter("knnDistanceArchiveK").value();
        yield ConstrainedBoundedArchives.knnDistance(size, k);
      }
      case "angleArchive" -> ConstrainedBoundedArchives.angle(size, true);
      default -> throw new JMetalException("Unknown PAES archive type: " + value());
    };
  }

  public void setSize(int size) {
    this.size = size;
  }
}
