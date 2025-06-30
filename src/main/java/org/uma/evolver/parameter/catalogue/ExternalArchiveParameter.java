package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.BestSolutionsArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.archive.impl.HypervolumeArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.archive.impl.SpatialSpreadDeviationArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;

public class ExternalArchiveParameter<S extends Solution<?>> extends CategoricalParameter {
  private int size;

  public ExternalArchiveParameter(String parameterName, List<String> archiveTypes) {
    super(parameterName, archiveTypes);
  }

  public ExternalArchiveParameter(List<String> archiveTypes) {
    this("externalArchive", archiveTypes);
  }

  public Archive<S> getExternalArchive() {
    Archive<S> archive;

    switch (value()) {
      case "crowdingDistanceArchive" -> archive = new CrowdingDistanceArchive<>(size);
      case "hypervolumeArchive" -> archive = new HypervolumeArchive<>(size, new WFGHypervolume<>());
      case "spatialSpreadDeviationArchive" -> archive = new SpatialSpreadDeviationArchive<>(size);
      case "unboundedArchive" ->
          archive = new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(), size);
      default -> throw new JMetalException("Archive type does not exist: " + name());
    }
    return archive;
  }

  public void setSize(int size) {
    this.size = size;
  }
}
