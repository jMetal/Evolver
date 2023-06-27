package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.DifferentialEvolutionCrossoverVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

public class VariationParameter extends CategoricalParameter {
  int counter = 0 ;
  public VariationParameter(List<String> variationStrategies) {
    super("variation", variationStrategies);
  }

  public Variation<? extends DoubleSolution> getDoubleSolutionParameter() {
    Variation<DoubleSolution> result;

    switch (value()) {
      case "crossoverAndMutationVariation" -> {
        int offspringPopulationSize;
        if (nonConfigurableParameters().containsKey("offspringPopulationSize")) {
          offspringPopulationSize = (Integer) getNonConfigurableParameter(
              "offspringPopulationSize");
        } else {
          throw new JMetalException("offspringPopulationSize parameter not found");
        }
        CrossoverParameter crossoverParameter =
            (CrossoverParameter) findSpecificParameter("crossover");
        var mutationParameter = (MutationParameter) findSpecificParameter("mutation");
        CrossoverOperator<DoubleSolution> crossoverOperator = crossoverParameter.getDoubleSolutionParameter();
        MutationOperator<DoubleSolution> mutationOperatorOperator =
            mutationParameter.getParameter();
        result =
            new CrossoverAndMutationVariation<>(
                offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
      }
      case "differentialEvolutionVariation" -> {
        var differentialEvolutionCrossoverParameter =
            (DifferentialEvolutionCrossoverParameter)
                findSpecificParameter("differentialEvolutionCrossover");
        Check.notNull(differentialEvolutionCrossoverParameter);
        var mutationDEParameter = (MutationParameter) findSpecificParameter("mutation");
        Check.notNull(mutationDEParameter);
        var subProblemIdGenerator =
            (SequenceGenerator<Integer>) getNonConfigurableParameter("subProblemIdGenerator");
        Check.notNull(subProblemIdGenerator);
        result =
            new DifferentialEvolutionCrossoverVariation(
                1,
                differentialEvolutionCrossoverParameter.getParameter(),
                mutationDEParameter.getParameter(),
                subProblemIdGenerator);
      }
      default -> throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }

  @Override
  public String name() {
    return "variation";
  }
}

