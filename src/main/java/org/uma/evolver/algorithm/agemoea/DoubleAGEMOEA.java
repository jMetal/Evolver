package org.uma.evolver.algorithm.agemoea;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable AGE-MOEA implementation for continuous (real-valued) optimization problems.
 *
 * <p>Extends {@link BaseAGEMOEA} with the wiring needed for {@link DoubleSolution} encodings:
 * mutation parameters that depend on the number of problem variables and, for non-uniform mutation,
 * the maximum number of iterations derived from the evaluation budget.
 *
 * @see BaseAGEMOEA
 */
public class DoubleAGEMOEA extends BaseAGEMOEA<DoubleSolution> {

  public DoubleAGEMOEA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  public DoubleAGEMOEA(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoubleAGEMOEA(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  @Override
  protected void setNonConfigurableParameters() {
    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    Check.that(maximumNumberOfEvaluations > 0, "Maximum number of evaluations must be greater than 0");
    Check.that(populationSize > 0, "Population size must be greater than 0");
    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }
  }
}
