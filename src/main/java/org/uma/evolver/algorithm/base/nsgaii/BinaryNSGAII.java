package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * A configurable implementation of the Non-dominated Sorting Genetic Algorithm II (NSGA-II) 
 * specifically designed for binary-encoded optimization problems.
 *
 * <p>This class extends the base {@link BaseNSGAII} implementation to handle binary-encoded
 * solutions, providing specialized support for binary-specific operations and parameters.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Support for binary-encoded solution spaces
 *   <li>Configurable genetic operators through the parameter space
 *   <li>Automatic handling of solution encoding/decoding
 *   <li>Integration with JMetal's binary problem interface
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Create a binary problem instance
 * BinaryProblem problem = new MyBinaryProblem();
 * 
 * // Configure the algorithm
 * int populationSize = 100;
 * int maxEvaluations = 25000;
 * ParameterSpace parameterSpace = new ParameterSpace();
 * // Configure parameter space with desired operators and parameters
 * 
 * // Create and run the algorithm
 * BinaryNSGAII algorithm = new BinaryNSGAII(problem, populationSize, maxEvaluations, parameterSpace);
 * algorithm.run();
 * 
 * // Get results
 * List<BinarySolution> population = algorithm.result();
 * }</pre>
 *
 * <p>The algorithm automatically configures non-configurable parameters such as the number of bits
 * in the solution based on the problem definition. For binary problems, the total number of bits
 * is automatically determined from the problem instance.
 *
 * @see BaseNSGAII
 * @see BinaryProblem
 * @see BinarySolution
 * @see MutationParameter
 * @author Your Name (your.email@example.com)
 * @since version
 */
public class BinaryNSGAII extends BaseNSGAII<BinarySolution> {
  /**
   * Constructs a new instance of BinaryNSGAII with the specified population size and parameter space.
   * 
   * <p>Note: This creates a partially configured instance. The {@link #createInstance(Problem, int)} 
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population to be used in the algorithm. Must be positive.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive or parameterSpace is null
   */
  public BinaryNSGAII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs a fully configured BinaryNSGAII instance ready for execution.
   *
   * @param problem the binary problem to be solved. Must implement the BinaryProblem interface.
   * @param populationSize the size of the population. Must be a positive integer.
   * @param maximumNumberOfEvaluations the evaluation budget for the algorithm. The algorithm will
   *                                 terminate once this number of evaluations is reached.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws ClassCastException if the provided problem does not implement BinaryProblem
   */
  public BinaryNSGAII(
      Problem<BinarySolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  /**
   * Creates and returns a new instance of BinaryNSGAII configured for the specified problem.
   * 
   * <p>This method implements the factory method pattern, allowing the creation of algorithm
   * instances with the same configuration but potentially different problems or evaluation limits.
   *
   * @param problem the binary optimization problem to solve. Must not be null.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the new instance.
   *                                 Must be positive.
   * @return a new, fully configured instance of BinaryNSGAII
   * @throws IllegalArgumentException if problem is null or maximumNumberOfEvaluations is not positive
   */
  @Override
  public BaseLevelAlgorithm<BinarySolution> createInstance(
      Problem<BinarySolution> problem, int maximumNumberOfEvaluations) {
    return new BinaryNSGAII(problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Configures non-configurable parameters based on the problem's characteristics.
   * 
   * <p>This method is automatically called during algorithm initialization to set up parameters
   * that depend on the specific problem instance, such as the total number of bits in the solution.
   * It configures mutation parameters based on the problem's binary encoding.
   * 
   * @implNote This method is part of the template method pattern and should not be called directly.
   * It is automatically invoked by the framework during algorithm initialization.
   */
  @Override
  protected void setNonConfigurableParameters() {
    int numberOfBitsInASolution = ((BinaryProblem)problem).totalNumberOfBits() ;

    var mutationParameter = (MutationParameter<BinarySolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
            "numberOfBitsInASolution", numberOfBitsInASolution);
  }
}
