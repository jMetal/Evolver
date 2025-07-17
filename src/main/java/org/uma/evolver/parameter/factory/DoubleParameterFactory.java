package org.uma.evolver.parameter.factory;

import java.util.List;

import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.DoubleVariationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Factory class for creating and configuring categorical parameters specific to evolutionary algorithms
 * that operate on DoubleSolution individuals. This factory implements the ParameterFactory interface
 * to provide type-safe creation of various algorithm components and their parameters.
 *
 * <p>The factory supports the creation of parameters for different components including:
 * <ul>
 *   <li>Variation operators (crossover, mutation)</li>
 *   <li>Selection mechanisms</li>
 *   <li>Archiving strategies</li>
 *   <li>Solution initialization methods</li>
 *   <li>Density estimators</li>
 *   <li>And other algorithm-specific parameters</li>
 * </ul>
 *
 * <p>This implementation is specifically designed for algorithms working with double-encoded solutions
 * and provides appropriate parameter types that are compatible with the DoubleSolution interface.
 *
 * @author Antonio J. Nebro
 * @since 1.0
 */
public class DoubleParameterFactory implements ParameterFactory<DoubleSolution> {

  /**
   * Creates and returns a specific {@link CategoricalParameter} instance based on the provided parameter name.
   * This factory method centralizes the creation of all parameter types used in evolutionary algorithms
   * that operate on double-encoded solutions.
   *
   * <p>The following parameter types are supported:
   * <table border="1">
   *   <caption>Supported Parameter Types</caption>
   *   <tr><th>Parameter Name</th><th>Creates</th><th>Description</th></tr>
   *   <tr><td>archiveType</td><td>{@link ExternalArchiveParameter}</td><td>External archive type for storing non-dominated solutions</td></tr>
   *   <tr><td>aggregationFunction</td><td>{@link AggregationFunctionParameter}</td><td>Function for aggregating multiple objectives</td></tr>
   *   <tr><td>createInitialSolutions</td><td>{@link CreateInitialSolutionsDoubleParameter}</td><td>Strategy for creating initial population</td></tr>
   *   <tr><td>crossover</td><td>{@link DoubleCrossoverParameter}</td><td>Double-solution crossover operator</td></tr>
   *   <tr><td>crossoverRepairStrategy</td><td>{@link RepairDoubleSolutionStrategyParameter}</td><td>Repair strategy for crossover operations</td></tr>
   *   <tr><td>densityEstimator</td><td>{@link DensityEstimatorParameter}</td><td>Density estimation method for solution selection</td></tr>
   *   <tr><td>mutation</td><td>{@link DoubleMutationParameter}</td><td>Double-solution mutation operator</td></tr>
   *   <tr><td>mutationRepairStrategy</td><td>{@link RepairDoubleSolutionStrategyParameter}</td><td>Repair strategy for mutation operations</td></tr>
   *   <tr><td>sequenceGenerator</td><td>{@link SequenceGeneratorParameter}</td><td>Generator for sequence-based parameters</td></tr>
   *   <tr><td>replacement</td><td>{@link ReplacementParameter}</td><td>Strategy for population replacement</td></tr>
   *   <tr><td>selection</td><td>{@link SelectionParameter}</td><td>Parent selection mechanism</td></tr>
   *   <tr><td>variation</td><td>{@link DoubleVariationParameter}</td><td>Variation operator for double solutions</td></tr>
   *   <tr><td>differentialEvolutionCrossover</td><td>{@link DifferentialEvolutionCrossoverParameter}</td><td>Differential evolution crossover operator</td></tr>
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
      case "archiveType" -> parameter = new ExternalArchiveParameter<DoubleSolution>("archiveType", values);
      case "aggregationFunction" -> parameter = new AggregationFunctionParameter(values);
      case "createInitialSolutions" -> parameter = new CreateInitialSolutionsDoubleParameter(values);
      case "crossover" -> parameter = new DoubleCrossoverParameter(values);
      case "crossoverRepairStrategy" -> parameter = new RepairDoubleSolutionStrategyParameter("crossoverRepairStrategy", values);
      case "densityEstimator" -> parameter = new DensityEstimatorParameter<DoubleSolution>(values);
      case "mutation" -> parameter = new DoubleMutationParameter(values);
      case "mutationRepairStrategy" -> parameter = new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", values);
      case "sequenceGenerator", "subProblemIdGenerator" -> parameter = new SequenceGeneratorParameter(parameterName, values);
      case "ranking" -> parameter = new RankingParameter<DoubleSolution>("ranking", values);
      case "replacement" -> parameter = new ReplacementParameter<DoubleSolution>(values);
      case "selection" -> parameter = new SelectionParameter<DoubleSolution>(values);
      case "variation" -> parameter = new DoubleVariationParameter(values) ;
      case "differentialEvolutionCrossover" -> parameter = new DifferentialEvolutionCrossoverParameter(values);
      default -> {
        parameter = new CategoricalParameter(parameterName, values);
      }
    }
    return parameter;
  }
}
