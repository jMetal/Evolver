package org.uma.evolver.parameter.factory;

import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.parameter.catalogue.DensityEstimatorParameter;
import org.uma.evolver.parameter.catalogue.RankingParameter;
import org.uma.evolver.parameter.catalogue.ReplacementParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsTreeParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.TreeCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.TreeMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.TreeVariationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;

/**
 * Parameter factory for algorithms working on derivation tree solutions ({@link
 * DerivationTreeSolution}), i.e. meta-optimizers using the tree encoding. It maps {@code
 * createInitialSolutions}, {@code crossover}, {@code mutation} and {@code variation} to their
 * tree-specific parameters; ranking, density estimation, replacement and selection are
 * encoding-independent.
 */
public class TreeParameterFactory implements ParameterFactory<DerivationTreeSolution> {

  @Override
  public CategoricalParameter createParameter(String parameterName, List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values cannot be null or empty");
    }

    return switch (parameterName) {
      case "createInitialSolutions" -> new CreateInitialSolutionsTreeParameter(values);
      case "crossover" -> new TreeCrossoverParameter(values);
      case "mutation" -> new TreeMutationParameter(values);
      case "variation" -> new TreeVariationParameter(values);
      case "densityEstimator" -> new DensityEstimatorParameter<DerivationTreeSolution>(values);
      case "ranking" -> new RankingParameter<DerivationTreeSolution>("ranking", values);
      case "replacement" -> new ReplacementParameter<DerivationTreeSolution>(values);
      case "selection" -> new SelectionParameter<DerivationTreeSolution>(values);
      default -> new CategoricalParameter(parameterName, values);
    };
  }
}
