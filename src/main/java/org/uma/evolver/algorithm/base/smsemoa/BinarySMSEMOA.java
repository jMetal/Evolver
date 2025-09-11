package org.uma.evolver.algorithm.base.smsemoa;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Configurable implementation of the SMS-EMOA (S-Metric Selection Evolutionary Multi-Objective Algorithm)
 * for binary-encoded problems.
 *
 * <p>This class provides a highly customizable version of SMS-EMOA, supporting:
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)</li>
 *   <li>Multiple crossover operators for binary strings (e.g., HUX, uniform, single-point)</li>
 *   <li>Bit-flip mutation for binary solutions</li>
 *   <li>Optional external archive integration</li>
 *   <li>Hypervolume-based selection for maintaining diversity</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * BinarySMSEMOA algorithm = new BinarySMSEMOA(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<BinarySolution> smsemoa = algorithm.build();
 * smsemoa.run();
 * }</pre>
 *
 * <p>Non-configurable parameters such as the number of bits per variable are set automatically based on the
 * problem configuration.
 *
 * @see BaseSMSEMOA
 * @see MutationParameter
 * @see <a href="https://doi.org/10.1007/11732242_32">SMS-EMOA: Multiobjective selection based on dominated hypervolume</a>
 */
public class BinarySMSEMOA extends BaseSMSEMOA<BinarySolution> {

  /**
   * Constructs a BinarySMSEMOA instance with the given population size and parameter space.
   * <p>
   * This constructor is typically used when the problem and maximum number of evaluations
   * will be set later via the createInstance method.
   *
   * @param populationSize the population size to use (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize is less than or equal to 0
   */
  public BinarySMSEMOA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a BinarySMSEMOA instance with the given problem, population size, maximum number of evaluations,
   * and parameter space.
   *
   * @param problem the binary problem to solve (must not be null)
   * @param populationSize the population size to use (must be > 0)
   * @param maximumNumberOfEvaluations the maximum number of evaluations (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize or maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem or parameterSpace is null
   */
  public BinarySMSEMOA(
      Problem<BinarySolution> problem, int populationSize, int maximumNumberOfEvaluations, ParameterSpace parameterSpace) {
   super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace) ;
  }

  /**
   * Creates a new instance of BinarySMSEMOA for the given problem and maximum number of evaluations.
   * <p>
   * This method creates a new instance using the current configuration but with the specified
   * binary problem and evaluation budget. The parameter space is cloned to ensure independent instances.
   *
   * @param problem the binary problem to solve (must not be null)
   * @param maximumNumberOfEvaluations the evaluation budget (must be > 0)
   * @return a new configured instance of BinarySMSEMOA
   * @throws IllegalArgumentException if maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem is null
   */
  @Override
  public synchronized BaseLevelAlgorithm<BinarySolution> createInstance(
      Problem<BinarySolution> problem, int maximumNumberOfEvaluations) {

    return new BinarySMSEMOA(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Configures non-configurable parameters that depend on the problem or algorithm configuration.
   * <p>
   * This method is automatically called during the build process and sets up various
   * parameters that cannot be directly configured through the parameter space, including:
   * <ul>
   *   <li>The number of problem variables for the mutation operator</li>
   *   <li>The maximum number of iterations for non-uniform mutation (if used)</li>
   *   <li>The perturbation value for uniform mutation (if used)</li>
   *   <li>The offspring population size for variation operators</li>
   * </ul>
   *
   * @throws IllegalStateException if the required parameters are not properly configured
   * @throws NullPointerException if the problem or required parameters are not set
   */
  @Override
  protected void setNonConfigurableParameters() {
    int numberOfBitsInASolution = ((BinaryProblem)problem).totalNumberOfBits() ;

    var mutationParameter = (MutationParameter<BinarySolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
            "numberOfBitsInASolution", numberOfBitsInASolution);  
  }
}
