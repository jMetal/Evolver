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

/**
 * A categorical parameter representing different aggregation functions used in decomposition-based
 * multi-objective optimization algorithms like MOEA/D.
 * 
 * <p>This parameter allows selecting and configuring the aggregation function used to scalarize
 * multiple objectives into a single scalar value. The available aggregation functions are:
 * <ul>
 *   <li>tschebyscheff: Uses the Tchebysheff approach for aggregation</li>
 *   <li>modifiedTschebyscheff: A modified version of the Tchebysheff approach</li>
 *   <li>weightedSum: Uses a weighted sum approach for aggregation</li>
 *   <li>penaltyBoundaryIntersection: Uses the Penalty-based Boundary Intersection (PBI) approach</li>
 * </ul>
 * 
 * <p>The aggregation function can be configured with normalization of objectives and, in the case of PBI,
 * a theta parameter that controls the balance between convergence and diversity.
 */
public class AggregationFunctionParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "aggregationFunction";

  private boolean normalizedObjectives;

  /**
   * Creates a new AggregationFunctionParameter with the specified aggregation function options.
   * 
   * @param aggregationFunctions A list of valid aggregation function names. Supported values:
   *                           - "tschebyscheff"
   *                           - "modifiedTschebyscheff"
   *                           - "weightedSum"
   *                           - "penaltyBoundaryIntersection"
   * @throws IllegalArgumentException if aggregationFunctions is null or empty
   */
  public AggregationFunctionParameter(List<String> aggregationFunctions) {
    super(DEFAULT_NAME, aggregationFunctions);
    this.normalizedObjectives = false;
  }

  /**
   * Sets whether the objectives should be normalized before aggregation.
   * 
   * @param normalizedObjectives true if objectives should be normalized, false otherwise
   */
  public void normalizedObjectives(boolean normalizedObjectives) {
    this.normalizedObjectives = normalizedObjectives;
  }

  /**
   * Creates and returns an AggregationFunction instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>This method also configures the normalization settings based on the "normalizeObjectives"
   * sub-parameter and, for PBI, the "pbiTheta" sub-parameter.
   * 
   * @return A configured AggregationFunction implementation
   * @throws JMetalException if the current value does not match any known aggregation function
   * @throws IllegalStateException if required sub-parameters are not found
   */
  public AggregationFunction getAggregationFunction() {
    AggregationFunction aggregationFunction;

    CategoricalParameter normalizeObjectivesParameter =
        ((CategoricalParameter) findGlobalSubParameter("normalizeObjectives"));
    boolean normalizeObjectives = (normalizeObjectivesParameter.value()).equalsIgnoreCase("true");

    switch (value()) {
      case "tschebyscheff" -> aggregationFunction = new Tschebyscheff(normalizedObjectives);
      case "modifiedTschebyscheff" ->
          aggregationFunction = new ModifiedTschebyscheff(normalizedObjectives);
      case "weightedSum" -> aggregationFunction = new WeightedSum(normalizedObjectives);
      case "penaltyBoundaryIntersection" -> {
        double theta = (double) findConditionalParameter("pbiTheta").value();
        aggregationFunction = new PenaltyBoundaryIntersection(theta, normalizedObjectives);
      }
      default -> throw new JMetalException("Aggregation function does not exist: " + name());
    }

    if (normalizeObjectives) {
      double epsilon = (double) normalizeObjectivesParameter.findConditionalParameter("epsilonParameterForNormalization").value();
      aggregationFunction.epsilon(epsilon);
    }
    return aggregationFunction;
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The string "aggregationFunction"
   */
  @Override
  public String name() {
    return "aggregationFunction";
  }
}
