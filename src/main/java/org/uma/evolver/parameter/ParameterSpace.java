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
public class ParameterSpace {
  protected final Map<String, Parameter<?>> parameterSpace;
  protected final List<Parameter<?>> topLevelParameters;

  /**
   * Constructs a new ParameterSpace and initializes its parameters, relationships, and top-level parameters.
   * Subclasses must implement the abstract methods to define the specific configuration.
   */
  public ParameterSpace() {
    parameterSpace = new LinkedHashMap<>();
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

  /**
   * Adds a parameter to the list of top-level parameters.
   * @param parameter
   */
  public void addTopLevelParameter(Parameter<?> parameter) {
    topLevelParameters.add(parameter);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Parameter<?> topLevel : topLevelParameters) {
      printParameterHierarchy(topLevel, sb, 0);
    }
    return sb.toString();
  }

  private void printParameterHierarchy(Parameter<?> parameter, StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append("  ");
    }
    sb.append("- ").append(parameter.name());
    sb.append(" [value=").append(parameter.value()).append("]");
    sb.append(System.lineSeparator());

    // Imprimir subparámetros globales
    List<Parameter<?>> globalSubParameters = parameter.globalSubParameters();
    if (globalSubParameters != null && !globalSubParameters.isEmpty()) {
      for (Parameter<?> global : globalSubParameters) {
        printParameterHierarchy(global, sb, indent + 1);
      }
    }

    // Imprimir subparámetros condicionales
    List<? extends Object> conditionalParameters = parameter.conditionalParameters();
    if (conditionalParameters != null && !conditionalParameters.isEmpty()) {
      for (Object condObj : conditionalParameters) {
        // Cada elemento es un ConditionalParameter<?>
        try {
          // Usamos reflexión para acceder a los métodos description() y parameter()
          java.lang.reflect.Method descMethod = condObj.getClass().getMethod("description");
          java.lang.reflect.Method paramMethod = condObj.getClass().getMethod("parameter");
          Object desc = descMethod.invoke(condObj);
          Object param = paramMethod.invoke(condObj);

          for (int i = 0; i < indent + 1; i++) {
            sb.append("  ");
          }
          sb.append("> if value = ").append(desc).append(":").append(System.lineSeparator());
          if (param instanceof Parameter) {
            printParameterHierarchy((Parameter<?>) param, sb, indent + 2);
          }
        } catch (Exception e) {
          // Si falla la reflexión, ignoramos ese subparámetro condicional
        }
      }
    }
  }
}
