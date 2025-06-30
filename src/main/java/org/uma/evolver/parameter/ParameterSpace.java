package org.uma.evolver.parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that defines a configurable parameter space for metaheuristics.
 * <p>
 * A {@code ParameterSpace} manages a collection of {@link Parameter} instances that define
 * the behavior and configuration of an algorithm. It provides mechanisms to store, retrieve,
 * and organize parameters, as well as to establish relationships between them.
 * </p>
 *
 * <p>
 * Each parameter space maintains:
 * <ul>
 *   <li>A map of all parameters, accessible by name.</li>
 *   <li>A list of top-level parameters, which are the main entry points for configuration.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Subclasses must implement the abstract methods to define the available parameters,
 * their relationships, and which parameters are considered top-level.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * ParameterSpace parameterSpace = new MyAlgorithmParameterSpace();
 * Parameter<?> populationSize = parameterSpace.get("populationSize");
 * List<Parameter<?>> mainParameters = parameterSpace.getTopLevelParameters();
 * </pre>
 * </p>
 */
public abstract class ParameterSpace {
  protected final Map<String, Parameter<?>> parameterSpace;
  protected final List<Parameter<?>> topLevelParameters;

  /**
   * Constructs a new ParameterSpace and initializes its parameters, relationships, and top-level parameters.
   * Subclasses must implement the abstract methods to define the specific configuration.
   */
  protected ParameterSpace() {
    parameterSpace = new LinkedHashMap<>();
    topLevelParameters = new ArrayList<>();

    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }

  /**
   * Defines and adds all parameters to the parameter space.
   * Subclasses must implement this method to specify which parameters are available.
   */
  protected abstract void setParameterSpace();

  /**
   * Establishes relationships (such as dependencies or hierarchies) between parameters.
   * Subclasses must implement this method to specify how parameters are related.
   */
  protected abstract void setParameterRelationships();

  /**
   * Identifies and adds the top-level parameters to the list.
   * Top-level parameters are the main entry points for configuration.
   * Subclasses must implement this method to specify which parameters are top-level.
   */
  protected abstract void setTopLevelParameters();

  /**
   * Adds a parameter to the parameter space.
   *
   * @param parameter the parameter to add
   */
  public void put(Parameter<?> parameter) {
    parameterSpace.put(parameter.name(), parameter);
  }

  /**
   * Retrieves a parameter by its name.
   *
   * @param parameterName the name of the parameter
   * @return the corresponding parameter, or {@code null} if not found
   */
  public Parameter<?> get(String parameterName) {
    Parameter<?> parameter = parameterSpace.get(parameterName);
    if (parameter == null) {
      throw new IllegalArgumentException("Parameter not found: " + parameterName);
    }
    return parameterSpace.get(parameterName);
  }

  /**
   * Returns the complete map of parameters managed by this parameter space.
   *
   * @return a map of parameter names to their corresponding Parameter objects
   */
  public Map<String, Parameter<?>> parameters() {
    return parameterSpace;
  }

  /**
   * Returns the list of top-level parameters in this parameter space.
   * These are typically the main parameters for configuring the algorithm.
   *
   * @return a list of top-level parameters
   */
  public List<Parameter<?>> topLevelParameters() {
    return topLevelParameters;
  }
}
