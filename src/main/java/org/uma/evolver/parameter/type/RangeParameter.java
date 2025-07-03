package org.uma.evolver.parameter.type;

import java.util.function.Predicate;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A generic parameter class for numeric values constrained within an inclusive range {@code
 * [lowerBound, upperBound]}. This abstract class provides built-in validation logic and requires
 * subclasses to implement type-specific string parsing.
 *
 * <p>This class is intended to be extended by concrete implementations such as {@code
 * IntegerParameter} and {@code DoubleParameter}, enabling reusable logic for bounded numerical
 * parameters in configurable systems.
 *
 * @param <T> The numeric and comparable type of the parameter (e.g., {@link Integer}, {@link
 *     Double})
 * @author Antonio J. Nebro
 */
public abstract class RangeParameter<T extends Number & Comparable<T>> extends Parameter<T> {
  /** Inclusive lower bound of the parameter. */
  protected final T lowerBound;

  /** Inclusive upper bound of the parameter. */
  protected final T upperBound;

  /** Predicate used to validate whether the parameter's value is within range. */
  protected final Predicate<T> validator;

  /**
   * Constructs a {@code RangeParameter} with the specified name and bounds.
   *
   * @param name the name of the parameter
   * @param lowerBound the inclusive lower bound
   * @param upperBound the inclusive upper bound
   * @throws NullPointerException if {@code lowerBound} or {@code upperBound} is null
   * @throws IllegalArgumentException if {@code lowerBound >= upperBound}
   */
  protected RangeParameter(String name, T lowerBound, T upperBound) {
    super(name);
    Check.notNull(lowerBound);
    Check.notNull(upperBound);
    Check.that(
        lowerBound.compareTo(upperBound) < 0,
        "Lower bound " + lowerBound + " must be less than upper bound " + upperBound);

    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.validator =
        value ->
            value != null && value.compareTo(lowerBound) >= 0 && value.compareTo(upperBound) <= 0;
  }

  /**
   * Parses the parameter value from a string array and validates it against the defined range.
   *
   * @param arguments the command-line style arguments array
   * @return the current instance after successful parsing and validation
   * @throws JMetalException if the parsed value is outside the allowed range
   */
  @Override
  public void parse(String[] arguments) {
    parse(this::parseValue, arguments);
    if (!validator.test(value())) {
      throw new JMetalException(
          "Parameter "
              + name()
              + ": Invalid value: "
              + value()
              + ". Range: ["
              + lowerBound
              + ", "
              + upperBound
              + "]");
    }
  }

  /**
   * Converts a string into a value of type {@code T}. Must be implemented by subclasses.
   *
   * @param stringValue the string to convert
   * @return the parsed value
   * @throws NumberFormatException if the string cannot be parsed to the correct type
   */
  protected abstract T parseValue(String stringValue);

  /**
   * Returns the minimum valid value (lower bound) for this parameter.
   *
   * @return the inclusive lower bound
   */
  public T minValue() {
    return lowerBound;
  }

  /**
   * Returns the maximum valid value (upper bound) for this parameter.
   *
   * @return the inclusive upper bound
   */
  public T maxValue() {
    return upperBound;
  }

  /**
   * Returns a string representation of the parameter including name, value, and bounds.
   *
   * @return a string describing the parameter
   */
  @Override
  public String toString() {
    return "Name: "
        + name()
        + ". Value: "
        + value()
        + ". "
        + ". Lower bound: "
        + lowerBound
        + ". Upper bound: "
        + upperBound;
  }

}
