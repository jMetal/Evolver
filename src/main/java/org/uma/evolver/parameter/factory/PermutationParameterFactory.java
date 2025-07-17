package org.uma.evolver.parameter.factory;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsPermutationParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.PermutationCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.PermutationMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.PermutationVariationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Factory class for creating and configuring categorical parameters specific to evolutionary algorithms
 * that operate on permutation-based solutions. This factory implements the ParameterFactory interface
 * to provide type-safe creation of various algorithm components and their parameters.
 *
 * <p>The factory supports the creation of parameters for different components including:
 * <ul>
 *   <li>Variation operators (crossover, mutation) for permutation solutions</li>
 *   <li>Selection mechanisms</li>
 *   <li>Archiving strategies</li>
 *   <li>Solution initialization methods</li>
 *   <li>Density estimators</li>
 *   <li>And other algorithm-specific parameters</li>
 * </ul>
 *
 * <p>This implementation is specifically designed for algorithms working with permutation-encoded solutions
 * and provides appropriate parameter types that are compatible with the PermutationSolution interface.
 *
 * @author Antonio J. Nebro
 * @since 1.0
 */
public class PermutationParameterFactory implements ParameterFactory<PermutationSolution<Integer>> {

  /**
   * Creates and returns a specific CategoricalParameter instance based on the provided parameter name.
   * This factory method supports the creation of various types of parameters used in evolutionary algorithms,
   * such as selection, crossover, mutation, and other algorithm-specific parameters.
   *
   * @param parameterName the name of the parameter to create. Supported values are:
   *                     - "archiveType": Creates an ExternalArchiveParameter
   *                     - "aggregationFunction": Creates an AggregationFunctionParameter
   *                     - "createInitialSolutions": Creates a CreateInitialSolutionsDoubleParameter
   *                     - "crossover": Creates a DoubleCrossoverParameter
   *                     - "crossoverRepairStrategy": Creates a RepairDoubleSolutionStrategyParameter for crossover
   *                     - "densityEstimator": Creates a DensityEstimatorParameter
   *                     - "mutation": Creates a DoubleMutationParameter
   *                     - "mutationRepairStrategy": Creates a RepairDoubleSolutionStrategyParameter for mutation
   *                     - "sequenceGenerator": Creates a SequenceGeneratorParameter
   *                     - "ranking": Creates a RankingParameter
   *                     - "replacement": Creates a ReplacementParameter
   *                     - "selection": Creates a SelectionParameter
   *                     - "variation": Creates a VariationParameter
   *                     - Any other value: Creates a basic CategoricalParameter
   * @param values the list of possible values for the parameter
   * @return an instance of CategoricalParameter corresponding to the specified parameter name
   * @throws IllegalArgumentException if the values list is null or empty
   */
  @Override
  public CategoricalParameter createParameter(
      String parameterName, List<String> values) {
    
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values cannot be null or empty");
    }

    CategoricalParameter parameter;
    switch (parameterName) {
      case "archiveType" -> parameter = new ExternalArchiveParameter<PermutationSolution<Integer>>("archiveType", values);
      case "aggregationFunction" -> parameter = new AggregationFunctionParameter(values);
      case "createInitialSolutions" -> parameter = new CreateInitialSolutionsPermutationParameter(values);
      case "crossover" -> parameter = new PermutationCrossoverParameter(values);
      case "densityEstimator" -> parameter = new DensityEstimatorParameter<PermutationSolution<Integer>>(values);
      case "mutation" -> parameter = new PermutationMutationParameter(values);
      case "sequenceGenerator", "subProblemIdGenerator" -> parameter = new SequenceGeneratorParameter(parameterName, values);
      case "ranking" -> parameter = new RankingParameter<PermutationSolution<Integer>>("ranking", values);
      case "replacement" -> parameter = new ReplacementParameter<PermutationSolution<Integer>>(values);
      case "selection" -> parameter = new SelectionParameter<PermutationSolution<Integer>>(values);
      case "variation" -> parameter = new PermutationVariationParameter(values) ;
      default -> {
        parameter = new CategoricalParameter(parameterName, values);
      }
    }
    return parameter;
  }
}
