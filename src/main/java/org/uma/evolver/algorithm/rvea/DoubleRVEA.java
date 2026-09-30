package org.uma.evolver.algorithm.rvea;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable RVEA, RVEA* and iRVEA for continuous (real-valued) optimization problems.
 *
 * <p>Extends {@link BaseRVEA} for {@link DoubleSolution}; the variant, selection, variation and
 * initialisation are drawn from a {@link ParameterSpace} (e.g. {@code RVEADouble.yaml}). An
 * optional external archive can be enabled via the {@code algorithmResult} parameter.
 *
 * <p>The reference vectors are given as a list, or as a directory of weight vector files read for
 * each problem; in both cases their number is the population size.
 *
 * @see BaseRVEA
 */
public class DoubleRVEA extends BaseRVEA<DoubleSolution> {

  /**
   * Creates an RVEA for a problem with a given list of reference vectors.
   *
   * @param problem the problem to solve (unconstrained)
   * @param populationSize the population size, equal to the number of reference vectors
   * @param maximumNumberOfEvaluations the evaluation budget
   * @param parameterSpace the parameter space
   * @param referenceVectors the reference vectors
   */
  public DoubleRVEA(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      List<double[]> referenceVectors) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace, referenceVectors);
  }

  /**
   * Creates an RVEA whose reference vectors are read, for each problem, from a directory of weight
   * vector files ({@code W<objectives>D_<populationSize>.dat}), as in MOEA/D. The problem and the
   * budget are given through {@link #createInstance}; this is the form a meta-optimizer uses.
   *
   * @param populationSize the population size, equal to the number of vectors of each file
   * @param weightVectorFilesDirectory the directory of the weight vector files
   * @param parameterSpace the parameter space
   */
  public DoubleRVEA(
      int populationSize, String weightVectorFilesDirectory, ParameterSpace parameterSpace) {
    super(populationSize, weightVectorFilesDirectory, parameterSpace);
  }

  /**
   * Creates an RVEA for a problem, with its reference vectors read from a directory of weight
   * vector files.
   */
  public DoubleRVEA(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory,
      ParameterSpace parameterSpace) {
    super(
        problem, populationSize, maximumNumberOfEvaluations, weightVectorFilesDirectory,
        parameterSpace);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());
    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }
  }

  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return referenceVectors != null
        ? new DoubleRVEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            parameterSpace.createInstance(),
            referenceVectors)
        : new DoubleRVEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            weightVectorFilesDirectory,
            parameterSpace.createInstance());
  }
}
