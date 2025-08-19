package org.uma.evolver.algorithm.base;

import java.util.List;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Interface representing a configurable evolutionary algorithm.
 * <p>
 * An {@code BaseLevelAlgorithm} defines the contract for evolutionary algorithms that can be
 * configured via a {@link ParameterSpace}, instantiated for a specific problem, and built
 * into a ready-to-run {@link org.uma.jmetal.algorithm.Algorithm} instance.
 * </p>
 *
 * <p>
 * Typical usage involves:
 * <ul>
 *   <li>Accessing and configuring the parameter space.</li>
 *   <li>Parsing command-line arguments or other configuration sources.</li>
 *   <li>Creating an instance for a specific problem and evaluation budget.</li>
 *   <li>Building the algorithm and running it.</li>
 * </ul>
 * </p>
 *
 * @param <S> the solution type handled by the algorithm
 */
public interface BaseLevelAlgorithm<S extends Solution<?>> {
  /**
   * Returns the parameter space associated with this algorithm.
   *
   * @return the parameter space
   */
  ParameterSpace parameterSpace();

  /**
   * Builds and returns a configured {@link Algorithm} instance.
   *
   * @return the configured algorithm
   */
  Algorithm<List<S>> build();

  /**
   * Creates a new instance of the algorithm for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of the algorithm
   */
  BaseLevelAlgorithm<S> createInstance(Problem<S> problem, int maximumNumberOfEvaluations);

  /**
   * Parses the given arguments and configures all top-level parameters in the parameter space.
   * Returns {@code this} for fluent usage.
   *
   * @param args the arguments to parse
   * @return this algorithm instance, configured according to the arguments
   */
  default BaseLevelAlgorithm<S> parse(String[] args) {
    for (Parameter<?> parameter : parameterSpace().topLevelParameters()) {
      parameter.parse(args);
    }
    return this;
  }
}
