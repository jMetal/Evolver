package org.uma.evolver.algorithm.paes;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable PAES implementation for real-valued (double-encoded) problems.
 *
 * <p>Extends {@link BasePAES} with the wiring needed for {@link DoubleSolution} encodings:
 * mutation parameters that depend on the number of problem variables and, for non-uniform mutation,
 * the maximum number of iterations derived from the evaluation budget.
 *
 * @see BasePAES
 */
public class DoublePAES extends BasePAES<DoubleSolution> {

  public DoublePAES(int numberOfSolutionsToFind, ParameterSpace parameterSpace) {
    super(numberOfSolutionsToFind, parameterSpace);
  }

  public DoublePAES(
      Problem<DoubleSolution> problem,
      int numberOfSolutionsToFind,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, numberOfSolutionsToFind, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoublePAES(
        problem, numberOfSolutionsToFind, maximumNumberOfEvaluations,
        parameterSpace.createInstance());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    Check.notNull(problem);
    Check.that(maximumNumberOfEvaluations > 0,
        "Maximum number of evaluations must be greater than 0");

    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      // population size is 1 for PAES
      mutationParameter.addNonConfigurableSubParameter("maxIterations", maximumNumberOfEvaluations);
    }
  }
}
