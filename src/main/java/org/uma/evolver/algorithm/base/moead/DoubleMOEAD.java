package org.uma.evolver.algorithm.base.moead;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADDoubleParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.AggregationFunctionParameter;
import org.uma.evolver.parameter.catalogue.SequenceGeneratorParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Configurable implementation of the MOEA/D algorithm for real-coded (double) problems.
 *
 * <p>This class provides a customizable version of MOEA/D specifically designed for continuous
 * optimization problems with real-valued decision variables. It extends the base {@link BaseMOEAD}
 * class with double-specific configurations and operations.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Support for real-valued decision variables</li>
 *   <li>Configurable crossover and mutation operators for real-coded optimization</li>
 *   <li>Customizable weight vector generation and neighborhood structures</li>
 *   <li>Support for various aggregation functions</li>
 *   <li>Configurable termination conditions and evaluation strategies</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create and configure the algorithm
 * ParameterSpace parameterSpace = new MOEADDoubleParameterSpace();
 * DoubleMOEAD algorithm = new DoubleMOEAD(100, "weightVectors/", parameterSpace);
 * 
 * // Parse command line arguments to override default parameters
 * algorithm.parse(args);
 * 
 * // Build and run the algorithm
 * EvolutionaryAlgorithm<DoubleSolution> moead = algorithm.build();
 * moead.run();
 * 
 * // Get the resulting non-dominated solutions
 * List<DoubleSolution> result = moead.result();
 * }</pre>
 *
 * <h2>Configuration Parameters</h2>
 * <p>The algorithm can be configured using the following parameters in the parameter space:
 * <ul>
 *   <li>{@code maximumNumberOfReplacedSolutions}: Controls how many solutions are replaced in the neighborhood</li>
 *   <li>{@code aggregationFunction}: The aggregation function used to combine objectives</li>
 *   <li>{@code sequenceGenerator}: The sequence generation strategy for weight vectors</li>
 *   <li>And other parameters inherited from {@link BaseMOEAD}</li>
 * </ul>
 *
 * <p>Non-configurable parameters, such as the number of problem variables, maximum number of
 * replaced solutions, and derived values for mutation or neighborhood, are set automatically based
 * on the problem and algorithm configuration.
 *
 * @see BaseMOEAD
 * @see MOEADDoubleParameterSpace
 * @see MutationParameter
 * @see org.uma.jmetal.solution.doublesolution.DoubleSolution
 */
public class DoubleMOEAD extends BaseMOEAD<DoubleSolution> {
  /**
   * Constructs a new DoubleMOEAD instance with the specified population size, weight vector files directory,
   * and parameter space.
   *
   * <p>This creates a partially configured instance. The {@link #createInstance(Problem, int)}
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population to be used in the algorithm. Must be positive.
   * @param weightVectorFilesDirectory the directory containing weight vector files. Must not be null.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive or any parameter is null
   */
  public DoubleMOEAD(int populationSize, String weightVectorFilesDirectory, ParameterSpace parameterSpace) {
    super(populationSize, weightVectorFilesDirectory, parameterSpace);
  }

  /**
   * Constructs a fully configured DoubleMOEAD instance ready for execution.
   *
   * <p>This constructor creates a fully configured instance that can be immediately built and executed.
   * All parameters are validated before the instance is created.
   *
   * @param problem the optimization problem to be solved. Must not be null.
   * @param populationSize the size of the population. Must be positive.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the algorithm.
   *                                 Must be positive and greater than populationSize.
   * @param weightVectorFilesDirectory the directory containing weight vector files. Must not be null.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws IllegalStateException if the weight vector files cannot be found or parsed
   */
  public DoubleMOEAD(
      Problem<DoubleSolution> problem,
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
   * Creates a new instance of DoubleMOEAD for the given problem and maximum number of evaluations.
   *
   * <p>This factory method creates a new instance of DoubleMOEAD with the same configuration as this
   * instance but with the specified problem and evaluation budget. The parameter space is cloned
   * to ensure independent configuration.
   *
   * @param problem the problem to solve. Must not be null.
   * @param maximumNumberOfEvaluations the evaluation budget. Must be positive and greater than population size.
   * @return a new configured instance of DoubleMOEAD
   * @throws IllegalArgumentException if the problem is null or maximumNumberOfEvaluations is invalid
   * @throws IllegalStateException if the parameter space cannot be cloned
   */
  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoubleMOEAD(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        weightVectorFilesDirectory,
        parameterSpace().createInstance());
  }

  /**
   * Sets non-configurable parameters that depend on the problem or algorithm configuration.
   *
   * <p>This method is automatically called during the build process to configure parameters that
   * cannot be set through the parameter space because they depend on the problem instance or
   * other runtime factors.
   *
   * <p>This implementation configures:
   * <ul>
   *   <li>The maximum number of solutions to replace in each neighborhood update
   *   <li>The aggregation function for combining objectives
   *   <li>Objective normalization settings
   *   <li>Sequence generation parameters
   *   <li>Problem-specific parameters for real-valued optimization
   * </ul>
   *
   * @throws IllegalStateException if required parameters are missing or invalid
   */
  @Override
  protected void setNonConfigurableParameters() {
    // Configure parameters specific to real-coded MOEA/D implementation
    ParameterSpace parameterSpace = parameterSpace();

    maximumNumberOfReplacedSolutions =
        (int) parameterSpace.get("maximumNumberOfReplacedSolutions").value();

    aggregationFunction =
        ((AggregationFunctionParameter) parameterSpace.get("aggregationFunction"))
            .getAggregationFunction();

    normalizedObjectives =
        ((String) parameterSpace.get("normalizeObjectives").value()).equalsIgnoreCase("true");

    SequenceGeneratorParameter subProblemIdGeneratorParameter =
        (SequenceGeneratorParameter) parameterSpace.get("subProblemIdGenerator");
    subProblemIdGeneratorParameter.sequenceLength(populationSize);
    subProblemIdGenerator = subProblemIdGeneratorParameter.getSequenceGenerator();

    neighborhood = getNeighborhood();

    parameterSpace
        .get("selection")
        .addNonConfigurableSubParameter("neighborhood", neighborhood)
        .addNonConfigurableSubParameter("subProblemIdGenerator", subProblemIdGenerator);

    MutationParameter mutationParameter = (MutationParameter) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }

    if (mutationParameter.value().equals("uniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "uniformMutationPerturbation", parameterSpace.get("uniformMutationPerturbation"));
    }

    if (parameterSpace.get("variation").value().equals("crossoverAndMutationVariation")) {
      parameterSpace.get("variation").addNonConfigurableSubParameter("offspringPopulationSize", 1);
    }
  }

  /**
   * Creates and configures the variation operator for real-coded solutions.
   * 
   * <p>This method creates the variation operator (typically a combination of crossover and mutation)
   * that will be used to generate new solutions. The specific operators are retrieved from the
   * parameter space configuration.
   *
   * <p>The variation operator is responsible for creating new candidate solutions by combining
   * and modifying existing solutions from the population. For real-coded problems, this typically
   * involves polynomial mutation and SBX (Simulated Binary Crossover) or differential evolution.
   *
   * @return a configured variation operator for real-coded solutions
   * @throws IllegalStateException if the variation operator cannot be created or configured
   * @see org.uma.jmetal.operator.variation.Variation
   */
  @Override
  @SuppressWarnings("unchecked")
  protected Variation<DoubleSolution> createVariation() {
    return ((VariationParameter<DoubleSolution>) parameterSpace.get("variation"))
        .getVariation();
  }
}
