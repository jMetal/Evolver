package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.DifferentialEvolutionCrossoverParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.DifferentialEvolutionCrossoverVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

public class VariationDoubleParameter extends VariationParameter<DoubleSolution> {

  public VariationDoubleParameter(List<String> variationStrategies) {
    super(variationStrategies);
  }

  public Variation<DoubleSolution> getVariation() {
    Variation<DoubleSolution> result;

    MutationParameter mutationParameter = (MutationParameter) findGlobalSubParameter("mutation");

    switch (value()) {
      case "crossoverAndMutationVariation" -> {
        int offspringPopulationSize;
        if (nonConfigurableSubParameters().containsKey("offspringPopulationSize")) {
          offspringPopulationSize = (Integer) nonConfigurableSubParameters().get(
                  "offspringPopulationSize");
        } else {
          throw new JMetalException("offspringPopulationSize parameter not found");
        }
        CrossoverParameter crossoverParameter =
                (CrossoverParameter) findSpecificSubParameter("crossover");
        if (mutationParameter == null) {
          mutationParameter = (MutationParameter) findSpecificSubParameter("mutation");
        }
        Check.notNull(mutationParameter);
        CrossoverOperator<DoubleSolution> crossoverOperator = crossoverParameter.getCrossover();
        MutationOperator<DoubleSolution> mutationOperatorOperator =
                mutationParameter.getMutation();
        result =
                new CrossoverAndMutationVariation<>(
                        offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
      }
      case "differentialEvolutionVariation" -> {
        var differentialEvolutionCrossoverParameter =
                (DifferentialEvolutionCrossoverParameter)
                        findSpecificSubParameter("differentialEvolutionCrossover");
        Check.notNull(differentialEvolutionCrossoverParameter);
        if (mutationParameter == null) {
          mutationParameter = (MutationParameter) findSpecificSubParameter("mutation");
        }
        Check.notNull(mutationParameter);
        var subProblemIdGenerator =
                (SequenceGenerator<Integer>) nonConfigurableSubParameters().get("subProblemIdGenerator");
        Check.notNull(subProblemIdGenerator);
        result =
                new DifferentialEvolutionCrossoverVariation(
                        1,
                        differentialEvolutionCrossoverParameter.getParameter(),
                        mutationParameter.getMutation(),
                        subProblemIdGenerator);
      }
      default -> throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }
}

