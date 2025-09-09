package org.uma.evolver.parameter.factory;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;

/**
 * A factory interface for creating {@link CategoricalParameter} instances specific to different types of solutions.
 * 
 * <p>This interface serves as the base for creating type-safe parameter factories for various solution
 * representations (e.g., binary, permutation, real-valued) in evolutionary algorithms. Implementations of this
 * interface provide concrete parameter creation logic tailored to specific solution types.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Type-safe parameter creation through generics</li>
 *   <li>Centralized parameter instantiation</li>
 *   <li>Support for different solution representations</li>
 *   <li>Flexible configuration through string-based parameter names</li>
 * </ul>
 *
 * <p>Implementations should handle the creation of all necessary parameters for configuring evolutionary
 * algorithm components such as variation operators, selection mechanisms, and archiving strategies.</p>
 *
 * @param <S> The type of solution this factory creates parameters for
 * 
 * @author Antonio J. Nebro
 * @since 1.0
 */
@FunctionalInterface
public interface ParameterFactory<S extends Solution<?>> {
  
  /**
   * Creates and returns a specific {@link CategoricalParameter} instance based on the provided parameter name.
   *
   * <p>This method serves as a factory for creating different types of parameters used in configuring
   * evolutionary algorithms. The actual type of parameter created depends on the {@code parameterName}.
   *
   * @param parameterName the name of the parameter to create (case-sensitive)
   * @param values the list of possible string values for the parameter (must not be null or empty)
   * @return an instance of {@code CategoricalParameter} corresponding to the specified parameter name
   * @throws IllegalArgumentException if the values list is null or empty
   * @see CategoricalParameter
   */
  CategoricalParameter createParameter(String parameterName, List<String> values);
}
