package org.uma.evolver.parameter.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.DoubleVariationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Factory class for creating and configuring categorical parameters specific to evolutionary
 * algorithms that operate on DoubleSolution individuals. This factory implements the
 * ParameterFactory interface to provide type-safe creation of various algorithm components and
 * their parameters.
 *
 * <p>The factory supports the creation of parameters for different components including:
 *
 * <ul>
 *   <li>Variation operators (crossover, mutation)
 *   <li>Selection mechanisms
 *   <li>Archiving strategies
 *   <li>Solution initialization methods
 *   <li>Density estimators
 *   <li>And other algorithm-specific parameters
 * </ul>
 *
 * <p>This implementation is specifically designed for algorithms working with double-encoded
 * solutions and provides appropriate parameter types that are compatible with the DoubleSolution
 * interface.
 *
 * <p>This implementation uses the Registry Pattern, allowing for dynamic registration of parameter
 * creators and making it more extensible than a traditional switch-based factory.
 *
 * @author Antonio J. Nebro
 * @since 1.0
 */
public class DoubleParameterFactory implements ParameterFactory<DoubleSolution> {

  private final Map<String, Function<List<String>, CategoricalParameter>> registry;

  /**
   * Creates a new instance of DoubleParameterFactory with default parameter creators registered.
   */
  public DoubleParameterFactory() {
    this.registry = new HashMap<>();
    registerDefaultCreators();
  }

  /**
   * Registers a parameter creator for a specific parameter type.
   *
   * @param type the parameter type to register
   * @param creator the function that creates the parameter
   * @return this factory instance for method chaining
   * @throws NullPointerException if type or creator is null
   */
  public DoubleParameterFactory register(
      String type, Function<List<String>, CategoricalParameter> creator) {
    Objects.requireNonNull(type, "Parameter type cannot be null");
    Objects.requireNonNull(creator, "Creator function cannot be null");
    registry.put(type, creator);
    return this;
  }

  /**
   * Creates and returns a specific {@link CategoricalParameter} instance based on the provided
   * parameter name.
   *
   * <p>If no specific creator is registered for the parameter name, a default {@link
   * CategoricalParameter} with the given name and values will be created.
   *
   * @param parameterName the name of the parameter to create (case-sensitive)
   * @param values the list of possible string values for the parameter (must not be null or empty)
   * @return an instance of {@code CategoricalParameter} corresponding to the specified parameter
   *     name
   * @throws IllegalArgumentException if the values list is null or empty
   */
  @Override
  public CategoricalParameter createParameter(String parameterName, List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values cannot be null or empty");
    }

    return registry
        .getOrDefault(parameterName, v -> new CategoricalParameter(parameterName, v))
        .apply(values);
  }

  /** Registers the default set of parameter creators. */
  private void registerDefaultCreators() {
    // Archive types
    register("archiveType", values -> new ExternalArchiveParameter<>("archiveType", values));

    // Aggregation functions
    register("aggregationFunction", AggregationFunctionParameter::new);

    // Initial solution creation
    register("createInitialSolutions", CreateInitialSolutionsDoubleParameter::new);

    // Crossover operators and strategies
    register("crossover", DoubleCrossoverParameter::new);
    register(
        "crossoverRepairStrategy",
        values -> new RepairDoubleSolutionStrategyParameter("crossoverRepairStrategy", values));
    register("differentialEvolutionCrossover", DifferentialEvolutionCrossoverParameter::new);

    // Density estimation
    register("densityEstimator", values -> new DensityEstimatorParameter<DoubleSolution>(values));

    // Mutation operators and strategies
    register("mutation", DoubleMutationParameter::new);
    register(
        "mutationRepairStrategy",
        values -> new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", values));

    // Sequence generation
    register(
        "sequenceGenerator", values -> new SequenceGeneratorParameter("sequenceGenerator", values));
    register(
        "subProblemIdGenerator",
        values -> new SequenceGeneratorParameter("subProblemIdGenerator", values));

    // Ranking and selection
    register("ranking", values -> new RankingParameter<DoubleSolution>("ranking", values));

    // Replacement strategies
    register("replacement", values -> new ReplacementParameter<DoubleSolution>(values));

    // Selection mechanisms
    register("selection", values -> new SelectionParameter<DoubleSolution>(values));

    // Variation operators
    register("variation", DoubleVariationParameter::new);
  }
}
