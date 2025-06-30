package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class VariationBinaryParameter extends VariationParameter<BinarySolution> {
  public VariationBinaryParameter(List<String> variationStrategies) {
    super(variationStrategies);
  }

  public Variation<BinarySolution> getVariation() {
    Variation<BinarySolution> result;
    int offspringPopulationSize = (Integer) findGlobalSubParameter("offspringPopulationSize").value();

    if ("crossoverAndMutationVariation".equals(value())) {
      CrossoverParameter crossoverParameter =
              (CrossoverParameter) findSpecificSubParameter("crossover");
      MutationParameter mutationParameter = (MutationParameter) findSpecificSubParameter("mutation");

      CrossoverOperator<BinarySolution> crossoverOperator =
              crossoverParameter.getCrossover();
      MutationOperator<BinarySolution> mutationOperatorOperator =
              mutationParameter.getMutation();

      result =
              new CrossoverAndMutationVariation<>(
                      offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
    } else {
      throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }
}

