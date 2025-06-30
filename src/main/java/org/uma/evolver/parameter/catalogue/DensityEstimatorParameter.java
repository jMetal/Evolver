package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.KnnDensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class DensityEstimatorParameter<S extends Solution<?>> extends CategoricalParameter {

  public DensityEstimatorParameter(String name, List<String> validDensityEstimators) {
    super(name, validDensityEstimators);
  }

  public DensityEstimator<S> getDensityEstimator() {
    DensityEstimator<S> result;
    switch (value()) {
      case "crowdingDistance":
        result = new CrowdingDistanceDensityEstimator<>();
        break;
      case "knn":
        boolean normalizeObjectives =
            (Boolean) findSpecificSubParameter("knnNormalizeObjectives").value();
        int knnNeighborhoodSize =
            (Integer) findSpecificSubParameter("knnNeighborhoodSize").value();
        result = new KnnDensityEstimator<>(knnNeighborhoodSize, normalizeObjectives);
        break;
      default:
        throw new JMetalException("Density estimator does not exist: " + name());
    }
    return result;
  }
}
