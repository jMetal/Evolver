package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.util.aggregationfunction.AggregationFunction;
import org.uma.jmetal.util.aggregationfunction.impl.ModifiedTschebyscheff;
import org.uma.jmetal.util.aggregationfunction.impl.PenaltyBoundaryIntersection;
import org.uma.jmetal.util.aggregationfunction.impl.Tschebyscheff;
import org.uma.jmetal.util.aggregationfunction.impl.WeightedSum;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class AggregationFunctionParameter extends CategoricalParameter {

  private boolean normalizedObjectives;

  public AggregationFunctionParameter(List<String> aggregationFunctions) {
    super("aggregationFunction", aggregationFunctions);

    normalizedObjectives = false;
  }

  public void normalizedObjectives(boolean normalizedObjectives) {
    this.normalizedObjectives = normalizedObjectives;
  }

  public AggregationFunction getAggregationFunction() {
    AggregationFunction aggregationFunction;

    BooleanParameter normalizeObjectivesParameter =
        ((BooleanParameter) findGlobalSubParameter("normalizeObjectives"));
    boolean normalizeObjectives = normalizeObjectivesParameter.value();

    switch (value()) {
      case "tschebyscheff" -> aggregationFunction = new Tschebyscheff(normalizedObjectives);
      case "modifiedTschebyscheff" ->
          aggregationFunction = new ModifiedTschebyscheff(normalizedObjectives);
      case "weightedSum" -> aggregationFunction = new WeightedSum(normalizedObjectives);
      case "penaltyBoundaryIntersection" -> {
        double theta = (double) findSpecificSubParameter("pbiTheta").value();
        aggregationFunction = new PenaltyBoundaryIntersection(theta, normalizedObjectives);
      }
      default -> throw new JMetalException("Aggregation function does not exist: " + name());
    }

    if (normalizeObjectives) {
      double epsilon = (double) normalizeObjectivesParameter.findSpecificSubParameter("epsilonParameterForNormalization").value();
      aggregationFunction.epsilon(epsilon);
    }
    return aggregationFunction;
  }
}
