package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.InertiaWeightComputingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.ConstantValueStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.LinearDecreasingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.LinearIncreasingStrategy;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.impl.RandomSelectedValueStrategy;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different inertia weight computing strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how the inertia weight is calculated during the PSO execution.
 * 
 * <p>The available inertia weight computing strategies are:
 * <ul>
 *   <li>constantValue: Uses a fixed inertia weight throughout the optimization</li>
 *   <li>randomSelectedValue: Randomly selects inertia weight from a specified range in each iteration</li>
 *   <li>linearDecreasingValue: Linearly decreases the inertia weight from a maximum to a minimum value</li>
 *   <li>linearIncreasingValue: Linearly increases the inertia weight from a minimum to a maximum value</li>
 * </ul>
 * 
 * <p>The inertia weight controls the exploration-exploitation trade-off in PSO. Higher values promote exploration,
 * while lower values favor exploitation of the search space.
 */
public class InertiaWeightComputingParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "inertiaWeightComputingStrategy";
  
  /**
   * Creates a new InertiaWeightComputingParameter with the specified valid values.
   * 
   * @param inertiaWeightStrategies A list of valid inertia weight strategy names. Supported values:
   *                              - "constantValue"
   *                              - "randomSelectedValue"
   *                              - "linearDecreasingValue"
   *                              - "linearIncreasingValue"
   * @throws IllegalArgumentException if inertiaWeightStrategies is null or empty
   */
  public InertiaWeightComputingParameter(List<String> inertiaWeightStrategies) {
    super(DEFAULT_NAME, inertiaWeightStrategies);
  }

  /**
   * Creates and returns an InertiaWeightComputingStrategy instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Required sub-parameters for each strategy:
   * <ul>
   *   <li>constantValue: "weight" (Double)</li>
   *   <li>randomSelectedValue: "weightMin" and "weightMax" (Double)</li>
   *   <li>linearDecreasingValue/linearIncreasingValue: 
   *     "weightMin", "weightMax" (Double), and non-configurable parameters "maxIterations" and "swarmSize" (Integer)
   *   </li>
   * </ul>
   * 
   * @return A configured InertiaWeightComputingStrategy implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known inertia weight strategy
   * @throws ClassCastException if any required sub-parameter has an incorrect type
   * @throws NullPointerException if any required sub-parameter is not found
   */
  public InertiaWeightComputingStrategy getInertiaWeightComputingStrategy() {
    return switch (value()) {
      case "constantValue" -> {
        Double weight = (Double) findConditionalParameter("inertiaWeight").value();
        yield new ConstantValueStrategy(weight);
      }
      case "randomSelectedValue" -> {
        Double weightMin = (Double) findConditionalParameter("inertiaWeightMin").value();
        Double weightMax = (Double) findConditionalParameter("inertiaWeightMax").value();
        yield new RandomSelectedValueStrategy(weightMin, weightMax);
      }
      case "linearDecreasingValue" -> {
        Double weightMin = (Double) findConditionalParameter("inertiaWeightMin").value();
        Double weightMax = (Double) findConditionalParameter("inertiaWeightMax").value();
        int iterations = (Integer) nonConfigurableSubParameters().get("maxIterations");
        int swarmSize = (Integer) nonConfigurableSubParameters().get("swarmSize");
        yield new LinearDecreasingStrategy(weightMin, weightMax, iterations, swarmSize);
      }
      case "linearIncreasingValue" -> {
        Double weightMin = (Double) findConditionalParameter("inertiaWeightMin").value();
        Double weightMax = (Double) findConditionalParameter("inertiaWeightMax").value();
        int iterations = (Integer) nonConfigurableSubParameters().get("maxIterations");
        int swarmSize = (Integer) nonConfigurableSubParameters().get("swarmSize");
        yield new LinearIncreasingStrategy(weightMin, weightMax, iterations, swarmSize);
      }
      default ->
          throw new JMetalException("Inertia weight computing strategy does not exist: " + name());
    };
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The string "inertiaWeightComputingStrategy"
   */
  @Override
  public String name() {
    return "inertiaWeightComputingStrategy";
  }
}
