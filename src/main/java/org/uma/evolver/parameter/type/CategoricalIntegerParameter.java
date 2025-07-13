package org.uma.evolver.parameter.type;

import java.util.List;
import java.util.function.Predicate;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter representing a categorical value selected from a predefined list of valid integers.
 *
 * <p>This class supports:
 * <ul>
 *   <li>Parsing from command-line-style arguments (e.g., {@code --paramName 3})</li>
 *   <li>Validation against an allowed list of integer values</li>
 *   <li>Formatted string representation including sub-parameters</li>
 * </ul>
 *
 * <p>Validation is performed immediately during parsing, and an exception is thrown if the value is not valid.
 *
 * <p>Example:
 * <pre>{@code
 * CategoricalIntegerParameter sizeParam = new CategoricalIntegerParameter("size", List.of(1, 2, 3));
 * sizeParam.parse(new String[] {"--size", "2"}); // OK
 * sizeParam.parse(new String[] {"--size", "5"}); // Throws exception
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class CategoricalIntegerParameter extends Parameter<Integer> {

  /** List of allowed integer values for this parameter. */
  private final List<Integer> validValues;

  /** Predicate used to validate the parameter value. */
  private final Predicate<Integer> validator;

  /**
   * Constructs a new {@code CategoricalIntegerParameter}.
   *
   * @param name        The name of the parameter (used as the command-line key)
   * @param validValues The list of valid integer values for this parameter
   * @throws NullPointerException     If {@code validValues} is null
   * @throws IllegalArgumentException If {@code validValues} is empty or contains duplicates
   */
  public CategoricalIntegerParameter(String name, List<Integer> validValues) {
    super(name);
    Check.notNull(validValues);
    Check.that(!validValues.isEmpty(), "The list of valid values cannot be empty");
    Check.that(
            validValues.size() == validValues.stream().distinct().count(),
            "The list of valid values cannot contain duplicates: " + validValues
    );

    this.validValues = List.copyOf(validValues);
    this.validator = value -> value != null && validValues.contains(value);
  }

  /**
   * Parses the parameter value from a list of arguments and validates it.
   *
   * @param arguments An array of strings representing command-line arguments
   * @throws JMetalException If the value is not in the list of valid values
   * @throws NumberFormatException If the value is not a valid integer
   */
  @Override
  public void parse(String[] arguments) {
    parse(Integer::parseInt, arguments);
    if (!validator.test(value())) {
      throw new JMetalException(
              "Parameter "
                      + name()
                      + ": Invalid value: "
                      + value()
                      + ". Valid values: "
                      + validValues);
    }
  }

    /**
   * Returns the list of valid values for this parameter.
   *
   * @return An unmodifiable list of valid integers
   */
  public List<Integer> validValues() {
    return validValues;
  }

  /**
   * Returns a formatted string representation of this parameter including name, value, and valid values.
   * Also includes sub-parameters, if any.
   *
   * @return A string representation of the parameter
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Name: " + name()
            + ". Value: " + value()
            + ". Valid values: " + validValues);
    for (Parameter<?> parameter : globalSubParameters()) {
      result.append("\n -> ").append(parameter.toString());
    }
    for (ConditionalParameter<?> parameter : conditionalParameters()) {
      result.append("\n -> ").append(parameter.parameter().toString());
    }
    return result.toString();
  }
}