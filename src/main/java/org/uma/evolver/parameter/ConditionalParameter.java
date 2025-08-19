package org.uma.evolver.parameter;

import java.util.function.Predicate;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Represents a parameter that becomes active only when a specified condition is satisfied by the
 * value of another parameter in a multi-objective metaheuristic parameter space.
 *
 * <p>This class is designed to model conditional parameter relationships commonly found in
 * metaheuristic algorithm configurations, where certain parameters are only relevant when a parent
 * parameter takes specific values. For example, archive-related parameters might only be applicable
 * when the algorithm variant is set to use an external archive.
 *
 * <p>The conditional relationship is defined through a {@link Predicate} that evaluates the parent
 * parameter's value. When the predicate returns {@code true}, the associated parameter becomes
 * active and should be considered during parameter space exploration or algorithm configuration.
 *
 * <p><strong>Example usage in YAML configuration:</strong>
 *
 * <pre>{@code
 * algorithmResult:
 *   type: categorical
 *   values:
 *     population: {}
 *     externalArchive:
 *       conditionalParameters:
 *         populationSizeWithArchive:
 *           type: integer
 *           range: [10, 200]
 * }</pre>
 *
 * <p>In this example, {@code populationSizeWithArchive} is a conditional parameter that only
 * becomes active when {@code algorithmResult} equals "externalArchive".
 *
 * <p>This class is immutable and thread-safe once constructed.
 *
 * @param <T> the type of the parent parameter's value that will be evaluated by the condition
 * @see Parameter
 * @see Predicate
 * @author [Author Name]
 * @version 1.0
 * @since 1.0
 */
public class ConditionalParameter<T> {

  /**
   * The predicate condition that determines when this parameter becomes active. This predicate is
   * evaluated against the parent parameter's value.
   */
  private final Predicate<T> condition;

  /**
   * The parameter that becomes active when the condition is satisfied. Uses wildcard type as the
   * parameter's specific type may differ from the condition type.
   */
  private final Parameter<?> parameter;

  /**
   * A human-readable description of the condition for documentation, debugging, and error reporting
   * purposes.
   */
  private final String description;

  /**
   * Constructs a new ConditionalParameter with the specified condition, parameter, and description.
   *
   * <p>All parameters are required and must not be null. The condition will be used to determine
   * when the associated parameter should be active during parameter space processing or algorithm
   * configuration.
   *
   * @param condition the predicate that determines when the parameter is active; must not be null
   * @param parameter the parameter to be activated when the condition is met; must not be null
   * @param description a textual description of the condition for documentation and debugging
   *     purposes; must not be null
   * @throws IllegalArgumentException if any parameter is null
   */
  public ConditionalParameter(Predicate<T> condition, Parameter<?> parameter, String description) {
    Check.notNull(condition);
    Check.notNull(parameter);
    Check.notNull(description);

    this.condition = condition;
    this.parameter = parameter;
    this.description = description;
  }

  /**
   * Returns the predicate condition associated with this conditional parameter.
   *
   * <p>The returned predicate can be used to test whether a parent parameter's value satisfies the
   * condition for activating this parameter.
   *
   * @return the predicate condition; never null
   */
  public Predicate<T> condition() {
    return condition;
  }

  /**
   * Returns the parameter that becomes active when the condition is satisfied.
   *
   * <p>This parameter should only be considered during parameter space exploration or algorithm
   * configuration when the associated condition evaluates to true.
   *
   * @return the conditional parameter; never null
   */
  public Parameter<?> parameter() {
    return parameter;
  }

  /**
   * Returns the human-readable description of this conditional parameter's condition.
   *
   * <p>The description provides context about when this parameter becomes active and is useful for
   * documentation, debugging, and user interfaces.
   *
   * @return the condition description; never null
   */
  public String description() {
    return description;
  }

  /**
   * Returns a string representation of this conditional parameter.
   *
   * <p>The string includes both the condition description and the parameter name for easy
   * identification during debugging and logging.
   *
   * @return a string representation in the format "Condition: [description], Parameter: [parameter
   *     name]"
   */
  @Override
  public String toString() {
    return "Condition: " + description + ", Parameter: " + parameter.name();
  }
}
