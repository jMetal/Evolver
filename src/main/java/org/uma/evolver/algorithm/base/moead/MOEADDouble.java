package org.uma.evolver.algorithm.base.moead;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.evolver.parameter.catalogue.AggregationFunctionParameter;
import org.uma.evolver.parameter.catalogue.SequenceGeneratorParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Configurable implementation of the MOEA/D algorithm for real-coded (double) problems.
 *
 * <p>This class provides a customizable version of MOEA/D, supporting various aggregation
 * functions, neighborhood strategies, crossover and mutation operators, and archive configurations
 * for double-valued problems.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * MOEADDouble algorithm = new MOEADDouble(problem, 100, 25000, "weightsVectorDirectory");
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<DoubleSolution> moead = algorithm.build();
 * moead.run();
 * }</pre>
 *
 * <p>Non-configurable parameters, such as the number of problem variables, maximum number of
 * replaced solutions, and derived values for mutation or neighborhood, are set automatically based
 * on the problem and algorithm configuration.
 *
 * @see
 *     MOEADDoubleParameterSpace
 * @see org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter
 */
public class MOEADDouble extends AbstractMOEAD<DoubleSolution> {
  /**
   * Constructs a MOEADDouble instance with the given population size and a default parameter space.
   *
   * @param populationSize the population size to use
   */
  public MOEADDouble(int populationSize) {
    super(populationSize, new MOEADDoubleParameterSpace());
  }

  /**
   * Constructs a MOEADDouble instance with the given problem, population size, maximum number of
   * evaluations, weight vector files directory, and a default parameter space.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param weightVectorFilesDirectory the directory containing weight vector files
   */
  public MOEADDouble(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory) {
    super(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        weightVectorFilesDirectory,
        new MOEADDoubleParameterSpace());
  }

  /**
   * Creates a new instance of MOEADDouble for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of MOEADDouble
   */
  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new MOEADDouble(
        problem, populationSize, maximumNumberOfEvaluations, weightVectorFilesDirectory);
  }

  /**
   * Sets non-configurable parameters that depend on the problem or algorithm configuration.
   *
   * <p>This method automatically sets:
   *
   * <ul>
   *   <li>The number of problem variables for the mutation operator.
   *   <li>The maximum number of replaced solutions and neighborhood configuration.
   *   <li>Aggregation function and normalization settings.
   *   <li>Sequence generator and other derived parameters.
   * </ul>
   */
  @Override
  protected void setNonConfigurableParameters() {
    // Set any additional non-configurable parameters specific to MOEADDouble here
    MOEADDoubleParameterSpace parameterSpace = (MOEADDoubleParameterSpace) parameterSpace();

    maximumNumberOfReplacedSolutions =
        (int) parameterSpace.get(parameterSpace.MAXIMUM_NUMBER_OF_REPLACED_SOLUTIONS).value();

    aggregationFunction =
        ((AggregationFunctionParameter) parameterSpace.get(parameterSpace.AGGREGATION_FUNCTION))
            .getAggregationFunction();

    normalizedObjectives =
        (boolean) parameterSpace.get(parameterSpace.NORMALIZE_OBJECTIVES).value();

    SequenceGeneratorParameter subProblemIdGeneratorParameter =
        (SequenceGeneratorParameter) parameterSpace.get(parameterSpace.SUB_PROBLEM_ID_GENERATOR);
    subProblemIdGeneratorParameter.sequenceLength(populationSize);
    subProblemIdGenerator = subProblemIdGeneratorParameter.getSequenceGenerator();

    neighborhood = getNeighborhood();

    parameterSpace
        .get(parameterSpace.SELECTION)
        .addNonConfigurableSubParameter("neighborhood", neighborhood)
        .addNonConfigurableSubParameter(
            parameterSpace.SUB_PROBLEM_ID_GENERATOR, subProblemIdGenerator);

    MutationParameter mutationParameter =
        (MutationParameter) parameterSpace.get(parameterSpace.MUTATION);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    if (mutationParameter.value().equals(parameterSpace.NON_UNIFORM)) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }

    if (mutationParameter.value().equals(parameterSpace.UNIFORM)) {
      mutationParameter.addNonConfigurableSubParameter(
          parameterSpace.UNIFORM_MUTATION_PERTURBATION,
          parameterSpace.get(parameterSpace.UNIFORM_MUTATION_PERTURBATION));
    }

    if (parameterSpace
        .get(parameterSpace.VARIATION)
        .value()
        .equals(parameterSpace.CROSSOVER_AND_MUTATION_VARIATION)) {
      parameterSpace
          .get(parameterSpace.VARIATION)
          .addNonConfigurableSubParameter("offspringPopulationSize", 1);
    }
  }
}
