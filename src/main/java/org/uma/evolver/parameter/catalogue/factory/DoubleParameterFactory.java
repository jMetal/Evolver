package org.uma.evolver.parameter.catalogue.factory;

import java.util.List;

import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class DoubleParameterFactory {

  public CategoricalParameter createCategoricalParameter(
      String parameterName, List<String> values) {
    switch (parameterName) {
      case "archiveType" -> {
        return new ExternalArchiveParameter<DoubleSolution>(values);
      }
      case "aggregationFunction" -> {
        return new AggregationFunctionParameter(values);
      }
      case "createInitialSolutions" -> {
        return new CreateInitialSolutionsDoubleParameter(values);
      }
      case "crossover" -> {
        return new DoubleCrossoverParameter(values);
      }
      case "crossoverRepairStrategy" -> {
        return new RepairDoubleSolutionStrategyParameter("crossoverRepairStrategy", values);
      }
      case "densityEstimator" -> {
        return new DensityEstimatorParameter<DoubleSolution>(values);
      }
      case "mutation" -> {
        return new DoubleMutationParameter(values);
      }
      case "mutationRepairStrategy" -> {
        return new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", values);
      }
      case "sequenceGenerator" -> {
        return new SequenceGeneratorParameter(parameterName, values);
      }
      case "ranking" -> {
        return new RankingParameter<DoubleSolution>("ranking", values);
      }
      case "replacement" -> {
        return new ReplacementParameter<DoubleSolution>(values);
      }
      default -> {
        return new CategoricalParameter(parameterName, values);
      }
    }
  }
}
