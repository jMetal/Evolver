package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Configurable implementation of the NSGA-II algorithm for Binary-based problems.
 *
 * <p>This class provides a highly customizable version of NSGA-II, supporting:
 *
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)
 *   <li>Multiple crossover operators (e.g., PMX, CX, OX, etc.)
 *   <li>Different mutation approaches (e.g., swap, scramble, insertion, etc.)
 *   <li>Optional external archive integration
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * NSGAIIBinary algorithm = new NSGAIIBinary(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<BinarySolution> nsgaii = algorithm.build();
 * nsgaii.run();
 * }</pre>
 *
 * <p>Non-configurable parameters, such as the number of problem variables and, depending on the
 * mutation operator, other derived values, are set automatically based on the problem and algorithm
 * configuration.
 *
 * @see MutationParameter
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
