package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter for creating initial binary solutions in evolutionary algorithms.
 * This parameter defines the strategy used to generate the initial population of binary solutions.
 *
 * <p>Currently supports the following strategies:
 * <ul>
 *   <li>DEFAULT_STRATEGY: Creates random binary solutions using RandomSolutionsCreation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * CreateInitialSolutionsBinaryParameter param = new CreateInitialSolutionsBinaryParameter(
 *     List.of("DEFAULT_STRATEGY"));
 * param.setValue("DEFAULT_STRATEGY");
 * SolutionsCreation<BinarySolution> strategy = param.getCreateInitialSolutionsStrategy(problem, 100);
 * List<BinarySolution> initialPopulation = strategy.create();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation
 */
public class CreateInitialSolutionsBinaryParameter
    extends CreateInitialSolutionsParameter<BinarySolution> {

  /** The default strategy name for creating initial binary solutions. */
  public static final String DEFAULT_STRATEGY = "DEFAULT_STRATEGY";

  /**
   * Creates a new CreateInitialSolutionsBinaryParameter with the specified valid values.
   *
   * @param validValues A list of valid strategy names for creating initial binary solutions.
   *                   Should typically include at least DEFAULT_STRATEGY.
   * @throws IllegalArgumentException if validValues is null or empty
   */
  public CreateInitialSolutionsBinaryParameter(List<String> validValues) {
    this("createInitialSolutions", validValues);
  }

   /**
   * Creates a new CreateInitialSolutionsBinaryParameter with the specified name and valid values.
   *
   * @param name
   * @param validValues
   */
  public CreateInitialSolutionsBinaryParameter(String name, List<String> validValues) {
    super(name, validValues);
  }
  
  /**
   * Creates and returns a SolutionsCreation strategy for binary solutions based on the current parameter value.
   *
   * @param problem The binary problem for which to create initial solutions
   * @param populationSize The number of solutions to create
   * @return A configured SolutionsCreation strategy for binary solutions
   * @throws JMetalException if the current value does not match any known strategy
   * @throws IllegalArgumentException if problem is null or populationSize is not positive
   */
  @Override
  public SolutionsCreation<BinarySolution> getCreateInitialSolutionsStrategy(
      Problem<BinarySolution> problem, int populationSize) {
    if (problem == null) {
      throw new IllegalArgumentException("Problem cannot be null");
    }
    if (populationSize <= 0) {
      throw new IllegalArgumentException("Population size must be positive: " + populationSize);
    }
    
    if (value().equals(DEFAULT_STRATEGY)) {
      return new RandomSolutionsCreation<>(problem, populationSize);
    }
    
    throw new JMetalException("Unknown initialization strategy: " + value());
  }
}
