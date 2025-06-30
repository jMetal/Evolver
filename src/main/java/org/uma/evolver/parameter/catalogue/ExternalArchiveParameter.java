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

/**
 * A categorical parameter representing different external archive strategies for multi-objective optimization algorithms.
 * This parameter allows selecting and configuring the archive used to store non-dominated solutions during optimization.
 * 
 * <p>The available archive types are:
 * <ul>
 *   <li>crowdingDistanceArchive: Maintains diversity using crowding distance</li>
 *   <li>hypervolumeArchive: Selects solutions based on hypervolume contribution</li>
 *   <li>spatialSpreadDeviationArchive: Maintains diversity using spatial spread deviation</li>
 *   <li>unboundedArchive: Uses a non-dominated solution list archive with best solutions</li>
 * </ul>
 * 
 * @param <S> The type of solutions stored in the archive
 */
public class ExternalArchiveParameter<S extends Solution<?>> extends CategoricalParameter {
  private int size;

  /**
   * Creates a new ExternalArchiveParameter with the specified parameter name and valid archive types.
   * 
   * @param parameterName The name of the parameter
   * @param archiveTypes A list of valid archive type names. Supported values:
   *                    - "crowdingDistanceArchive"
   *                    - "hypervolumeArchive"
   *                    - "spatialSpreadDeviationArchive"
   *                    - "unboundedArchive"
   * @throws IllegalArgumentException if parameterName is null or empty, or if archiveTypes is null or empty
   */
  public ExternalArchiveParameter(String parameterName, List<String> archiveTypes) {
    super(parameterName, archiveTypes);
  }

  /**
   * Creates a new ExternalArchiveParameter with the default parameter name "externalArchive" and the specified archive types.
   * 
   * @param archiveTypes A list of valid archive type names
   * @throws IllegalArgumentException if archiveTypes is null or empty
   */
  public ExternalArchiveParameter(List<String> archiveTypes) {
    this("externalArchive", archiveTypes);
  }

  /**
   * Creates and returns an Archive instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Note: The size of the archive must be set using {@link #setSize(int)} before calling this method.
   * 
   * @return A configured Archive implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known archive type
   * @throws IllegalStateException if the archive size has not been set
   */
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

  /**
   * Sets the maximum size of the external archive.
   * This method must be called before {@link #getExternalArchive()}.
   * 
   * @param size The maximum number of solutions the archive can hold
   * @throws IllegalArgumentException if size is less than or equal to zero
   */
  public void setSize(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Archive size must be greater than zero");
    }
    this.size = size;
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The name of this parameter as specified in the constructor
   */
  @Override
  public String name() {
    return super.name();
  }
}
