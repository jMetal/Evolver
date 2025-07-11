package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.positionupdate.PositionUpdate;
import org.uma.jmetal.component.catalogue.pso.positionupdate.impl.DefaultPositionUpdate;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different position update strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring position update mechanisms used in PSO algorithms.
 * 
 * <p>The available position update strategies are:
 * <ul>
 *   <li>defaultPositionUpdate: Standard position update that applies velocity to current position
 *       while respecting the defined position bounds</li>
 * </ul>
 * 
 * <p>The default position update strategy requires position bounds and velocity change parameters
 * to handle boundary conditions when particles move beyond the defined search space.
 */
public class PositionUpdateParameter extends CategoricalParameter {
  private static final String DEFAULT_NAME = "positionUpdate";
  /**
   * Creates a new PositionUpdateParameter with the specified valid values.
   * 
   * @param positionUpdateStrategies A list of valid position update strategy names.
   *                                Currently supports: "defaultPositionUpdate"
   * @throws IllegalArgumentException if positionUpdateStrategies is null or empty
   */
  public PositionUpdateParameter(List<String> positionUpdateStrategies) {
    super(DEFAULT_NAME, positionUpdateStrategies);
  }

  /**
   * Creates and returns a PositionUpdate instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Required sub-parameters for the default strategy:
   * <ul>
   *   <li>positionBounds: List of Bounds&lt;Double&gt; defining the search space limits</li>
   *   <li>velocityChangeWhenLowerLimitIsReached: double value for velocity adjustment at lower bounds</li>
   *   <li>velocityChangeWhenUpperLimitIsReached: double value for velocity adjustment at upper bounds</li>
   * </ul>
   * 
   * @return A configured PositionUpdate implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known position update strategy
   * @throws ClassCastException if any required sub-parameter has an incorrect type
   * @throws NullPointerException if any required sub-parameter is not found
   */
  public PositionUpdate getPositionUpdate() {
    PositionUpdate result;
    switch (value()) {
      case "defaultPositionUpdate":
        List<Bounds<Double>> positionBounds =
            (List<Bounds<Double>>) nonConfigurableSubParameters().get("positionBounds");
        double velocityChangeWhenLowerLimitIsReached =
            (double) findConditionalSubParameter("velocityChangeWhenLowerLimitIsReached").value();
        double velocityChangeWhenUpperLimitIsReached =
            (double) findConditionalSubParameter("velocityChangeWhenUpperLimitIsReached").value();

        result =
            new DefaultPositionUpdate(
                velocityChangeWhenLowerLimitIsReached,
                velocityChangeWhenUpperLimitIsReached,
                positionBounds);
        break;
      default:
        throw new JMetalException("Position update component unknown: " + value());
    }
    return result;
  }
}
