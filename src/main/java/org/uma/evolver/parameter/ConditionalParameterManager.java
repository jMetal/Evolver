package org.uma.evolver.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages a collection of {@link ConditionalParameter} objects associated with a main parameter in
 * a multi-objective metaheuristic parameter space.
 *
 * <p>This class serves as a registry and coordinator for conditional parameters, enabling flexible
 * and adaptive parameter configurations where certain parameters are only relevant when a parent
 * parameter satisfies specific conditions. It supports both custom predicate-based conditions and
 * common value-based equality conditions.
 *
 * <p>The manager handles the dynamic activation of parameters during parameter space processing,
 * automatically parsing arguments only for parameters whose conditions are satisfied by the current
 * value of the main parameter.
 *
 * <p><strong>Example usage scenario:</strong>
 *
 * <pre>{@code
 * // For a categorical parameter "algorithmVariant" with values "basic" and "advanced"
 * ConditionalParameterManager<String> manager = new ConditionalParameterManager<>();
 *
 * // Add parameters that only apply to "advanced" variant
 * manager.addConditionalParameter("advanced", archiveSize);
 * manager.addConditionalParameter("advanced", selectionPressure);
 *
 * // During processing, only parse arguments for active parameters
 * manager.parseConditionalParameters("advanced", args);
 * }</pre>
 *
 * <p>This class is particularly useful for processing YAML configuration files where parameters
 * have nested conditional structures, allowing for dynamic parameter space exploration based on
 * parent parameter values.
 *
 * @param <T> the type of the main parameter's value used in the conditions
 * @see ConditionalParameter
 * @see Parameter
 * @see Predicate
 * @author [Author Name]
 * @version 1.0
 * @since 1.0
 */
public class ConditionalParameterManager<T> {

  /**
   * Internal storage for all managed conditional parameters. Each entry represents a parameter that
   * becomes active when its associated condition is met.
   */
  private final List<ConditionalParameter<T>> conditionalParameters = new ArrayList<>();

  /**
   * Adds a conditional parameter with a custom predicate condition.
   *
   * <p>This method provides maximum flexibility by allowing any predicate-based condition to
   * determine when the parameter becomes active. The condition will be evaluated against the main
   * parameter's value during parameter space processing.
   *
   * @param condition the predicate that determines when the parameter is active; must not be null
   * @param parameter the parameter to be activated when the condition is met; must not be null
   * @param description a textual description of the condition for documentation, debugging, and
   *     logging purposes; must not be null
   * @throws IllegalArgumentException if any parameter is null (via ConditionalParameter
   *     constructor)
   */
  public void addConditionalParameter(
      Predicate<T> condition, Parameter<?> parameter, String description) {
    conditionalParameters.add(new ConditionalParameter<>(condition, parameter, description));
  }

  /**
   * Adds a conditional parameter that becomes active when the main parameter equals the specified
   * string value.
   *
   * <p>This is a convenience method for the common case where a parameter should only be active
   * when the main parameter has a specific string value. The condition uses {@link
   * Object#equals(Object)} for comparison.
   *
   * <p><strong>Note:</strong> This method assumes the main parameter type {@code T} can be
   * meaningfully compared with a String. If {@code T} is not String-compatible, this may result in
   * the condition never being satisfied.
   *
   * @param value the string value that activates the parameter; must not be null
   * @param parameter the parameter to be activated; must not be null
   */
  public void addConditionalParameter(String value, Parameter<?> parameter) {
    conditionalParameters.add(
        new ConditionalParameter<>(v -> v.equals(value), parameter, value));
  }

  /**
   * Adds a conditional parameter that becomes active when the main parameter equals the specified
   * integer value.
   *
   * <p>This is a convenience method for the common case where a parameter should only be active
   * when the main parameter has a specific integer value. The condition uses {@link
   * Object#equals(Object)} for comparison.
   *
   * <p><strong>Note:</strong> This method assumes the main parameter type {@code T} can be
   * meaningfully compared with an Integer. If {@code T} is not Integer-compatible, this may result
   * in the condition never being satisfied.
   *
   * @param value the integer value that activates the parameter
   * @param parameter the parameter to be activated; must not be null
   */
  public void addConditionalParameter(int value, Parameter<?> parameter) {
    conditionalParameters.add(
        new ConditionalParameter<>(v -> v.equals(value), parameter, "" + value));
  }

  /**
   * Adds a conditional parameter that becomes active when the main parameter equals the specified
   * boolean value.
   *
   * <p>This is a convenience method for the common case where a parameter should only be active
   * when the main parameter has a specific boolean value. The condition uses {@link
   * Object#equals(Object)} for comparison.
   *
   * <p><strong>Note:</strong> This method assumes the main parameter type {@code T} can be
   * meaningfully compared with a Boolean. If {@code T} is not Boolean-compatible, this may result
   * in the condition never being satisfied.
   *
   * @param value the boolean value that activates the parameter
   * @param parameter the parameter to be activated; must not be null
   */
  public void addConditionalParameter(boolean value, Parameter<?> parameter) {
    conditionalParameters.add(
        new ConditionalParameter<>(v -> v.equals(value), parameter, "" + value));
  }

  /**
   * Parses arguments for all conditional parameters whose conditions are satisfied by the given
   * main parameter value.
   *
   * <p>This method iterates through all managed conditional parameters, evaluates their conditions
   * against the provided value, and calls {@link Parameter#parse(String[])} only on those
   * parameters whose conditions return {@code true}.
   *
   * <p>This enables dynamic parameter processing where only relevant parameters are considered
   * based on the current configuration state.
   *
   * @param value the current value of the main parameter to test conditions against
   * @param args the command-line arguments or configuration values to parse for the active
   *     parameters
   */
  public void parseConditionalParameters(T value, String[] args) {
    conditionalParameters.forEach(
        conditionalParameter -> {
          Predicate<T> condition = conditionalParameter.condition();
          if (condition.test(value)) {
            conditionalParameter.parameter().parse(args);
          }
        });
  }

  /**
   * Finds a conditional parameter by its name.
   *
   * <p>This method searches through all managed conditional parameters and returns the first
   * parameter whose name matches the specified parameter name.
   *
   * <p><strong>Return behavior:</strong> This method returns {@code null} if no parameter with the
   * given name is found. Callers should check for null return values before using the result.
   *
   * @param parameterName the name of the parameter to find; must not be null
   * @return the {@link Parameter} with the given name, or {@code null} if no parameter with the
   *     specified name is found
   */
  public Parameter<?> findConditionalParameter(String parameterName) {
    return conditionalParameters().stream()
        .filter(parameter -> parameter.parameter().name().equals(parameterName))
        .findFirst()
        .map(ConditionalParameter::parameter)
        .orElse(null);
  }

  /**
   * Returns an unmodifiable view of all managed conditional parameters.
   *
   * <p>The returned list contains all {@link ConditionalParameter} objects that have been
   * registered with this manager. This method is useful for introspection, validation, and
   * debugging purposes.
   *
   * <p>The returned list reflects the current state of the manager and will include any parameters
   * added after this method is called, but modifications to the returned list will not affect the
   * manager's internal state.
   *
   * @return the list of all managed {@link ConditionalParameter} objects; never null, but may be
   *     empty
   */
  public List<ConditionalParameter<T>> conditionalParameters() {
    return conditionalParameters;
  }
}
