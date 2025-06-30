package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class DoubleMutationParameter extends MutationParameter<DoubleSolution> {
  private static final List<String> validCrossoverNames = List.of("polynomial", "linkedPolynomial", "uniform", "nonUniform");

  public DoubleMutationParameter(List<String> mutationOperators) {
    super(mutationOperators);

    mutationOperators.stream()
            .filter(crossoverOperator -> !validCrossoverNames.contains(crossoverOperator))
            .forEach(
                    crossoverOperator -> {
                      throw new JMetalException(
                              "Invalid crossover operator name: "
                                      + crossoverOperator
                                      + ". Supported names are: "
                                      + validCrossoverNames);
                    });
  }

  @Override
  public MutationOperator<DoubleSolution> getMutation() {
    MutationOperator<DoubleSolution> result;
    int numberOfProblemVariables =
        (int) nonConfigurableSubParameters().get("numberOfProblemVariables");
    double mutationProbability =
        (double) findGlobalSubParameter("mutationProbabilityFactor").value()
            / numberOfProblemVariables;
    RepairDoubleSolutionStrategyParameter repairDoubleSolution =
        (RepairDoubleSolutionStrategyParameter) findGlobalSubParameter("mutationRepairStrategy");

    switch (value()) {
      case "polynomial" -> {
        Double distributionIndex =
            (Double) findSpecificSubParameter("polynomialMutationDistributionIndex").value();
        result =
            new PolynomialMutation(
                mutationProbability,
                distributionIndex,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "linkedPolynomial" -> {
        Double distributionIndex =
            (Double) findSpecificSubParameter("linkedPolynomialMutationDistributionIndex").value();
        result =
            new LinkedPolynomialMutation(
                mutationProbability,
                distributionIndex,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "uniform" -> {
        Double perturbation =
            (Double) findSpecificSubParameter("uniformMutationPerturbation").value();
        result =
            new UniformMutation(
                mutationProbability,
                perturbation,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "nonUniform" -> {
        Double perturbation =
            (Double) findSpecificSubParameter("nonUniformMutationPerturbation").value();
        int maxIterations = (Integer) nonConfigurableSubParameters().get("maxIterations");
        result =
            new NonUniformMutation(
                mutationProbability,
                perturbation,
                maxIterations,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      default -> throw new JMetalException("Mutation operator does not exist: " + name());
    }
    return result;
  }
}
