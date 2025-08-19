package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract parameter class representing a configurable crossover operator for evolutionary algorithms.
 * <p>
 * This class acts as a factory for creating specific {@link CrossoverOperator} instances
 * based on the selected crossover type. It extends {@link CategoricalParameter} to allow
 * selection among a list of available crossover operator names.
 * </p>
 *
 * @param <S> the solution type for which the crossover operator is defined
 */
public abstract class CrossoverParameter<S extends Solution<?>> extends CategoricalParameter {
  public static final String DEFAULT_NAME = "crossover";

  /**
   * Constructs a crossover parameter with the given list of available crossover operator names.
   *
   * @param crossoverOperators the list of supported crossover operator names
   */
  protected CrossoverParameter(List<String> crossoverOperators) {
    this(DEFAULT_NAME, crossoverOperators);
  }

  protected CrossoverParameter(String name, List<String> crossoverOperators) {
    super(name, crossoverOperators);
  }
  
  /**
   * Returns the configured {@link CrossoverOperator} instance for the selected crossover type.
   *
   * @return the crossover operator
   */
  public abstract CrossoverOperator<S> getCrossover();
}
