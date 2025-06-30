package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Configurable implementation of the NSGA-II algorithm for double-valued (real-coded) problems.
 *
 * <p>This class provides a highly customizable version of NSGA-II, supporting:
 * <ul>
 *   <li>Various selection strategies (e.g., tournament, random)</li>
 *   <li>Multiple crossover operators (e.g., SBX, BLX-Alpha, whole arithmetic)</li>
 *   <li>Different mutation approaches (e.g., uniform, polynomial, non-uniform)</li>
 *   <li>Optional external archive integration</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 * NSGAIIDouble algorithm = new NSGAIIDouble(problem, 100, 25000);
 * algorithm.parse(args);
 * EvolutionaryAlgorithm<DoubleSolution> nsgaii = algorithm.build();
 * nsgaii.run();
 * }</pre>
 *
 * <p>Non-configurable parameters such as the number of problem variables and, depending on the mutation
 * operator, the maximum number of iterations or perturbation value, are set automatically based on the
 * problem and algorithm configuration.
 *
 * @see NSGAIIDoubleParameterSpace
 * @see MutationParameter
 */
public class NSGAIIDouble extends AbstractNSGAII<DoubleSolution> {
  /**
   * Constructs an NSGAIIDouble instance with the given population size and a default parameter space.
   *
   * @param populationSize the population size to use
   */
  public NSGAIIDouble(int populationSize) {
    this(populationSize, new NSGAIIDoubleParameterSpace());
  }

  /**
   * Constructs an NSGAIIDouble instance with the given population size and parameter space.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public NSGAIIDouble(int populationSize, NSGAIIDoubleParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Constructs an NSGAIIDouble instance with the given problem, population size, and maximum number of evaluations.
   * Uses a default parameter space.
   *
   * @param problem the problem to solve
   * @param populationSize the population size to use
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   */
  public NSGAIIDouble(
      Problem<DoubleSolution> problem, int populationSize, int maximumNumberOfEvaluations) {
   super(problem, populationSize, maximumNumberOfEvaluations, new NSGAIIDoubleParameterSpace()) ;
  }

  /**
   * Creates a new instance of NSGAIIDouble for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of NSGAIIDouble
   */
  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new NSGAIIDouble(problem, populationSize, maximumNumberOfEvaluations);
  }

  /**
   * Sets non-configurable parameters that depend on the problem or algorithm configuration.
   * <p>
   * This method automatically sets:
   * <ul>
   *   <li>The number of problem variables for the mutation operator.</li>
   *   <li>The maximum number of iterations for non-uniform mutation.</li>
   *   <li>The perturbation value for uniform mutation.</li>
   * </ul>
   */
  @Override
  protected void setNonConfigurableParameters() {
    NSGAIIDoubleParameterSpace parameterSpace = (NSGAIIDoubleParameterSpace) parameterSpace();

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
  }
}
