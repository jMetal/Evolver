package org.uma.evolver.parameter.catalogue.factory;

import java.util.List;

import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class DoubleParameterFactory {

  public static CategoricalParameter createCategoricalParameter(
      String parameterName, List<String> values) {

    CategoricalParameter parameter;
    switch (parameterName) {
      case "archiveType" -> parameter = new ExternalArchiveParameter<DoubleSolution>(values);
      case "aggregationFunction" -> parameter = new AggregationFunctionParameter(values);
      case "createInitialSolutions" -> parameter = new CreateInitialSolutionsDoubleParameter(values);
      case "crossover" -> parameter = new DoubleCrossoverParameter(values);
      case "crossoverRepairStrategy" -> parameter = new RepairDoubleSolutionStrategyParameter("crossoverRepairStrategy", values);
      case "densityEstimator" -> parameter = new DensityEstimatorParameter<DoubleSolution>(values);
      case "mutation" -> parameter = new DoubleMutationParameter(values);
      case "mutationRepairStrategy" -> parameter = new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", values);
      case "sequenceGenerator" -> parameter = new SequenceGeneratorParameter(parameterName, values);
      case "ranking" -> parameter = new RankingParameter<DoubleSolution>("ranking", values);
      case "replacement" -> parameter = new ReplacementParameter<DoubleSolution>(values);
      case "selection" -> parameter = new SelectionParameter<DoubleSolution>(values);
      default -> {
        parameter = new CategoricalParameter(parameterName, values);
      }
    }
    return parameter;
  }
}
