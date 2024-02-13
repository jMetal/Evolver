package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;


public class MutationParameter extends CategoricalParameter {

  public MutationParameter(List<String> mutationOperators) {
    super("mutation", mutationOperators);
  }

  public MutationOperator<DoubleSolution> getParameter() {
    MutationOperator<DoubleSolution> result;
    int numberOfProblemVariables = (int) getNonConfigurableParameter("numberOfProblemVariables");
    double mutationProbability = (double) findGlobalParameter(
        "mutationProbabilityFactor").value() / numberOfProblemVariables;
    RepairDoubleSolutionStrategyParameter repairDoubleSolution =
        (RepairDoubleSolutionStrategyParameter) findGlobalParameter("mutationRepairStrategy");

    switch (value()) {
      case "polynomial":
        Double distributionIndex =
            (Double) findSpecificParameter("polynomialMutationDistributionIndex").value();
        result =
            new PolynomialMutation(
                mutationProbability, distributionIndex, repairDoubleSolution.getParameter());
        break;
      case "linkedPolynomial":
        distributionIndex =
            (Double) findSpecificParameter("linkedPolynomialMutationDistributionIndex").value();
        result =
            new LinkedPolynomialMutation(
                mutationProbability, distributionIndex, repairDoubleSolution.getParameter());
        break;
      case "uniform":
        Double perturbation = (Double) findSpecificParameter(
            "uniformMutationPerturbation").value();
        result =
            new UniformMutation(mutationProbability, perturbation,
                repairDoubleSolution.getParameter());
        break;
      case "nonUniform":
        perturbation = (Double) findSpecificParameter("nonUniformMutationPerturbation").value();
        int maxIterations = (Integer) getNonConfigurableParameter("maxIterations");
        result =
            new NonUniformMutation(mutationProbability, perturbation, maxIterations,
                repairDoubleSolution.getParameter());
        break;
      case "multiMutation":
        Double nonUniPerturbation = (Double) findSpecificParameter("nonUniformMutationPerturbation").value();
        Double uniPerturbation = (Double) findSpecificParameter("uniformMutationPerturbation").value();
        int nonUniMaxIterations = (Integer) getNonConfigurableParameter("maxIterations");
        Double polyDistributionIndex =
                (Double) findSpecificParameter("polynomialMutationDistributionIndex").value();
        Double linkedPolyDistributionIndex =
                (Double) findSpecificParameter("linkedPolynomialMutationDistributionIndex").value();

        Double polyMutationProbability = (Double) findSpecificParameter("polyMutationProbability").value();
        Double linkedPolyMutationProbability = (Double) findSpecificParameter("linkedPolyMutationProbability").value();
        Double uniMutationProbability = (Double) findSpecificParameter("uniMutationProbability").value();
        Double nonUniMutationProbability = (Double) findSpecificParameter("nonUniMutationProbability").value();

        result = new MultiMutation(mutationProbability, repairDoubleSolution.getParameter(), polyMutationProbability,linkedPolyMutationProbability, uniMutationProbability, nonUniMutationProbability,
                uniPerturbation, polyDistributionIndex, linkedPolyDistributionIndex, nonUniPerturbation, nonUniMaxIterations);
        break;
      default:
        throw new JMetalException("Mutation operator does not exist: " + name());
    }
    return result;
  }

  @Override
  public String name() {
    return "mutation";
  }
}

