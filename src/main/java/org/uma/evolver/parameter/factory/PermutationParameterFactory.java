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
   * Creates and returns a specific {@link CategoricalParameter} instance based on the provided parameter name.
   * This factory method centralizes the creation of all parameter types used in evolutionary algorithms
   * that operate on permutation-encoded solutions.
   *
   * <p>The following parameter types are supported:
   * <table border="1">
   *   <caption>Supported Parameter Types</caption>
   *   <tr><th>Parameter Name</th><th>Creates</th><th>Description</th></tr>
   *   <tr><td>archiveType</td><td>{@link ExternalArchiveParameter}</td><td>External archive type for storing non-dominated solutions</td></tr>
   *   <tr><td>aggregationFunction</td><td>{@link AggregationFunctionParameter}</td><td>Function for aggregating multiple objectives</td></tr>
   *   <tr><td>createInitialSolutions</td><td>{@link CreateInitialSolutionsPermutationParameter}</td><td>Strategy for creating initial population of permutation solutions</td></tr>
   *   <tr><td>crossover</td><td>{@link PermutationCrossoverParameter}</td><td>Permutation-solution crossover operator</td></tr>
   *   <tr><td>densityEstimator</td><td>{@link DensityEstimatorParameter}</td><td>Density estimation method for solution selection</td></tr>
   *   <tr><td>mutation</td><td>{@link PermutationMutationParameter}</td><td>Permutation-solution mutation operator</td></tr>
   *   <tr><td>sequenceGenerator</td><td>{@link SequenceGeneratorParameter}</td><td>Generator for sequence-based parameters</td></tr>
   *   <tr><td>ranking</td><td>{@link RankingParameter}</td><td>Solution ranking method</td></tr>
   *   <tr><td>replacement</td><td>{@link ReplacementParameter}</td><td>Strategy for population replacement</td></tr>
   *   <tr><td>selection</td><td>{@link SelectionParameter}</td><td>Parent selection mechanism</td></tr>
   *   <tr><td>variation</td><td>{@link PermutationVariationParameter}</td><td>Variation operator for permutation solutions</td></tr>
   *   <tr><td>subProblemIdGenerator</td><td>{@link SequenceGeneratorParameter}</td><td>Generator for sub-problem identifiers</td></tr>
   *   <tr><td>any other value</td><td>{@link CategoricalParameter}</td><td>Basic categorical parameter with the given name</td></tr>
   * </table>
   *
   * @param parameterName the name of the parameter to create (case-sensitive)
   * @param values the list of possible string values for the parameter (must not be null or empty)
   * @return an instance of {@code CategoricalParameter} corresponding to the specified parameter name
   * @throws IllegalArgumentException if the values list is null or empty
   * @see CategoricalParameter
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
