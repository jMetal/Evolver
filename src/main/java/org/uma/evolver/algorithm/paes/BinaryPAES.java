package org.uma.evolver.algorithm.paes;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Configurable PAES implementation for binary-encoded problems.
 *
 * <p>Extends {@link BasePAES} with the wiring needed for {@link BinarySolution} encodings:
 * the total number of bits is injected into the bit-flip mutation parameter so that the
 * actual mutation probability is computed as {@code mutationProbabilityFactor / numberOfBits}.
 *
 * @see BasePAES
 */
public class BinaryPAES extends BasePAES<BinarySolution> {

  public BinaryPAES(int numberOfSolutionsToFind, ParameterSpace parameterSpace) {
    super(numberOfSolutionsToFind, parameterSpace);
  }

  public BinaryPAES(
      Problem<BinarySolution> problem,
      int numberOfSolutionsToFind,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, numberOfSolutionsToFind, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public BaseLevelAlgorithm<BinarySolution> createInstance(
      Problem<BinarySolution> problem, int maximumNumberOfEvaluations) {
    return new BinaryPAES(
        problem, numberOfSolutionsToFind, maximumNumberOfEvaluations,
        parameterSpace.createInstance());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    int numberOfBitsInASolution = ((BinaryProblem) problem).totalNumberOfBits();
    var mutationParameter = (MutationParameter<BinarySolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter("numberOfBitsInASolution", numberOfBitsInASolution);
  }
}
