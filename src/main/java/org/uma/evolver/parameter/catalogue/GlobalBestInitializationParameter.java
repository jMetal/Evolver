package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.globalbestinitialization.GlobalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.globalbestinitialization.impl.DefaultGlobalBestInitialization;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different global best initialization strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how the initial global best solution is determined at the start of the PSO algorithm.
 * 
 * <p>The available global best initialization strategies are:
 * <ul>
 *   <li>defaultGlobalBestInitialization: Standard strategy that initializes the global best
 *       by selecting the best solution from the initial population</li>
 * </ul>
 * 
 * <p>Proper initialization of the global best is crucial as it influences the initial
 * direction of the swarm's movement in the search space.
 */
public class GlobalBestInitializationParameter extends CategoricalParameter {
  /**
   * Creates a new GlobalBestInitializationParameter with the specified valid values.
   * 
   * @param globalBestInitializationStrategies A list of valid global best initialization strategy names.
   *                                         Currently supports: "defaultGlobalBestInitialization"
   * @throws IllegalArgumentException if globalBestInitializationStrategies is null or empty
   */
  public GlobalBestInitializationParameter(List<String> globalBestInitializationStrategies) {
    super("globalBestInitialization", globalBestInitializationStrategies);
  }

  /**
   * Creates and returns a GlobalBestInitialization instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @return A configured GlobalBestInitialization implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known initialization strategy
   */
  public GlobalBestInitialization getGlobalBestInitialization() {
    GlobalBestInitialization result;

    if ("defaultGlobalBestInitialization".equals(value())) {
      result = new DefaultGlobalBestInitialization();
    } else {
      throw new JMetalException("Global best initialization component unknown: " + value());
    }

    return result;
  }

  /**
   * Returns the name of this parameter.
   * 
   * @return The string "globalBestInitialization"
   */
  @Override
  public String name() {
    return "globalBestInitialization";
  }
}
