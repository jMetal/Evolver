package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter for creating initial double solutions in evolutionary algorithms.
 * This parameter defines the strategy used to generate the initial population of double solutions.
 *
 * <p>Supported strategies:
 * <ul>
 *   <li>DEFAULT_STRATEGY: Creates random double solutions using RandomSolutionsCreation</li>
 *   <li>SCATTER_SEARCH: Creates solutions using ScatterSearchSolutionsCreation with a default reference set size of 4</li>
 *   <li>LATIN_HYPERCUBE_SAMPLING: Creates solutions using Latin Hypercube Sampling for better space coverage</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * CreateInitialSolutionsDoubleParameter param = new CreateInitialSolutionsDoubleParameter(
 *     List.of("DEFAULT_STRATEGY", "SCATTER_SEARCH", "LATIN_HYPERCUBE_SAMPLING"));
 * param.setValue("LATIN_HYPERCUBE_SAMPLING");
 * SolutionsCreation<DoubleSolution> strategy = param.getCreateInitialSolutionsStrategy(problem, 100);
 * List<DoubleSolution> initialPopulation = strategy.create();
 * }
 * </pre>
 *
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation
 */
public class CreateInitialSolutionsDoubleParameter
    extends CreateInitialSolutionsParameter<DoubleSolution> {

  /** Strategy that creates random double solutions using uniform distribution. */
  public static final String DEFAULT_STRATEGY = "default";
  
  /** 
   * Strategy that creates solutions using scatter search with a reference set.
   * This provides good diversity in the initial population.
   */
  public static final String SCATTER_SEARCH = "scatterSearch";
  
  /** 
   * Strategy that uses Latin Hypercube Sampling to ensure good coverage of the search space.
   * This is particularly useful for high-dimensional problems.
   */
  public static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling";

  /**
   * Creates a new CreateInitialSolutionsDoubleParameter with the specified valid values.
   *
   * @param validValues A list of valid strategy names for creating initial double solutions.
   *                   Should typically include at least DEFAULT_STRATEGY, and optionally
   *                   SCATTER_SEARCH and/or LATIN_HYPERCUBE_SAMPLING.
   * @throws IllegalArgumentException if validValues is null or empty
   */
  public CreateInitialSolutionsDoubleParameter(List<String> validValues) {
    super(validValues);
  }

  /**
   * Creates and returns a SolutionsCreation strategy for double solutions based on the current parameter value.
   *
   * @param problem The double problem for which to create initial solutions
   * @param populationSize The number of solutions to create
   * @return A configured SolutionsCreation strategy for double solutions
   * @throws JMetalException if the current value does not match any known strategy
   * @throws IllegalArgumentException if problem is null, not a DoubleProblem (for some strategies),
   *                                  or populationSize is not positive
   */
  @Override
  public SolutionsCreation<DoubleSolution> getCreateInitialSolutionsStrategy(
      Problem<DoubleSolution> problem, int populationSize) {
    if (problem == null) {
      throw new IllegalArgumentException("Problem cannot be null");
    }
    if (populationSize <= 0) {
      throw new IllegalArgumentException("Population size must be positive: " + populationSize);
    }
    
    return switch (value()) {
      case DEFAULT_STRATEGY -> new RandomSolutionsCreation<>(problem, populationSize);
      case SCATTER_SEARCH -> {
        if (!(problem instanceof DoubleProblem)) {
          throw new IllegalArgumentException("SCATTER_SEARCH strategy requires a DoubleProblem");
        }
        yield new ScatterSearchSolutionsCreation((DoubleProblem) problem, populationSize, 4);
      }
      case LATIN_HYPERCUBE_SAMPLING -> {
        if (!(problem instanceof DoubleProblem)) {
          throw new IllegalArgumentException("LATIN_HYPERCUBE_SAMPLING strategy requires a DoubleProblem");
        }
        yield new LatinHypercubeSamplingSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      default -> throw new JMetalException("Unknown initialization strategy: " + value());
    };
  }
}
