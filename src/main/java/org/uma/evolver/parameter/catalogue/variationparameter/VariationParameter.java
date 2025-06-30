package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.solution.Solution;

/**
 * Abstract parameter class representing a configurable variation operator in evolutionary algorithms.
 * <p>
 * This class serves as a base for specific variation operator parameters, providing a common
 * interface for creating variation operators that can be configured at runtime. It extends
 * {@link CategoricalParameter} to allow selection among different variation strategies.
 *
 * <p>Variation operators are responsible for creating new solutions by combining and modifying
 * existing ones, typically through crossover and mutation operations.
 *
 * @param <S> The type of solutions being evolved
 */
public abstract class VariationParameter<S extends Solution<?>> extends CategoricalParameter {
  
  /**
   * Constructs a new VariationParameter with the specified list of variation strategy names.
   *
   * @param variationStrategies the list of supported variation strategy names
   * @throws IllegalArgumentException if variationStrategies is null or empty
   */
  protected VariationParameter(List<String> variationStrategies) {
    super("variation", variationStrategies);
  }

  /**
   * Creates and returns a configured {@link Variation} operator based on the current parameter value.
   * The specific implementation is provided by concrete subclasses.
   *
   * @return a configured variation operator
   * @throws IllegalStateException if the operator cannot be created with the current configuration
   */
  public abstract Variation<S> getVariation();
}

