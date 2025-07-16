package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.globalbestupdate.GlobalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.globalbestupdate.impl.DefaultGlobalBestUpdate;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different global best update strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how the global best solution is updated during the PSO execution.
 * 
 * <p>The available global best update strategies are:
 * <ul>
 *   <li>defaultGlobalBestUpdate: Standard strategy that updates the global best if a better solution is found</li>
 * </ul>
 * 
 * <p>The global best represents the best solution found by any particle in the swarm so far,
 * which guides the movement of all particles in subsequent iterations.
 */
public class GlobalBestUpdateParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "globalBestUpdate";
  
  /**
   * Creates a new GlobalBestUpdateParameter with the specified valid values.
   * 
   * @param updateStrategies A list of valid global best update strategy names.
   *                        Currently supports: "defaultGlobalBestUpdate"
   * @throws IllegalArgumentException if updateStrategies is null or empty
   */
  /**
   * Constructs a new GlobalBestUpdateParameter instance with the given list of update strategies.
   * 
   * @param updateStrategies the list of valid global best update strategy names
   */
  public GlobalBestUpdateParameter(List<String> updateStrategies) {
    this(DEFAULT_NAME, updateStrategies);
  }

  /**
   * Constructs a new GlobalBestUpdateParameter instance with the given name and list of update strategies.
   * 
   * @param name The name of the parameter
   * @param updateStrategies the list of valid global best update strategy names
   */
  public GlobalBestUpdateParameter(String name, List<String> updateStrategies) {
    super(name, updateStrategies);
  }

  /**
   * Creates and returns a GlobalBestUpdate instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @return A configured GlobalBestUpdate implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known global best update strategy
   */
  public GlobalBestUpdate getGlobalBestUpdate() {
    GlobalBestUpdate result;
    if ("defaultGlobalBestUpdate".equals(value())) {
      result = new DefaultGlobalBestUpdate();
    } else {
      throw new JMetalException("Global Best Update component unknown: " + value());
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