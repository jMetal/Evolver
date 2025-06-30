package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class VariationPermutationParameter extends VariationParameter<PermutationSolution<Integer>> {
  public VariationPermutationParameter(List<String> variationStrategies) {
    super(variationStrategies);
  }

  public Variation<PermutationSolution<Integer>> getVariation() {
    Variation<PermutationSolution<Integer>> result;
    int offspringPopulationSize;
    if (nonConfigurableSubParameters().containsKey("offspringPopulationSize")) {
      offspringPopulationSize = (Integer) nonConfigurableSubParameters().get(
              "offspringPopulationSize");
    } else {
      throw new JMetalException("offspringPopulationSize parameter not found");
    }

    if ("crossoverAndMutationVariation".equals(value())) {
      CrossoverParameter crossoverParameter =
              (CrossoverParameter) findSpecificSubParameter("crossover");
      MutationParameter mutationParameter = (MutationParameter) findSpecificSubParameter("mutation");

      CrossoverOperator<PermutationSolution<Integer>> crossoverOperator =
              crossoverParameter.getCrossover();
      MutationOperator<PermutationSolution<Integer>> mutationOperatorOperator =
              mutationParameter.getMutation();

      result = new CrossoverAndMutationVariation<>(
              offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
    } else {
      throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }
}

