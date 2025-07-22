package org.uma.evolver.algorithm.base.smsemoa;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable implementation of the SMS-EMOA (S-Metric Selection Evolutionary Multi-Objective Algorithm)
 * for double-valued (real-coded) problems.
 *
 * <p>This class provides a highly customizable version of SMS-EMOA, supporting:
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)</li>
 *   <li>Multiple crossover operators (e.g., SBX, BLX-Alpha, whole arithmetic)</li>
 *   <li>Different mutation approaches (e.g., uniform, polynomial, non-uniform)</li>
 *   <li>Optional external archive integration</li>
 *   <li>Hypervolume-based selection for maintaining diversity</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * DoubleSMSEMOA algorithm = new DoubleSMSEMOA(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<DoubleSolution> smsemoa = algorithm.build();
 * smsemoa.run();
 * }</pre>
 *
 * <p>Non-configurable parameters such as the number of problem variables and, depending on the mutation
 * operator, the maximum number of iterations or perturbation value, are set automatically based on the
 * problem and algorithm configuration.
 *
 * @see BaseSMSEMOA
 * @see MutationParameter
 * @see <a href="https://doi.org/10.1007/11732242_32">SMS-EMOA: Multiobjective selection based on dominated hypervolume</a>
 */
public class DoubleSMSEMOA extends BaseSMSEMOA<DoubleSolution> {

  /**
   * Constructs a DoubleSMSEMOA instance with the given population size and parameter space.
   * <p>
   * This constructor is typically used when the problem and maximum number of evaluations
   * will be set later via the createInstance method.
   *
   * @param populationSize the population size to use (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize is less than or equal to 0
   */
  public DoubleSMSEMOA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a DoubleSMSEMOA instance with the given problem, population size, maximum number of evaluations,
   * and parameter space.
   *
   * @param problem the problem to solve (must not be null)
   * @param populationSize the population size to use (must be > 0)
   * @param maximumNumberOfEvaluations the maximum number of evaluations (must be > 0)
   * @param parameterSpace the parameter space containing the algorithm configuration
   * @throws IllegalArgumentException if populationSize or maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem or parameterSpace is null
   */
  public DoubleSMSEMOA(
      Problem<DoubleSolution> problem, int populationSize, int maximumNumberOfEvaluations, ParameterSpace parameterSpace) {
   super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace) ;
  }

  /**
   * Creates a new instance of DoubleSMSEMOA for the given problem and maximum number of evaluations.
   * <p>
   * This method creates a new instance using the current configuration but with the specified
   * problem and evaluation budget. The parameter space is cloned to ensure independent instances.
   *
   * @param problem the problem to solve (must not be null)
   * @param maximumNumberOfEvaluations the evaluation budget (must be > 0)
   * @return a new configured instance of DoubleSMSEMOA
   * @throws IllegalArgumentException if maximumNumberOfEvaluations is less than or equal to 0
   * @throws NullPointerException if problem is null
   */
  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {

    return new DoubleSMSEMOA(
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

    if (mutationParameter.value().equals("uniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "uniformMutationPerturbation", parameterSpace.get("uniformMutationPerturbation"));
    }

    if (parameterSpace.get("variation").value().equals("crossoverAndMutationVariation")) {
      parameterSpace.get("variation").addNonConfigurableSubParameter("offspringPopulationSize", 1);
    }
  }
}
