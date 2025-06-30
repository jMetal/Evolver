package org.uma.evolver.parameter;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Manages a collection of {@link SpecificSubParameter} objects associated with a main parameter.
 *
 * <p>This class allows the registration and management of conditional sub-parameters, enabling
 * flexible and adaptive parameter configurations. Each sub-parameter is associated with a condition
 * (predicate) that determines when it is relevant or active, based on the value of the main parameter.
 *
 * <p>Typical usage includes adding sub-parameters that should only be considered when the main
 * parameter takes a specific value or satisfies a certain predicate, parsing arguments for active
 * sub-parameters, and searching for sub-parameters by name.
 *
 * @param <T> the type of the main parameter's value used in the conditions
 */
public class SpecificSubParameterManager<T> {
  private final List<SpecificSubParameter<T>> specificSubParameters = new ArrayList<>();

  /**
   * Adds a specific sub-parameter with a custom condition and description.
   *
   * @param condition   the predicate that determines when the sub-parameter is active
   * @param parameter   the sub-parameter to be activated when the condition is met
   * @param description a textual description of the condition (for documentation or debugging)
   */
  public void addSpecificSubParameter(
      Predicate<T> condition, Parameter<?> parameter, String description) {
    specificSubParameters.add(new SpecificSubParameter<>(condition, parameter, description));
  }

  /**
   * Adds a specific sub-parameter that is active when the main parameter equals the given string value.
   *
   * @param value     the value that activates the sub-parameter
   * @param parameter the sub-parameter to be activated
   */
  public void addSpecificSubParameter(String value, Parameter<?> parameter) {
    specificSubParameters.add(
        new SpecificSubParameter<>(v -> v.equals(value), parameter, value));
  }

  /**
   * Adds a specific sub-parameter that is active when the main parameter equals the given integer value.
   *
   * @param value     the value that activates the sub-parameter
   * @param parameter the sub-parameter to be activated
   */
  public void addSpecificSubParameter(int value, Parameter<?> parameter) {
    specificSubParameters.add(
        new SpecificSubParameter<>(v -> v.equals(value), parameter, "" + value));
  }

  /**
   * Adds a specific sub-parameter that is active when the main parameter equals the given boolean value.
   *
   * @param value     the value that activates the sub-parameter
   * @param parameter the sub-parameter to be activated
   */
  public void addSpecificSubParameter(boolean value, Parameter<?> parameter) {
    specificSubParameters.add(
            new SpecificSubParameter<>(v -> v.equals(value), parameter, "" + value));
  }

  /**
   * Parses the arguments for all sub-parameters whose condition is satisfied by the given value.
   *
   * @param value the value of the main parameter to test the conditions
   * @param args  the arguments to parse for the active sub-parameters
   */
  public void parseSpecificSubParameters(T value, String[] args) {
    specificSubParameters.forEach(
        specificSubParameter -> {
          Predicate<T> condition = specificSubParameter.condition();
          if (condition.test(value)) {
            specificSubParameter.parameter().parse(args);
          }
        });
  }

  /**
   * Finds a specific sub-parameter by its name.
   *
   * @param parameterName the name of the sub-parameter to find
   * @return the {@link Parameter} with the given name, or throws NullPointerException if not found
   */
  public Parameter<?> findSpecificSubParameter(String parameterName) {
    return Objects.requireNonNull(
            specificSubParameters().stream()
                .filter(parameter -> parameter.parameter().name().equals(parameterName))
                .findFirst()
                .orElse(null))
        .parameter();
  }

  /**
   * Returns the list of all managed specific sub-parameters.
   *
   * @return the list of {@link SpecificSubParameter} objects
   */
  public List<SpecificSubParameter<T>> specificSubParameters() {
    return specificSubParameters;
  }
}
