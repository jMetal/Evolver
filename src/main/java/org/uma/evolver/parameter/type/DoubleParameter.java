package org.uma.evolver.parameter.type;

/**
 * A parameter representing a {@code double} value constrained within a specified inclusive range.
 * <p>
 * This class is a concrete implementation of {@link RangeParameter} for the {@link Double} type.
 * It supports automatic parsing from command-line style arguments and validation against the
 * configured bounds.
 *
 * <p>Example usage:
 * <pre>{@code
 * DoubleParameter param = new DoubleParameter("threshold", 0.0, 1.0);
 * param.parse(new String[] { "--threshold", "0.5" });
 * Double value = param.value(); // 0.5
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class DoubleParameter extends RangeParameter<Double> {

  /**
   * Constructs a {@code DoubleParameter} with the given name and range.
   *
   * @param name the parameter name
   * @param lowerBound the inclusive lower bound
   * @param upperBound the inclusive upper bound
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if {@code lowerBound >= upperBound}
   */
  public DoubleParameter(String name, double lowerBound, double upperBound) {
    super(name, lowerBound, upperBound);
  }

  /**
   * Parses a string into a {@link Double}.
   *
   * @param stringValue the string to parse
   * @return the parsed {@link Double} value
   * @throws NumberFormatException if the string is not a valid {@code double}
   */
  @Override
  protected Double parseValue(String stringValue) {
    return Double.parseDouble(stringValue);
  }
}