package org.uma.evolver.algorithm.base.moead;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.AggregationFunctionParameter;
import org.uma.evolver.parameter.catalogue.SequenceGeneratorParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Configurable implementation of the MOEA/D algorithm for binary-based problems.
 *
 * <p>This class provides a customizable version of MOEA/D, supporting various aggregation
 * functions, neighborhood strategies, crossover and mutation operators, and archive configurations
 * for permutation-valued problems.
 *
 * <p>Typical usage:
 * <pre>{@code
 * MOEADBinary algorithm = new MOEADBinary(problem, 100, 25000, "weightsVectorDirectory");
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<BinarySolution> moead = algorithm.build();
 * moead.run();
 * }</pre>
 *
 * <p>Non-configurable parameters, such as the number of problem variables, maximum number of
 * replaced solutions, and derived values for mutation or neighborhood, are set automatically based
 * on the problem and algorithm configuration.
 *
 * @see org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter
 */
public class BinaryMOEAD extends BaseMOEAD<BinarySolution> {
  /**
   * Constructs a MOEADDouble instance with the given population size and a default parameter space.
   *
   * @param populationSize the population size to use
   */
  public BinaryMOEAD(int populationSize, String weightVectorFilesDirectory, ParameterSpace parameterSpace) {
    super(populationSize, weightVectorFilesDirectory, parameterSpace);
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
  public BinaryMOEAD(
      Problem<BinarySolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory,
      ParameterSpace parameterSpace) {
    super(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        weightVectorFilesDirectory,
        parameterSpace);
  }

  /**
   * Creates a new instance of MOEADDouble for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of MOEADDouble
   */
  @Override
  public BaseLevelAlgorithm<BinarySolution> createInstance(
      Problem<BinarySolution> problem, int maximumNumberOfEvaluations) {
    return new BinaryMOEAD(
        problem, populationSize, maximumNumberOfEvaluations, weightVectorFilesDirectory, parameterSpace().createInstance());
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
    ParameterSpace parameterSpace = parameterSpace();

    maximumNumberOfReplacedSolutions =
            (int) parameterSpace.get("maximumNumberOfReplacedSolutions").value();

    aggregationFunction =
            ((AggregationFunctionParameter) parameterSpace.get("aggregationFunction")).getAggregationFunction();

    normalizedObjectives =
            ((String)parameterSpace.get("normalizeObjectives").value()).equalsIgnoreCase("true");

    SequenceGeneratorParameter subProblemIdGeneratorParameter =
            (SequenceGeneratorParameter) parameterSpace.get("subProblemIdGenerator");
    subProblemIdGeneratorParameter.sequenceLength(populationSize);
    subProblemIdGenerator = subProblemIdGeneratorParameter.getSequenceGenerator();

    neighborhood = getNeighborhood();

    parameterSpace
            .get("selection")
            .addNonConfigurableSubParameter("neighborhood", neighborhood)
            .addNonConfigurableSubParameter(
                    "subProblemIdGenerator", subProblemIdGenerator);

    parameterSpace.get("variation").addNonConfigurableSubParameter("offspringPopulationSize", 1);

    parameterSpace.get("mutation").addNonConfigurableSubParameter("numberOfBitsInASolution", ((BinaryProblem)problem).totalNumberOfBits());

    if (parameterSpace
        .get("variation")
        .value()
        .equals("crossoverAndMutationVariation")) {
      parameterSpace
          .get("variation")
          .addNonConfigurableSubParameter("offspringPopulationSize", 1);
    }
  }
}
