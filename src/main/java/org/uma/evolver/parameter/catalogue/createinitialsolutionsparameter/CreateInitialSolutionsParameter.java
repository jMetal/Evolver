package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * An abstract categorical parameter for creating initial solutions in evolutionary algorithms. This
 * parameter defines the strategy used to generate the initial population.
 *
 * <p>Implementations of this class should provide specific strategies for creating initial
 * solutions based on the problem type (e.g., binary, double, permutation).
 *
 * @param <S> The type of solutions being created
 */
public abstract class CreateInitialSolutionsParameter<S extends Solution<?>>
    extends CategoricalParameter {

  /**
   * Creates a new CreateInitialSolutionsParameter with the specified name and valid values.
   *
   * @param name
   * @param validValues
   */
  public CreateInitialSolutionsParameter(String name, List<String> validValues) {
    super(name, validValues);
  }

  /**
   * Creates a new CreateInitialSolutionsParameter with the specified valid values.
   *
   * @param validValues A list of valid strategy names for creating initial solutions
   * @throws IllegalArgumentException if validValues is null or empty
   */
  protected CreateInitialSolutionsParameter(List<String> validValues) {
    this("createInitialSolutions", validValues);
  }

  /**
   * Creates and returns a SolutionsCreation strategy based on the current parameter value. The
   * specific implementation is provided by concrete subclasses.
   *
   * @param problem The problem for which to create initial solutions
   * @param populationSize The number of solutions to create
   * @return A configured SolutionsCreation strategy
   * @throws IllegalArgumentException if problem is null or populationSize is not positive
   */
  public abstract SolutionsCreation<S> getCreateInitialSolutionsStrategy(
      Problem<S> problem, int populationSize);
}
