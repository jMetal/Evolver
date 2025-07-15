package org.uma.evolver.algorithm.base.nsgaii;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

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
public class NSGAIIDoubleV2 extends AbstractNSGAIIV2<DoubleSolution> {

  /**
   * Constructs an NSGAIIDouble instance with the given population size and parameter space.
   *
   * @param populationSize the population size to use
   * @param parameterSpace the parameter space for configuration
   */
  public NSGAIIDoubleV2(int populationSize, ParameterSpace parameterSpace) {
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
  public NSGAIIDoubleV2(
      Problem<DoubleSolution> problem, int populationSize, int maximumNumberOfEvaluations, ParameterSpace parameterSpace) {
   super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace) ;
  }

  /**
   * Creates a new instance of NSGAIIDouble for the given problem and maximum number of evaluations.
   *
   * @param problem the problem to solve
   * @param maximumNumberOfEvaluations the evaluation budget
   * @return a new configured instance of NSGAIIDouble
   */
  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    String yamlParameterSpaceFile = "resources/parameterSpaces/NSGAIIDouble.yaml" ;
     var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());

    return new NSGAIIDoubleV2(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
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

    /*
    if (mutationParameter.value().equals("uniform")) {
      mutationParameter.addNonConfigurableSubParameter(
              "uniformMutationPerturbation",
              parameterSpace.get("uniformMutationPerturbation"));
    }

     */
  }
}
