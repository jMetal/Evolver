package org.uma.evolver.algorithm.smsemoa;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Configurable implementation of the SMS-EMOA (S-Metric Selection Evolutionary Multi-Objective Algorithm)
 * for permutation-based problems.
 *
 * <p>This class provides a highly customizable version of SMS-EMOA, supporting:
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)</li>
 *   <li>Multiple crossover operators for permutations (e.g., PMX, OX, CX)</li>
 *   <li>Different mutation approaches for permutations (e.g., swap, insertion, inversion)</li>
 *   <li>Optional external archive integration</li>
 *   <li>Hypervolume-based selection for maintaining diversity</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * PermutationSMSEMOA algorithm = new PermutationSMSEMOA(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<PermutationSolution<Integer>> smsemoa = algorithm.build();
 * smsemoa.run();
 * }</pre>
 *
 * <p>Non-configurable parameters such as the permutation length are set automatically based on the
 * problem configuration.
 *
 * @see BaseSMSEMOA
 * @see MutationParameter
 * @see <a href="https://doi.org/10.1007/11732242_32">SMS-EMOA: Multiobjective selection based on dominated hypervolume</a>
 */
public class PermutationSMSEMOA extends BaseSMSEMOA<PermutationSolution<Integer>> {

  /**
   * Constructs a PermutationSMSEMOA instance with the given population size and parameter space.
   * <p>
   * This constructor is typically used when the permutation problem and maximum number of evaluations
   * will be set later via the createInstance method.
   *
   * @param populationSize the population size to use (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize is less than or equal to 0
   */
  public PermutationSMSEMOA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a PermutationSMSEMOA instance with the given permutation problem, population size, 
   * maximum number of evaluations, and parameter space.
   *
   * @param problem the permutation problem to solve (must not be null)
   * @param populationSize the population size to use (must be > 0)
   * @param maximumNumberOfEvaluations the maximum number of evaluations (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize or maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem or parameterSpace is null
   */
  public PermutationSMSEMOA(
      Problem<PermutationSolution<Integer>> problem, int populationSize, int maximumNumberOfEvaluations, ParameterSpace parameterSpace) {
   super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace) ;
  }

  /**
   * Creates a new instance of PermutationSMSEMOA for the given permutation problem and maximum number of evaluations.
   * <p>
   * This method creates a new instance using the current configuration but with the specified
   * permutation problem and evaluation budget. The parameter space is cloned to ensure independent instances.
   *
   * @param problem the permutation problem to solve (must not be null)
   * @param maximumNumberOfEvaluations the evaluation budget (must be > 0)
   * @return a new configured instance of PermutationSMSEMOA
   * @throws IllegalArgumentException if maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem is null
   */
  @Override
  public synchronized BaseLevelAlgorithm<PermutationSolution<Integer>> createInstance(
      Problem<PermutationSolution<Integer>> problem, int maximumNumberOfEvaluations) {

    return new PermutationSMSEMOA(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Configures non-configurable parameters that depend on the permutation problem or algorithm configuration.
   * <p>
   * This method is automatically called during the build process and sets up various
   * parameters that are specific to permutation problems and cannot be directly configured
   * through the parameter space. For permutation problems, most parameters are handled
   * by the base class implementation, so this method is intentionally left empty.
   * <p>
   * Subclasses can override this method to set up additional problem-specific parameters.
   *
   * @throws IllegalStateException if the required parameters are not properly configured
   * @throws NullPointerException if the problem or required parameters are not set
   */
  @Override
  protected void setNonConfigurableParameters() {
    // No non-configurable parameters need to be set for the permutation variant
  }
}
