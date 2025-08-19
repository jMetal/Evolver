package org.uma.evolver.parameter.catalogue;

import org.uma.evolver.parameter.type.DoubleParameter;

/**
 * A specialized DoubleParameter that represents a probability value between 0.0 and 1.0 (inclusive).
 * This class is used to ensure that probability values used in evolutionary algorithms
 * remain within the valid range [0.0, 1.0].
 *
 * <p>Common uses include:
 * <ul>
 *   <li>Crossover probability (e.g., in genetic algorithms)</li>
 *   <li>Mutation probability</li>
 *   <li>Any other parameter that represents a probability</li>
 * </ul>
 *
 * <p>This class enforces the constraint that the value must be between 0.0 and 1.0.
 */
public class ProbabilityParameter extends DoubleParameter {
  /**
   * Creates a new ProbabilityParameter with the specified name.
   * The value will be constrained to the range [0.0, 1.0].
   *
   * @param name The name of the parameter. Must not be null or empty.
   * @throws IllegalArgumentException if name is null or empty
   */
  public ProbabilityParameter(String name)  {
    super(name, 0.0, 1.0);
  }
}
