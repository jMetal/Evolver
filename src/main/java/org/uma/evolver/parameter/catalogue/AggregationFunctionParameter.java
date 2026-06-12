package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.util.aggregationfunction.AggregationFunction;
import org.uma.jmetal.util.aggregationfunction.impl.AugmentedTschebyscheff;
import org.uma.jmetal.util.aggregationfunction.impl.InvertedPenaltyBoundaryIntersection;
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
 *   <li>augmentedTschebyscheff: Tchebysheff augmented with a small weighted-sum term</li>
 *   <li>weightedSum: Uses a weighted sum approach for aggregation</li>
 *   <li>penaltyBoundaryIntersection: Uses the Penalty-based Boundary Intersection (PBI) approach</li>
 *   <li>invertedPenaltyBoundaryIntersection: Uses the Inverted PBI (IPBI) approach. As it measures
 *       displacements from the nadir point, objective normalization is always enforced for this
 *       function, regardless of the "normalizeObjectives" setting</li>
 * </ul>
 *
 * <p>The aggregation function can be configured with normalization of objectives and, in the case
 * of PBI and IPBI, a theta parameter that controls the balance between convergence and diversity.
 */
public class AggregationFunctionParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "aggregationFunction";

  /**
   * Creates a new AggregationFunctionParameter with the specified aggregation function options.
   * 
   * @param aggregationFunctions A list of valid aggregation function names. Supported values:
   *                           - "tschebyscheff"
   *                           - "modifiedTschebyscheff"
   *                           - "augmentedTschebyscheff"
   *                           - "weightedSum"
   *                           - "penaltyBoundaryIntersection"
   *                           - "invertedPenaltyBoundaryIntersection"
   * @throws IllegalArgumentException if aggregationFunctions is null or empty
   */
  public AggregationFunctionParameter(List<String> aggregationFunctions) {
    this(DEFAULT_NAME, aggregationFunctions);
  }

  /**
   * Creates a new AggregationFunctionParameter with the specified name and aggregation function options.
   *
   * @param name The name of the parameter
   * @param aggregationFunctions A list of valid aggregation function names
   * @throws IllegalArgumentException if aggregationFunctions is null or empty
   */
  public AggregationFunctionParameter(String name, List<String> aggregationFunctions) {
    super(name, aggregationFunctions);
  }

  /**
   * Creates and returns an AggregationFunction instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>This method also configures the normalization settings based on the "normalizeObjectives"
   * sub-parameter and the theta sub-parameters of PBI ("pbiTheta") and IPBI ("ipbiTheta"). For
   * IPBI, normalization is always enabled because the function requires the nadir point
   * estimation, which is only maintained when objectives are normalized.
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

    aggregationFunction = switch (value()) {
      case "tschebyscheff" -> new Tschebyscheff(normalizeObjectives);
      case "modifiedTschebyscheff" -> new ModifiedTschebyscheff(normalizeObjectives);
      case "augmentedTschebyscheff" -> new AugmentedTschebyscheff(normalizeObjectives);
      case "weightedSum" -> new WeightedSum(normalizeObjectives);
      case "penaltyBoundaryIntersection" -> {
        double theta = (double) findConditionalParameter("pbiTheta").value();
        yield new PenaltyBoundaryIntersection(theta, normalizeObjectives);
      }
      case "invertedPenaltyBoundaryIntersection" -> {
        double theta = (double) findConditionalParameter("ipbiTheta").value();
        // IPBI measures displacements from the nadir point, which MOEADReplacement only tracks
        // when normalization is enabled, so normalization is always forced for this function.
        yield new InvertedPenaltyBoundaryIntersection(theta, true);
      }
      default -> throw new JMetalException("Aggregation function does not exist: " + name());
    };

    if (normalizeObjectives) {
      double epsilon = (double) normalizeObjectivesParameter.findConditionalParameter("epsilonParameterForNormalization").value();
      aggregationFunction.epsilon(epsilon);
    }
    return aggregationFunction;
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
