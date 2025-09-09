package org.uma.evolver.parameter.type;

import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A parameter representing a boolean value.
 *
 * <p>This class supports:
 * <ul>
 *   <li>Parsing from command-line-style arguments (e.g., {@code --paramName true})</li>
 *   <li>Validation to ensure the value is either {@code true} or {@code false} (case-insensitive)</li>
 *   <li>Formatted string representation including sub-parameters</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * BooleanParameter enabled = new BooleanParameter("enabled");
 * enabled.parse(new String[] {"--enabled", "true"});
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class BooleanParameter extends Parameter<Boolean> {

  /**
   * Constructs a new {@code BooleanParameter}.
   *
   * @param name The parameter name to be used when parsing arguments
   * @throws NullPointerException if {@code name} is null
   * @throws IllegalArgumentException if {@code name} is blank
   */
  public BooleanParameter(String name) {
    super(name);
    Check.notNull(name);
    Check.that(!name.isBlank(), "The parameter name cannot be empty");
  }

  /**
   * Parses the parameter value from a list of command-line-style arguments.
   *
   * @param args An array of strings representing the arguments
   */
  @Override
  public void parse(String[] args) {
    super.parse(Boolean::parseBoolean, args);
  }

  /**
   * Returns a formatted string representation of this parameter, including sub-parameters if any.
   *
   * @return A string describing the parameter and its sub-parameters
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Name: " + name()
            + ". Value: " + value());

    for (Parameter<?> parameter : globalSubParameters()) {
      result.append("\n -> ").append(parameter.toString());
    }
    for (ConditionalParameter<Boolean> parameter : conditionalParameters()) {
      result.append("\n -> ").append(parameter.parameter().toString());
    }

    return result.toString();
  }
}