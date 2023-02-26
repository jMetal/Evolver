package org.uma.evolver.parameter;

import java.util.List;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.catalogue.CrossoverParameter;
import org.uma.jmetal.auto.parameter.catalogue.DifferentialEvolutionCrossoverParameter;
import org.uma.jmetal.auto.parameter.catalogue.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.DifferentialEvolutionCrossoverVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

public class VariationParameter extends CategoricalParameter {
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
        } else if (findSpecificParameter("offspringPopulationSize") != null) {
          offspringPopulationSize = (Integer) findSpecificParameter(
              "offspringPopulationSize").value();
        } else {
          throw new JMetalException("offspringPopulationSize parameter not found");
        }
        CrossoverParameter crossoverParameter =
            (CrossoverParameter) findSpecificParameter("crossover");
        MutationParameter mutationParameter = (MutationParameter) findSpecificParameter("mutation");
        CrossoverOperator<DoubleSolution> crossoverOperator = crossoverParameter.getDoubleSolutionParameter();
        MutationOperator<DoubleSolution> mutationOperatorOperator =
            mutationParameter.getDoubleSolutionParameter();
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
                mutationDEParameter.getDoubleSolutionParameter(),
                subProblemIdGenerator);
      }
      default -> throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }

  public Variation<? extends BinarySolution> getBinarySolutionParameter() {
    Variation<BinarySolution> result;
    int offspringPopulationSize = (Integer)findGlobalParameter("offspringPopulationSize").value() ;

    if ("crossoverAndMutationVariation".equals(value())) {
      CrossoverParameter crossoverParameter =
          (CrossoverParameter) findSpecificParameter("crossover");
      MutationParameter mutationParameter = (MutationParameter) findSpecificParameter("mutation");

      CrossoverOperator<BinarySolution> crossoverOperator = crossoverParameter.getBinarySolutionParameter();
      MutationOperator<BinarySolution> mutationOperatorOperator =
          mutationParameter.getBinarySolutionParameter();

      result =
          new CrossoverAndMutationVariation<>(
              offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
    } else {
      throw new JMetalException("Variation component unknown: " + value());
    }

    return result;
  }

  @Override
  public String name() {
    return "variation";
  }
}
