package org.uma.evolver.parameter;

import java.util.*;
/**
 * Abstract class that defines a configurable parameter space for metaheuristics.
 * <p>
 * This class provides a framework for managing algorithm parameters in a hierarchical
 * structure. It maintains a collection of parameters that define the behavior of
 * metaheuristic algorithms, allowing for configuration through parameter spaces.
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li>Stores parameters in a map for easy access by name</li>
 *   <li>Maintains a list of top-level parameters for configuration entry points</li>
 *   <li>Provides an abstract createInstance() method for parameter space initialization</li>
 * </ul>
 * </p>
 *
 * <p>
 * Usage example:
 * <pre>
 * ParameterSpace parameterSpace = new MyAlgorithmParameterSpace();
 * Parameter<?> populationSize = parameterSpace.get("populationSize");
 * List<Parameter<?>> mainParameters = parameterSpace.topLevelParameters();
 * </pre>
 * </p>
 */
public abstract class ParameterSpace {
  protected final Map<String, Parameter<?>> parameterSpace;
  protected final List<Parameter<?>> topLevelParameters;

  /**
   * Constructs a new ParameterSpace and initializes its parameters, relationships,
   * and top-level parameters. Subclasses must implement the abstract methods to
   * define the specific parameter configuration.
   */
  public ParameterSpace() {
    parameterSpace = new HashMap<>();
    topLevelParameters = new ArrayList<>();
  }

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
   * @return the corresponding parameter, or throws IllegalArgumentException if not found
   */
  public Parameter<?> get(String parameterName) {
    Parameter<?> parameter = parameterSpace.get(parameterName);
    if (parameter == null) {
      throw new IllegalArgumentException("Parameter not found: " + parameterName);
    }
    return parameter;
  }

  /**
   * Returns the complete map of parameters managed by this parameter space.
   *
   * @return an unmodifiable map of parameter names to their corresponding Parameter objects
   */
  public Map<String, Parameter<?>> parameters() {
    return Collections.unmodifiableMap(parameterSpace);
  }

  /**
   * Returns the list of top-level parameters in this parameter space.
   * These are typically the main parameters for configuring the algorithm.
   *
   * @return an unmodifiable list of top-level parameters
   */
  public List<Parameter<?>> topLevelParameters() {
    return Collections.unmodifiableList(topLevelParameters);
  }

  /**
   * Adds a parameter to the list of top-level parameters.
   * These parameters are the main entry points for configuring the algorithm.
   *
   * @param parameter the parameter to add as a top-level parameter
   */
  public void addTopLevelParameter(Parameter<?> parameter) {
    topLevelParameters.add(parameter);
  }

  /**
   * Abstract method that must be implemented by subclasses to create and
   * configure the parameter space. This method should set up all parameters
   * and their relationships for the specific algorithm configuration.
   */
  public abstract ParameterSpace createInstance();
}
