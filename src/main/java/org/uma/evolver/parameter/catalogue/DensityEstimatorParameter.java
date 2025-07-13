package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.KnnDensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different density estimator strategies for solutions in
 * multi-objective optimization. This parameter allows selecting and configuring how solution
 * density is estimated, which is crucial for maintaining diversity in the population.
 *
 * <p>The available density estimator strategies are:
 * <ul>
 *   <li>crowdingDistance: Uses crowding distance to estimate solution density (used in NSGA-II)</li>
 *   <li>knn: Uses k-nearest neighbors to estimate solution density (used in some variants of MOEA/D)</li>
 * </ul>
 *
 * <p>For the "knn" strategy, the following sub-parameters are required:
 * <ul>
 *   <li>knnNeighborhoodSize: The number of nearest neighbors to consider</li>
 *   <li>knnNormalizeObjectives: Whether to normalize objectives before computing distances</li>
 * </ul>
 *
 * @param <S> The type of solutions being evaluated
 */
public class DensityEstimatorParameter<S extends Solution<?>> extends CategoricalParameter {
  public static final String DEFAULT_NAME = "densityEstimator";

  /**
   * Creates a new DensityEstimatorParameter with the specified name and valid density estimator strategies.
   *
   * @param name The name of the parameter
   * @param validDensityEstimators A list of valid density estimator strategy names. Supported values:
   *                              - "crowdingDistance"
   *                              - "knn" (requires additional sub-parameters)
   * @throws IllegalArgumentException if name is null or empty, or if validDensityEstimators is null or empty
   */
  public DensityEstimatorParameter(String name, List<String> validDensityEstimators) {
    super(name, validDensityEstimators);
  }

  public DensityEstimatorParameter(List<String> validDensityEstimators) {
    this(DEFAULT_NAME, validDensityEstimators);
  }
  
  /**
   * Creates and returns a DensityEstimator instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   *
   * <p>For the "knn" strategy, this method will look for the following sub-parameters:
   * <ul>
   *   <li>knnNeighborhoodSize: The number of nearest neighbors to consider (must be a positive integer)</li>
   *   <li>knnNormalizeObjectives: Whether to normalize objectives before computing distances (boolean)</li>
   * </ul>
   *
   * @return A configured DensityEstimator implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known density estimator
   * @throws IllegalStateException if required sub-parameters are missing or have invalid values
   */
  public DensityEstimator<S> getDensityEstimator() {
    DensityEstimator<S> result;
    switch (value()) {
      case "crowdingDistance":
        result = new CrowdingDistanceDensityEstimator<>();
        break;
      case "knn":
        boolean normalizeObjectives =
            (Boolean) findConditionalParameter("knnNormalizeObjectives").value();
        int knnNeighborhoodSize =
            (Integer) findConditionalParameter("knnNeighborhoodSize").value();
        result = new KnnDensityEstimator<>(knnNeighborhoodSize, normalizeObjectives);
        break;
      default:
        throw new JMetalException("Density estimator does not exist: " + name());
    }
    return result;
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
