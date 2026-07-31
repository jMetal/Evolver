package org.uma.evolver.algorithm.nsgaiii;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable NSGA-III implementation for continuous (real-valued) optimization problems.
 *
 * <p>Extends {@link BaseNSGAIII} with the wiring needed for {@link DoubleSolution} encodings:
 * mutation parameters that depend on the number of problem variables and, for non-uniform
 * mutation, the maximum number of iterations derived from the evaluation budget.
 *
 * @see BaseNSGAIII
 */
public class DoubleNSGAIII extends BaseNSGAIII<DoubleSolution> {

  public DoubleNSGAIII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  public DoubleNSGAIII(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  /**
   * Constructs an instance using an explicit list of reference points (e.g. read from a
   * weight-vector file, the same convention used for MOEA/D and RVEA) instead of Das-Dennis
   * generation.
   */
  public DoubleNSGAIII(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      List<double[]> referencePoints) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace, referencePoints);
  }

  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoubleNSGAIII(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        parameterSpace.createInstance(),
        explicitReferencePoints);
  }

  @Override
  protected void setNonConfigurableParameters() {
    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    Check.that(
        maximumNumberOfEvaluations > 0, "Maximum number of evaluations must be greater than 0");
    Check.that(populationSize > 0, "Population size must be greater than 0");
    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }
  }
}
