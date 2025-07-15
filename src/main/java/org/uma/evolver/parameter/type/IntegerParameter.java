package org.uma.evolver.parameter.type;

/**
 * A parameter representing an {@code int} value constrained within a specified inclusive range.
 * <p>
 * This class is a concrete implementation of {@link RangeParameter} for the {@link Integer} type.
 * It supports parsing from command-line arguments and ensures the value falls within the specified range.
 *
 * <p>Example usage:
 * <pre>{@code
 * IntegerParameter param = new IntegerParameter("iterations", 1, 100);
 * param.parse(new String[] { "--iterations", "50" });
 * Integer value = param.value(); // 50
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class IntegerParameter extends RangeParameter<Integer> {
 /**
   * Constructs an {@code IntegerParameter} with the given name and range.
   *
   * @param name the parameter name
   * @param lowerBound the inclusive lower bound
   * @param upperBound the inclusive upper bound
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if {@code lowerBound >= upperBound}
   */
  public IntegerParameter(String name, int lowerBound, int upperBound) {
    super(name, lowerBound, upperBound);
  }

  /**
   * Parses a string into an {@link Integer}.
   *
   * @param stringValue the string to parse
   * @return the parsed {@link Integer} value
   * @throws NumberFormatException if the string is not a valid {@code int}
   */
  @Override
  protected Integer parseValue(String stringValue) {
    return Integer.parseInt(stringValue);
  }
}