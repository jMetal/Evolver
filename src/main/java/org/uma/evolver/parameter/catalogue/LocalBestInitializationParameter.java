package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.localbestinitialization.LocalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.localbestinitialization.impl.DefaultLocalBestInitialization;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different local best initialization strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how the local best initialization is computed during the PSO execution.
 * 
 * <p>The available local best initialization strategies are:
 * <ul>
 *   <li>defaultLocalBestInitialization: Uses the default local best initialization strategy</li>
 * </ul>
 */
public class LocalBestInitializationParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "localBestInitialization";
  
  /**
   * Creates a new LocalBestInitializationParameter with the specified valid values.
   * 
   * @param localBestInitializationStrategies A list of valid local best initialization strategy names. Typical values include:
   *                                          "defaultLocalBestInitialization"
   * @throws IllegalArgumentException if localBestInitializationStrategies is null or empty
   */
  public LocalBestInitializationParameter(List<String> localBestInitializationStrategies) {
    this(DEFAULT_NAME, localBestInitializationStrategies);
  }

  /**
   * Creates a new LocalBestInitializationParameter with the specified name and valid values.
   * 
   * @param name The name of the parameter
   * @param localBestInitializationStrategies A list of valid local best initialization strategy names. Typical values include:
   *                                          "defaultLocalBestInitialization"
   * @throws IllegalArgumentException if localBestInitializationStrategies is null or empty
   */
  public LocalBestInitializationParameter(String name, List<String> localBestInitializationStrategies) {
    super(name, localBestInitializationStrategies);
  }

  /** 
   * Creates and returns a LocalBestInitialization instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @return A configured LocalBestInitialization implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known local best initialization strategy
   */
  public LocalBestInitialization getLocalBestInitialization() {
    LocalBestInitialization result;

    if ("defaultLocalBestInitialization".equals(value())) {
      result = new DefaultLocalBestInitialization();
    } else {
      throw new JMetalException("Local best initialization component unknown: " + value());
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
