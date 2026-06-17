package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.AngleDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.KnnDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.ShiftedDensityEstimator;
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
 *   <li>shifted: Uses shift-based density estimation (SDE), measuring the distance to the k-th nearest
 *       neighbor after shifting objectives; designed to preserve diversity in many-objective problems</li>
 *   <li>angle: Uses angle-based density estimation, measuring the angles between solutions (from the
 *       origin) in the normalized objective space; objectives are always normalized</li>
 * </ul>
 *
 * <p>For the "knn" strategy, the following sub-parameters are required:
 * <ul>
 *   <li>knnNeighborhoodSize: The number of nearest neighbors to consider</li>
 *   <li>knnNormalizeObjectives: Whether to normalize objectives before computing distances</li>
 * </ul>
 *
 * <p>For the "shifted" strategy, the following sub-parameter is required:
 * <ul>
 *   <li>shiftedNeighborhoodSize: The k-th nearest neighbor used to estimate density</li>
 * </ul>
 *
 * <p>For the "angle" strategy, the following sub-parameter is required:
 * <ul>
 *   <li>angleNeighborhoodSize: The number of nearest angular neighbors used to estimate density</li>
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

  /**
   * Creates a new DensityEstimatorParameter with the specified valid density estimator strategies.
   *
   * @param validDensityEstimators A list of valid density estimator strategy names. Supported values:
   *                              - "crowdingDistance"
   *                              - "knn" (requires additional sub-parameters)
   * @throws IllegalArgumentException if validDensityEstimators is null or empty
   */
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
    return switch (value()) {
      case "crowdingDistance" -> new CrowdingDistanceDensityEstimator<>();
      case "knn" -> {
        String value = (String) findConditionalParameter("knnNormalizeObjectives").value();
        boolean normalizeObjectives = value.toLowerCase().equals("true");
        int knnNeighborhoodSize =
            (Integer) findConditionalParameter("knnNeighborhoodSize").value();
        yield new KnnDensityEstimator<>(knnNeighborhoodSize, normalizeObjectives);
      }
      case "shifted" -> {
        int shiftedNeighborhoodSize =
            (Integer) findConditionalParameter("shiftedNeighborhoodSize").value();
        yield new ShiftedDensityEstimator<>(shiftedNeighborhoodSize);
      }
      case "angle" -> {
        int angleNeighborhoodSize =
            (Integer) findConditionalParameter("angleNeighborhoodSize").value();
        // Objectives are always normalized: angles are measured from the origin, which only
        // corresponds to the ideal point in the [0,1] normalized objective space.
        yield new AngleDensityEstimator<>(null, true, angleNeighborhoodSize);
      }
      default -> throw new JMetalException("Density estimator does not exist: " + value());
    };
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
