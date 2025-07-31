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
   * Constructs an NSGAIIBinary instance with the given population size and parameter space. This is a partial instance that is 
   * not fully usable until the createMethod() is called
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public BinaryNSGAII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs an NSGAIIBinary instance with the given problem, population size, and maximum
   * number of evaluations. Uses a default parameter space.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
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
   * Creates a new instance of NSGAIIBinary for the given problem and maximum number of
   * evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of NSGAIIBinary
   */
  @Override
  public BaseLevelAlgorithm<BinarySolution> createInstance(
      Problem<BinarySolution> problem, int maximumNumberOfEvaluations) {
    return new BinaryNSGAII(problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /** Sets non-configurable parameters that depend on the problem or algorithm configuration. */
  @Override
  protected void setNonConfigurableParameters() {
    int numberOfBitsInASolution = ((BinaryProblem)problem).totalNumberOfBits() ;

    var mutationParameter = (MutationParameter<BinarySolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
            "numberOfBitsInASolution", numberOfBitsInASolution);

  }
}
