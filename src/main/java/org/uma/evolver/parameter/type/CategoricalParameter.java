package org.uma.evolver.parameter.type;

import java.util.List;
import java.util.function.Predicate;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ConditionalSubParameter;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter representing a categorical value selected from a predefined list of valid values.
 *
 * <p>This class supports:
 *
 * <ul>
 *   <li>Parsing from command-line-style arguments (e.g., {@code --paramName value})
 *   <li>Validation against an allowed list of string values
 *   <li>Formatted string representation including sub-parameters
 * </ul>
 *
 * <p>Validation is performed immediately during parsing, and an exception is thrown if the value is
 * not valid.
 *
 * <p>Example:
 *
 * <pre>{@code
 * CategoricalParameter colorParam = new CategoricalParameter("color", List.of("red", "green", "blue"));
 * colorParam.parse(new String[] {"--color", "green"}); // OK
 * colorParam.parse(new String[] {"--color", "yellow"}); // Throws exception
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class CategoricalParameter extends Parameter<String> {

  /** List of allowed string values for this parameter. */
  private final List<String> validValues;

  /** Predicate used to validate the parameter value. */
  private final Predicate<String> validator;

  /**
   * Constructs a new {@code CategoricalParameter}.
   *
   * @param name The name of the parameter (used as the command-line key)
   * @param validValues The list of valid string values for this parameter
   * @throws NullPointerException If {@code validValues} is null
   * @throws IllegalArgumentException If {@code validValues} is empty
   */
  public CategoricalParameter(String name, List<String> validValues) {
    super(name);
    Check.notNull(validValues);
    Check.that(!validValues.isEmpty(), "The list of valid values cannot be empty");
    Check.that(
        validValues.size() == validValues.stream().distinct().count(),
        "The list of valid values cannot contain duplicates: " + validValues);

    this.validValues = List.copyOf(validValues);
    this.validator = value -> value != null && validValues.contains(value);
  }

  /**
   * Parses the parameter value from a list of arguments and validates it.
   *
   * @param arguments An array of strings representing command-line arguments
   * @throws JMetalException If the value is not in the list of valid values
   */
  @Override
  public void parse(String[] arguments) {
    parse(s -> s, arguments);
    if (!validator.test(value())) {
      throw new JMetalException(
          "Parameter " + name() + ": Invalid value: " + value() + ". Valid values: " + validValues);
    }
  }

  /**
   * Returns the list of valid values for this parameter.
   *
   * @return An unmodifiable list of valid strings
   */
  public List<String> validValues() {
    return validValues;
  }

  /**
   * Returns a formatted string representation of this parameter including name, value, and valid
   * values. Also includes sub-parameters, if any.
   *
   * @return A string representation of the parameter
   */
  @Override
  public String toString() {
    StringBuilder result =
        new StringBuilder(
            "Name: " + name() + ". Value: " + value() + ". Valid values: " + validValues);
    for (Parameter<?> parameter : globalSubParameters()) {
      result.append("\n -> ").append(parameter.toString());
    }
    for (ConditionalSubParameter<String> parameter : conditionalSubParameters()) {
      result.append("\n -> ").append(parameter.parameter().toString());
    }
    return result.toString();
  }
}
