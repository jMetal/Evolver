package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A configurable implementation of the Non-dominated Sorting Genetic Algorithm II (NSGA-II) 
 * specifically designed for continuous (real-valued) optimization problems.
 *
 * <p>This class extends the base {@link BaseNSGAII} implementation to handle double-encoded
 * solutions, providing specialized support for continuous optimization problems with real-valued variables.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Support for real-valued solution spaces</li>
 *   <li>Configurable genetic operators through the parameter space</li>
 *   <li>Automatic handling of solution bounds and constraints</li>
 *   <li>Integration with JMetal's double solution interface</li>
 *   <li>Support for various crossover and mutation operators</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Create a continuous problem instance
 * DoubleProblem problem = new MyDoubleProblem();
 * 
 * // Configure the algorithm
 * int populationSize = 100;
 * int maxEvaluations = 25000;
 * ParameterSpace parameterSpace = new NSGAIIDoubleParameterSpace();
 * // Configure parameter space with desired operators and parameters
 * 
 * // Create and run the algorithm
 * DoubleNSGAII algorithm = new DoubleNSGAII(problem, populationSize, maxEvaluations, parameterSpace);
 * algorithm.run();
 * 
 * // Get results
 * List<DoubleSolution> population = algorithm.result();
 * }</pre>
 *
 * <p>The algorithm automatically configures non-configurable parameters such as the number of variables
 * and their bounds based on the problem definition. For mutation operators like non-uniform mutation,
 * it also configures the maximum number of iterations based on the evaluation budget.
 *
 * @see BaseNSGAII
 * @see DoubleSolution
 * @see NSGAIIDoubleParameterSpace
 * @see MutationParameter
 * @since version
 */
public class DoubleNSGAII extends BaseNSGAII<DoubleSolution> {

  /**
   * Constructs a new instance of DoubleNSGAII with the specified population size and parameter space.
   * 
   * <p>Note: This creates a partially configured instance. The {@link #createInstance(Problem, int)} 
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population to be used in the algorithm. Must be positive.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive or parameterSpace is null
   */
  public DoubleNSGAII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a fully configured DoubleNSGAII instance ready for execution.
   *
   * @param problem the continuous optimization problem to be solved. Must implement the DoubleProblem interface.
   * @param populationSize the size of the population. Must be a positive integer.
   * @param maximumNumberOfEvaluations the evaluation budget for the algorithm. The algorithm will
   *                                 terminate once this number of evaluations is reached.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws ClassCastException if the provided problem does not implement DoubleProblem
   */
  public DoubleNSGAII(
      Problem<DoubleSolution> problem, int populationSize, int maximumNumberOfEvaluations, ParameterSpace parameterSpace) {
   super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace) ;
  }

  /**
   * Creates and returns a new instance of DoubleNSGAII configured for the specified problem.
   * 
   * <p>This method implements the factory method pattern, allowing the creation of algorithm
   * instances with the same configuration but potentially different problems or evaluation limits.
   *
   * @param problem the continuous optimization problem to solve. Must not be null.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the new instance.
   *                                 Must be positive.
   * @return a new, fully configured instance of DoubleNSGAII
   * @throws IllegalArgumentException if problem is null or maximumNumberOfEvaluations is not positive
   */
  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
   
    return new DoubleNSGAII(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Configures non-configurable parameters based on the problem's characteristics.
   * 
   * <p>This method is automatically called during algorithm initialization to set up parameters
   * that depend on the specific problem instance. It handles:
   * <ul>
   *   <li>Number of problem variables for mutation operators</li>
   *   <li>Maximum number of iterations for non-uniform mutation</li>
   *   <li>Perturbation values for uniform mutation</li>
   * </ul>
   * 
   * @implNote This method is part of the template method pattern and should not be called directly.
   * It is automatically invoked by the framework during algorithm initialization.
   * @throws IllegalStateException if required parameters are not properly configured
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
  }
}
