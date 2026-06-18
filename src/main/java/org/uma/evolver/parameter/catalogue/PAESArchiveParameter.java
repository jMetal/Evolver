package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.HypervolumeArchive;
import org.uma.jmetal.util.archive.impl.SpatialSpreadDeviationArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;

/**
 * A categorical parameter representing the PAES density archive type.
 *
 * <p>Returns a {@link BoundedArchive} (not just {@link org.uma.jmetal.util.archive.Archive}),
 * which is required by {@link org.uma.evolver.algorithm.paes.PAESReplacement} to access the
 * archive's density comparator for non-dominated tiebreaking.
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
      default -> throw new JMetalException("Unknown PAES archive type: " + value());
    };
  }

  public void setSize(int size) {
    this.size = size;
  }
}
