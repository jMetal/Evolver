package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract parameter class representing a configurable mutation operator for evolutionary algorithms.
 * <p>
 * This class acts as a factory for creating specific {@link MutationOperator} instances
 * based on the selected mutation type. It extends {@link CategoricalParameter} to allow
 * selection among a list of available mutation operator names.
 * </p>
 *
 * @param <S> the solution type for which the mutation operator is defined
 */
public abstract class MutationParameter<S extends Solution<?>> extends CategoricalParameter {
  public static final String DEFAULT_NAME = "mutation";

  /**
   * Constructs a mutation parameter with the given list of available mutation operator names.
   *
   * @param mutationOperators the list of supported mutation operator names
   * @throws IllegalArgumentException if mutationOperators is null or empty
   */
  public MutationParameter(List<String> mutationOperators) {
    this(DEFAULT_NAME, mutationOperators);
  }

  public MutationParameter(String name, List<String> mutationOperators) {
    super(name, mutationOperators);
  }

  /**
   * Creates and returns a configured {@link MutationOperator} instance based on the current parameter value.
   * The specific implementation is provided by concrete subclasses.
   *
   * @return a configured mutation operator
   * @throws IllegalStateException if the operator cannot be created with the current configuration
   */
  public abstract MutationOperator<S> getMutation();
}
