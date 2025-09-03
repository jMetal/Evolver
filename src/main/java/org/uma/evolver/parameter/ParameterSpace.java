package org.uma.evolver.parameter;

import java.util.*;

/**
 * Abstract class that defines a configurable parameter space for metaheuristics.
 * <p>
 * This class provides a framework for managing algorithm parameters in a hierarchical
 * structure. It maintains two main collections:
 * <ul>
 *   <li>A map of all parameters for direct access by name ({@code parameterSpace})</li>
 *   <li>An ordered list of top-level parameters that serve as entry points for configuration
 *   ({@code topLevelParameters})</li>
 * </ul>
 * Note that all top-level parameters are also included in the main parameter space map.
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li><b>Hierarchical Parameter Management</b>: Supports complex parameter hierarchies with
 *   conditional and global parameters</li>
 *   <li><b>Type Safety</b>: Uses generics to ensure type safety for all parameter values</li>
 *   <li><b>Immutable Views</b>: Provides unmodifiable views of parameters and top-level parameter lists</li>
 *   <li><b>Flexible Configuration</b>: Allows dynamic addition and retrieval of parameters</li>
 * </ul>
 * </p>
 *
 * <p>
 * Usage example:
 * <pre>
 * // Create a custom parameter space
 * public class MyParameterSpace extends ParameterSpace {
 *     public MyParameterSpace() {
 *         // Add parameters
 *         put(new IntegerParameter("populationSize", 50, 200, 100));
 *         put(new DoubleParameter("mutationRate", 0.0, 1.0, 0.1));
 *         
 *         // Mark as top-level parameters
 *         addTopLevelParameter(get("populationSize"));
 *         addTopLevelParameter(get("mutationRate"));
 *     }
 *     
 *     {@literal @}Override
 *     public ParameterSpace createInstance() {
 *         return new MyParameterSpace();
 *     }
 * }
 * 
 * // Usage
 * ParameterSpace space = new MyParameterSpace();
 * Parameter<?> popSize = space.get("populationSize");
 * List<Parameter<?>> topParams = space.topLevelParameters();
 * </pre>
 * </p>
 * 
 * @see Parameter
 * @see IntegerParameter
 * @see DoubleParameter
 * @see CategoricalParameter
 */
public abstract class ParameterSpace {
  protected final Map<String, Parameter<?>> parameterSpace;
  protected final List<Parameter<?>> topLevelParameters;

  /**
   * Constructs a new, empty ParameterSpace.
   * <p>
   * The constructor initializes the internal data structures for storing parameters.
   * Subclasses should typically add parameters and set up their relationships in
   * their constructors after calling {@code super()}.
   * </p>
   * <p>
   * Example:
   * <pre>
   * public class MyParameterSpace extends ParameterSpace {
   *     public MyParameterSpace() {
   *         super();  // Initializes parameter storage
   *         // Add parameters here
   *     }
   * }
   * </pre>
   * </p>
   */
  public ParameterSpace() {
    parameterSpace = new HashMap<>();
    topLevelParameters = new ArrayList<>();
  }

  /**
   * Adds a parameter to the parameter space.
   * <p>
   * The parameter's name must be unique within this parameter space. If a parameter
   * with the same name already exists, it will be replaced.
   * </p>
   *
   * @param parameter the parameter to add (must not be null)
   * @throws NullPointerException if the parameter is null
   * @see #get(String)
   * @see #parameters()
   */
  public void put(Parameter<?> parameter) {
    parameterSpace.put(parameter.name(), parameter);
  }

  /**
   * Retrieves a parameter by its name.
   *
   * @param parameterName the name of the parameter to retrieve (case-sensitive)
   * @return the corresponding parameter (never null)
   * @throws IllegalArgumentException if no parameter with the given name exists
   * @throws NullPointerException if parameterName is null
   * @see #put(Parameter)
   * @see #parameters()
   */
  public Parameter<?> get(String parameterName) {
    Parameter<?> parameter = parameterSpace.get(parameterName);
    if (parameter == null) {
      throw new IllegalArgumentException("Parameter not found: " + parameterName);
    }
    return parameter;
  }

  /**
   * Returns an unmodifiable view of all parameters in this parameter space.
   * <p>
   * The returned map includes all parameters, both top-level and nested, keyed by parameter name.
   * The map is unmodifiable; to add parameters, use {@link #put(Parameter)}.
   * </p>
   *
   * @return an unmodifiable map of parameter names to their corresponding Parameter objects
   * @see #topLevelParameters()
   * @see #get(String)
   */
  public Map<String, Parameter<?>> parameters() {
    return Collections.unmodifiableMap(parameterSpace);
  }

  /**
   * Returns an unmodifiable list of top-level parameters in this parameter space.
   * <p>
   * Top-level parameters are the main entry points for configuring the algorithm.
   * These parameters are also included in the map returned by {@link #parameters()}.
   * The order of parameters in the list matches the order in which they were added.
   * </p>
   *
   * @return an unmodifiable list of top-level parameters (never null)
   * @see #addTopLevelParameter(Parameter)
   * @see #parameters()
   */
  public List<Parameter<?>> topLevelParameters() {
    return Collections.unmodifiableList(topLevelParameters);
  }

  /**
   * Adds a parameter to the list of top-level parameters.
   * <p>
   * Top-level parameters serve as the main entry points for algorithm configuration.
   * The parameter must have already been added to this parameter space using
   * {@link #put(Parameter)} before calling this method.
   * </p>
   *
   * @param parameter the parameter to add as a top-level parameter (must not be null)
   * @throws IllegalArgumentException if the parameter is not in this parameter space
   * @throws NullPointerException if the parameter is null
   * @see #topLevelParameters()
   * @see #put(Parameter)
   */
  public void addTopLevelParameter(Parameter<?> parameter) {
    topLevelParameters.add(parameter);
  }

  /**
   * Creates and configures a new instance of this parameter space.
   * <p>
   * This method is part of the prototype pattern, allowing parameter spaces to be
   * cloned with the same configuration. Implementations should return a new instance
   * of the concrete parameter space class with the same parameter configuration.
   * </p>
   * <p>
   * Example implementation:
   * <pre>
   * {@literal @}Override
   * public ParameterSpace createInstance() {
   *     return new MyParameterSpace();
   * }
   * </pre>
   * </p>
   *
   * @return a new instance of this parameter space
   */
  public abstract ParameterSpace createInstance();
}
