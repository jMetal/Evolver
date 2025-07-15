package org.uma.evolver.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Abstract class representing a generic parameter for evolutionary algorithms.
 *
 * <p>Each parameter has a name and a value, which is assigned by parsing an array containing
 * sequences of [key, value] pairs, for example:
 *
 * <pre>
 * ["--populationSize", "100", "--offspringPopulationSize", "100", "--createInitialSolutions", "random"]
 * </pre>
 *
 * <p>Every parameter has a {@link #name} (such as "populationSize" or "offspringPopulationSize")
 * and a {@link #value} that is obtained after invoking the {@link #parse(String[])} method.
 *
 * <p>Parameters can be seen as factories for any kind of objects, from single values (e.g., {@link
 * org.uma.evolver.parameter.type.DoubleParameter}) to genetic operators (e.g., {@link org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter}).
 *
 * <p>A parameter can contain other parameters, and there are three types of associations between
 * them. Using {@link org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter} as an example:
 *
 * <ul>
 *   <li><b>Global sub-parameter:</b> Any crossover has a probability parameter.
 *   <li><b>Specific sub-parameter:</b> An SBX crossover has a distribution index as a specific
 *       parameter.
 *   <li><b>Non-configurable sub-parameter:</b> Constant parameters needed by a particular
 *       parameter.
 * </ul>
 *
 * <p>The {@code Parameter} class provides methods for setting and getting these sub-parameters:
 *
 * <ul>
 *   <li>{@link #addGlobalSubParameter(Parameter)}
 *   <li>{@link #addSpecificSubParameter(String, Parameter)}
 *   <li>{@link #addNonConfigurableSubParameter(String, Object)}
 *   <li>{@link #globalSubParameters()}
 *   <li>{@link #specificSubParameters()}
 *   <li>{@link #findGlobalSubParameter(String)}
 *   <li>{@link #findSpecificSubParameter(String)}
 * </ul>
 *
 * @author Antonio J. Nebro
 * @param <T> Type of the parameter value
 */
/**
 * Represents a configurable parameter with a value of type {@code T}, supporting hierarchical
 * sub-parameters.
 *
 * <p>A {@code Parameter} can have:
 *
 * <ul>
 *   <li><b>Global sub-parameters</b>: Always relevant, regardless of this parameter's value.
 *   <li><b>Specific sub-parameters</b>: Only relevant when this parameter has a certain value or
 *       condition.
 *   <li><b>Non-configurable sub-parameters</b>: Internal or fixed configuration values.
 * </ul>
 *
 * <p>Subclasses must implement the {@link #parse(String[])} method to define how the parameter
 * value is parsed from arguments. This class provides fluent methods for adding sub-parameters and
 * utilities for parsing and retrieving them.
 *
 * @param <T> the type of the parameter value
 */
public abstract class Parameter<T> {
  private T value;
  private final String name;
  private final List<Parameter<?>> globalSubParameters = new ArrayList<>();
  private final Map<String, Object> nonConfigurableSubParameters = new HashMap<>();
  private ConditionalParameterManager<T> conditionalParameterManager;

  /**
   * Constructs a parameter with the given name.
   *
   * @param name the name of the parameter
   */
  protected Parameter(String name) {
    Check.notNull(name);
    Check.that(!name.isEmpty(), "Then parameter name must not be empty");
    this.name = name;
    conditionalParameterManager = new ConditionalParameterManager<>();
  }

  /**
   * Parses the value for this parameter from the given arguments. Subclasses must implement this
   * method to define their parsing logic.
   *
   * @param arguments the arguments from which to parse the parameter value
   */
  public abstract void parse(String[] arguments);

  /**
   * Parses the value for this parameter using the provided function and arguments, and also parses
   * all global and specific sub-parameters.
   *
   * @param parseFunction the function to convert a string to the parameter value type
   * @param args the argument array
   */
  public void parse(Function<String, T> parseFunction, String[] args) {
    value(on("--" + name(), args, parseFunction));
    parseGlobalSubParameters(args);
    conditionalParameterManager.parseConditionalParameters(value(), args);
  }

  /**
   * Returns the name of this parameter.
   *
   * @return the parameter name
   */
  public String name() {
    return name;
  }

  /**
   * Returns the current value of this parameter.
   *
   * @return the parameter value
   */
  public T value() {
    return value;
  }

  /**
   * Sets the value of this parameter.
   *
   * @param value the value to set
   */
  public void value(T value) {
    this.value = value;
  }

  /**
   * Returns the list of global sub-parameters associated with this parameter. Global sub-parameters
   * are always relevant, regardless of the parameter's value.
   *
   * @return a list of global sub-parameters
   */
  public List<Parameter<?>> globalSubParameters() {
    return globalSubParameters;
  }

  /**
   * Adds a global sub-parameter to this parameter.
   *
   * @param parameter the global sub-parameter to add
   */
  public Parameter<T> addGlobalSubParameter(Parameter<?> parameter) {
    globalSubParameters.add(parameter);

    return this ;
  }

  /**
   * Returns the list of specific sub-parameters associated with this parameter. Specific
   * sub-parameters are only relevant when the parameter has a certain value or condition.
   *
   * @return a list of specific sub-parameters
   */
  public List<ConditionalParameter<T>> conditionalParameters() {
    return conditionalParameterManager.conditionalParameters();
  }

  /**
   * Adds a specific sub-parameter that depends on a string value.
   *
   * @param dependsOn the value or condition that activates the sub-parameter
   * @param parameter the specific sub-parameter to add
   */
  public Parameter<T> addConditionalParameter(String dependsOn, Parameter<?> parameter) {
    conditionalParameterManager.addConditionalParameter(dependsOn, parameter);

    return this ;
  }

  /**
   * Adds a specific sub-parameter that depends on a boolean value.
   *
   * @param dependsOn the value or condition that activates the sub-parameter
   * @param parameter the specific sub-parameter to add
   */
  public Parameter<T> addConditionalParameter(Boolean dependsOn, Parameter<?> parameter) {
    conditionalParameterManager.addConditionalParameter(dependsOn, parameter);

    return this ;
  }


  /**
   * Adds a non-configurable sub-parameter to this parameter. These are typically used for internal
   * or fixed configuration values.
   *
   * @param parameterName the name of the sub-parameter
   * @param value the value of the sub-parameter
   */
  public Parameter<T> addNonConfigurableSubParameter(String parameterName, Object value) {
    nonConfigurableSubParameters.put(parameterName, value);

    return this ;
  }

  /**
   * Returns a map of non-configurable sub-parameters associated with this parameter.
   *
   * @return a map of non-configurable sub-parameters
   */
  public Map<String, Object> nonConfigurableSubParameters() {
    return nonConfigurableSubParameters;
  }

  /**
   * Finds a global sub-parameter given its name.
   *
   * @param parameterName the name of the global sub-parameter
   * @return the global sub-parameter, or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  public Parameter<?> findGlobalSubParameter(String parameterName) {
    return globalSubParameters().stream()
        .filter(parameter -> parameter.name().equals(parameterName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Finds a specific sub-parameter given its name.
   *
   * @param parameterName the name of the specific sub-parameter
   * @return the specific sub-parameter, or {@code null} if not found
   */
  public Parameter<?> findConditionalParameter(String parameterName) {
    return conditionalParameterManager.findConditionalParameter(parameterName);
  }

  /**
   * Finds a list of the specific sub-parameters associated with a particular parameter value.
   *
   * @param parameterValue the value that activates the specific sub-parameters
   * @return a list of specific sub-parameters for the given value
   */
  public List<Parameter<?>> findConditionalParameters(String parameterValue) {
    return conditionalParameters().stream()
        .filter(conditionalParameter -> conditionalParameter.description().equals(parameterValue))
        .map(parameter -> parameter.parameter())
        .collect(Collectors.toList());
  }

  /**
   * Returns a string representation of this parameter, including its value and sub-parameters.
   *
   * @return a string representation of the parameter
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Name: " + name() + ": " + "Value: " + value());
    if (!globalSubParameters.isEmpty()) {
      result.append("\n\t");
      for (Parameter<?> parameter : globalSubParameters) {
        result.append(" \n -> ").append(parameter.toString());
      }
    }
    if (!conditionalParameterManager.conditionalParameters().isEmpty()) {
      result.append("\n\t");
      for (ConditionalParameter<T> parameter :
          conditionalParameterManager.conditionalParameters()) {
        result.append(" \n -> ").append(parameter.toString());
      }
    }
    return result.toString();
  }

  // --- Private helper methods ---

  /**
   * Helper method to parse a value from the arguments using a key and a parser function.
   *
   * @param key the key to search for in the arguments
   * @param args the argument array
   * @param parser the function to convert the string value
   * @return the parsed value
   */
  private T on(String key, String[] args, Function<String, T> parser) {
    return parser.apply(retrieve(args, key));
  }

  /**
   * Helper method to retrieve the value associated with a key from the arguments array.
   *
   * @param args the argument array
   * @param key the key to search for
   * @return the value associated with the key
   * @throws IllegalArgumentException if the key is missing or has no value
   */
  private String retrieve(String[] args, String key) {
    int index = List.of(args).indexOf(key);
    Check.that(index != -1 && index != args.length - 1, "Missing parameter: " + key);
    return args[index + 1];
  }

  /**
   * Parses all global sub-parameters from the given arguments.
   *
   * @param args the argument array
   */
  private void parseGlobalSubParameters(String[] args) {
    globalSubParameters().forEach(parameter -> parameter.parse(args));
  }
}
