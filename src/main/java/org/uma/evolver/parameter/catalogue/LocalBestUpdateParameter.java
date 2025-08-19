package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.localbestupdate.LocalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.localbestupdate.impl.DefaultLocalBestUpdate;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different local best update strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how a particle's personal best (pbest) is updated based on its current position.
 * 
 * <p>The available local best update strategies are:
 * <ul>
 *   <li>defaultLocalBestUpdate: Updates the particle's personal best if the current position dominates it,
 *       using the provided dominance comparator</li>
 * </ul>
 * 
 * <p>This component is crucial for maintaining the memory of each particle's best-found position,
 * which guides the particle's movement in subsequent iterations.
 */
public class LocalBestUpdateParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "localBestUpdate";
  
  /**
   * Creates a new LocalBestUpdateParameter with the specified valid values.
   * 
   * @param localBestUpdateStrategies A list of valid local best update strategy names.
   *                                 Currently supports: "defaultLocalBestUpdate"
   * @throws IllegalArgumentException if localBestUpdateStrategies is null or empty
   */
  public LocalBestUpdateParameter(List<String> localBestUpdateStrategies) {
    this(DEFAULT_NAME, localBestUpdateStrategies);
  }

  /**
   * Constructs a new LocalBestUpdateParameter instance with the given name and list of update strategies.
   * 
   * @param name The name of the parameter
   * @param localBestUpdateStrategies the list of valid local best update strategy names
   */
  public LocalBestUpdateParameter(String name, List<String> localBestUpdateStrategies) {
    super(name, localBestUpdateStrategies);
  }

  /**
   * Creates and returns a LocalBestUpdate instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @param comparator The dominance comparator used to determine if the current position
   *                  is better than the personal best. Must not be null.
   * @return A configured LocalBestUpdate implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known local best update strategy
   * @throws NullPointerException if the provided comparator is null
   */
  public LocalBestUpdate getLocalBestUpdate(DominanceComparator<DoubleSolution> comparator) {
    LocalBestUpdate result;

    if ("defaultLocalBestUpdate".equals(value())) {
      result = new DefaultLocalBestUpdate(comparator);
    } else {
      throw new JMetalException("Local best update component unknown: " + value());
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