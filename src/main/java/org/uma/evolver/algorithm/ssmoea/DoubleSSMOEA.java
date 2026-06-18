package org.uma.evolver.algorithm.ssmoea;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Configurable Steady-State MOEA for real-valued (double-encoded) problems.
 *
 * <p>The offspring population size is fixed to 1. Variation can be either
 * {@code crossoverAndMutationVariation} (with {@code gaSelection}) or
 * {@code differentialEvolutionVariation} (with {@code DifferentialEvolutionSelection}).
 * Replacement can be {@code rankingAndDensityEstimator} or {@code singleSolutionReplacement}.
 */
public class DoubleSSMOEA extends BaseSSMOEA<DoubleSolution> {

  public DoubleSSMOEA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  public DoubleSSMOEA(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoubleSSMOEA(problem, populationSize, maximumNumberOfEvaluations,
        parameterSpace.createInstance());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    MutationParameter<DoubleSolution> mutationParameter =
        (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter("numberOfProblemVariables",
        problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter("maxIterations",
          maximumNumberOfEvaluations / populationSize);
    }

    if (mutationParameter.value().equals("uniform")) {
      mutationParameter.addNonConfigurableSubParameter("perturbationValue",
          parameterSpace.get("uniformMutationPerturbation"));
    }
  }
}
