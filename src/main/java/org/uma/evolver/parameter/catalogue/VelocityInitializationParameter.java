package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.VelocityInitialization;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.impl.DefaultVelocityInitialization;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.impl.SPSO2007VelocityInitialization;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.impl.SPSO2011VelocityInitialization;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different velocity initialization strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting various velocity initialization mechanisms used at the start of the PSO algorithm.
 * 
 * <p>The available velocity initialization strategies are:
 * <ul>
 *   <li>defaultVelocityInitialization: Standard velocity initialization</li>
 *   <li>SPSO2007VelocityInitialization: Velocity initialization following the Standard PSO 2007 specifications</li>
 *   <li>SPSO2011VelocityInitialization: Velocity initialization following the Standard PSO 2011 specifications</li>
 * </ul>
 * 
 * <p>Each strategy initializes particle velocities according to different methodologies, which can affect
 * the exploration/exploitation behavior of the PSO algorithm.
 */
public class VelocityInitializationParameter extends CategoricalParameter {

  /**
   * Creates a new VelocityInitializationParameter with the specified valid values.
   * 
   * @param variationStrategies A list of valid velocity initialization strategy names. Typical values include:
   *                          "defaultVelocityInitialization", "SPSO2007VelocityInitialization", "SPSO2011VelocityInitialization"
   * @throws IllegalArgumentException if variationStrategies is null or empty
   */
  public VelocityInitializationParameter(List<String> variationStrategies) {
    super("velocityInitialization", variationStrategies);
  }

  /**
   * Creates and returns a VelocityInitialization instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @return A configured VelocityInitialization implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known velocity initialization strategy
   */
  public VelocityInitialization getVelocityInitialization() {
    VelocityInitialization result;

    switch (value()) {
      case "defaultVelocityInitialization" -> {
        result = new DefaultVelocityInitialization();
      }
      case "SPSO2007VelocityInitialization" -> {
        result = new SPSO2007VelocityInitialization();
      }
      case "SPSO2011VelocityInitialization" -> {
        result = new SPSO2011VelocityInitialization();
      }
      default -> throw new JMetalException("Velocity initialization component unknown: " + value());
    }

    return result;
  }

  /**
   * Returns the name of this parameter.
   * 
   * @return The string "velocityInitialization"
   */
  @Override
  public String name() {
    return "velocityInitialization";
  }
}
