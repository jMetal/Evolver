package org.uma.evolver.parameter;

import java.util.function.Predicate;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Represents a conditional sub-parameter associated with a main parameter in the parameter configuration system.
 *
 * <p>A {@code SpecificSubParameter} links a sub-parameter to a main parameter through a condition.
 * The sub-parameter becomes relevant or active only when the condition, defined as a {@link Predicate},
 * is satisfied by the value of the main parameter. This enables the modeling of dependencies and
 * conditional relationships between parameters, allowing for flexible and adaptive configuration.
 *
 * <p>Typical usage involves associating sub-parameters that should only be considered when the main
 * parameter takes a specific value or satisfies a certain predicate.
 *
 * @param <T> the type of the main parameter's value used in the condition
 */
public class ConditionalSubParameter<T> {
  private final Predicate<T> condition;
  private final Parameter<?> parameter;
  private final String description;

  /**
   * Constructs a SpecificSubParameter with the given condition, sub-parameter, and description.
   *
   * @param condition   the predicate that determines when the sub-parameter is active
   * @param parameter   the sub-parameter to be activated when the condition is met
   * @param description a textual description of the condition (for documentation or debugging)
   */
  public ConditionalSubParameter(Predicate<T> condition, Parameter<?> parameter, String description) {
    Check.notNull(condition);
    Check.notNull(parameter);

    this.condition = condition;
    this.parameter = parameter;
    this.description = description;
  }

  /**
   * Returns the predicate condition associated with this sub-parameter.
   *
   * @return the predicate condition
   */
  public Predicate<T> condition() {
    return condition;
  }

  /**
   * Returns the sub-parameter associated with this condition.
   *
   * @return the sub-parameter
   */
  public Parameter<?> parameter() {
    return parameter;
  }

  /**
   * Returns the description of this specific sub-parameter condition.
   *
   * @return the description
   */
  public String description() {
    return description;
  }

  @Override
  public String toString() {
    return "Condition: " + description + ", Parameter: " + parameter.name();
  }
}
