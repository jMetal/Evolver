package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithBoundValue;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithOppositeBoundValue;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithRandomValue;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different repair strategies for double-solution variables
 * that fall outside their defined bounds. This parameter allows selecting and configuring
 * how out-of-bounds values in double solutions should be handled during the optimization process.
 *
 * <p>The available repair strategies are:
 * <ul>
 *   <li>random: Replaces out-of-bounds values with a random value within the bounds</li>
 *   <li>bounds: Clips the value to the nearest bound (upper or lower)</li>
 *   <li>round: Wraps the value around to the opposite bound (useful for cyclic variables)</li>
 * </ul>
 *
 * <p>This is particularly useful in evolutionary algorithms when mutation or crossover operators
 * might generate solutions with variables outside their defined bounds.
 */
public class RepairDoubleSolutionStrategyParameter extends CategoricalParameter {
  private static final String DEFAULT_NAME = "repairDoubleSolutionStrategy";
  /**
   * Creates a new RepairDoubleSolutionStrategyParameter with the specified name and repair strategies.
   *
   * @param name The name of the parameter
   * @param strategies A list of valid repair strategy names. Supported values:
   *                  - "random": Replaces with random value within bounds
   *                  - "bounds": Clips to nearest bound
   *                  - "round": Wraps to opposite bound
   * @throws IllegalArgumentException if name is null or empty, or if strategies is null or empty
   */
  public RepairDoubleSolutionStrategyParameter(String name, List<String> strategies) {
    super(name, strategies);
  }

  /**
   * Creates a new RepairDoubleSolutionStrategyParameter with the specified repair strategies.
   *
   * @param strategies A list of valid repair strategy names. Supported values:
   *                   - "random": Replaces with random value within bounds
   *                   - "bounds": Clips to nearest bound
   *                   - "round": Wraps to opposite bound
   * @throws IllegalArgumentException if strategies is null or empty
   */
  public RepairDoubleSolutionStrategyParameter(List<String> strategies) {
    this(DEFAULT_NAME, strategies);
  }

  /**
   * Creates and returns a RepairDoubleSolution instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   *
   * @return A configured RepairDoubleSolution implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known repair strategy
   */
  public RepairDoubleSolution getRepairDoubleSolutionStrategy() {
    RepairDoubleSolution result;
    switch (value()) {
      case "random" -> result = new RepairDoubleSolutionWithRandomValue();
      case "bounds" -> result = new RepairDoubleSolutionWithBoundValue();
      case "round" -> result = new RepairDoubleSolutionWithOppositeBoundValue();
      default -> throw new JMetalException("Repair strategy unknown: " + name());
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
