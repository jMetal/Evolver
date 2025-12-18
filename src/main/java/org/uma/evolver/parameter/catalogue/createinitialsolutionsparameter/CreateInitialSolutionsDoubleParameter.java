package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ChaosBasedSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.GridSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.OppositionBasedSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A parameter for creating initial double solutions in evolutionary algorithms.
 * This parameter defines the strategy used to generate the initial population of double solutions.
 *
 * <p>Supported strategies (compatible with jMetal 6.10):
 * <ul>
 *   <li>DEFAULT_STRATEGY: Creates random double solutions using RandomSolutionsCreation</li>
 *   <li>SCATTER_SEARCH: Creates solutions using ScatterSearchSolutionsCreation with configurable reference set size</li>
 *   <li>LATIN_HYPERCUBE_SAMPLING: Creates solutions using Latin Hypercube Sampling for better space coverage</li>
 *   <li>CHAOS_BASED: Creates solutions using chaotic sequences (logistic map) for better coverage than random</li>
 *   <li>GRID_BASED: Creates solutions on a regular grid for systematic and deterministic coverage</li>
 *   <li>OPPOSITION_BASED: Creates pairs of solutions (original + opposite) for enhanced exploration</li>
 * </ul>
 *
 * <p>The strategies are designed to provide different levels of initial population diversity:
 * <ul>
 *   <li><strong>Random</strong>: Fast generation with uniform random distribution</li>
 *   <li><strong>Scatter Search</strong>: Enhanced diversity through reference set-based generation</li>
 *   <li><strong>Latin Hypercube</strong>: Optimal space coverage, particularly effective for high-dimensional problems</li>
 *   <li><strong>Chaos-Based</strong>: Deterministic but chaotic sequences providing better coverage than random</li>
 *   <li><strong>Grid-Based</strong>: Systematic grid coverage, ideal for low-dimensional problems</li>
 *   <li><strong>Opposition-Based</strong>: Explores opposite regions of search space for better exploration</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * CreateInitialSolutionsDoubleParameter param = new CreateInitialSolutionsDoubleParameter(
 *     List.of("default", "scatterSearch", "latinHypercubeSampling", "chaosBased", "gridBased", "oppositionBased"));
 * param.setValue("chaosBased");
 * SolutionsCreation<DoubleSolution> strategy = param.getCreateInitialSolutionsStrategy(problem, 100);
 * List<DoubleSolution> initialPopulation = strategy.create();
 * }
 * </pre>
 *
 * @since jMetal 6.10
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ChaosBasedSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.GridSolutionsCreation
 * @see org.uma.jmetal.component.catalogue.common.solutionscreation.impl.OppositionBasedSolutionsCreation
 */
public class CreateInitialSolutionsDoubleParameter
    extends CreateInitialSolutionsParameter<DoubleSolution> {

  /** Strategy that creates random double solutions using uniform distribution. */
  public static final String DEFAULT_STRATEGY = "default";
  
  /** 
   * Strategy that creates solutions using scatter search with a reference set.
   * This provides good diversity in the initial population by using a reference set
   * to guide the generation of diverse solutions.
   */
  public static final String SCATTER_SEARCH = "scatterSearch";
  
  /** 
   * Strategy that uses Latin Hypercube Sampling to ensure good coverage of the search space.
   * This is particularly useful for high-dimensional problems as it guarantees better
   * space-filling properties compared to random sampling.
   */
  public static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling";

  /** 
   * Strategy that uses chaos-based sequences (logistic map) for solution creation.
   * Provides better coverage than random initialization while maintaining unpredictability.
   * Useful when reproducibility with a fixed seed is needed.
   */
  public static final String CHAOS_BASED = "chaosBased";

  /** 
   * Strategy that creates solutions on a regular grid across the search space.
   * Ensures systematic and deterministic coverage. Best for low-dimensional problems
   * as grid size grows exponentially with dimensions.
   */
  public static final String GRID_BASED = "gridBased";

  /** 
   * Strategy that creates pairs of solutions (original + opposite) by mirroring
   * each variable across the center of its bounds. Enhances exploration by ensuring
   * opposite regions of the search space are covered.
   */
  public static final String OPPOSITION_BASED = "oppositionBased";

  /** Default reference set size for scatter search strategy. */
  private static final int DEFAULT_REFERENCE_SET_SIZE = 4;

  /**
   * Creates a new CreateInitialSolutionsDoubleParameter with the specified valid values.
   *
   * @param validValues A list of valid strategy names for creating initial double solutions.
   *                   Should typically include at least DEFAULT_STRATEGY, and optionally
   *                   SCATTER_SEARCH, LATIN_HYPERCUBE_SAMPLING, CHAOS_BASED, GRID_BASED, 
   *                   and/or OPPOSITION_BASED.
   * @throws IllegalArgumentException if validValues is null or empty
   */
  public CreateInitialSolutionsDoubleParameter(List<String> validValues) {
    this(DEFAULT_NAME, validValues);
  }

  /**
   * Creates a new CreateInitialSolutionsDoubleParameter with the specified name and valid values.
   *
   * @param name The parameter name
   * @param validValues A list of valid strategy names for creating initial double solutions
   * @throws IllegalArgumentException if name is null/empty or validValues is null/empty
   */
  public CreateInitialSolutionsDoubleParameter(String name, List<String> validValues) {
    super(name, validValues);
  }
  
  /**
   * Creates and returns a SolutionsCreation strategy for double solutions based on the current parameter value.
   * 
   * <p>This method uses improved error checking with jMetal 6.10's Check utility and provides
   * better error messages for debugging.
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
    
    // Use jMetal 6.10's improved error checking
    Check.notNull(problem);
    Check.that(populationSize > 0, "Population size must be positive, but was: " + populationSize);
    
    return switch (value()) {
      case DEFAULT_STRATEGY -> {
        // Random solutions creation - works with any Problem<DoubleSolution>
        yield new RandomSolutionsCreation<>(problem, populationSize);
      }
      case SCATTER_SEARCH -> {
        // Scatter search requires DoubleProblem for bounds information
        Check.that(problem instanceof DoubleProblem, 
            "SCATTER_SEARCH strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new ScatterSearchSolutionsCreation((DoubleProblem) problem, populationSize, DEFAULT_REFERENCE_SET_SIZE);
      }
      case LATIN_HYPERCUBE_SAMPLING -> {
        // Latin Hypercube Sampling requires DoubleProblem for bounds information
        Check.that(problem instanceof DoubleProblem, 
            "LATIN_HYPERCUBE_SAMPLING strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new LatinHypercubeSamplingSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case CHAOS_BASED -> {
        // Chaos-based solutions creation requires DoubleProblem for bounds information
        Check.that(problem instanceof DoubleProblem, 
            "CHAOS_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new ChaosBasedSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case GRID_BASED -> {
        // Grid-based solutions creation requires DoubleProblem for bounds information
        Check.that(problem instanceof DoubleProblem, 
            "GRID_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new GridSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case OPPOSITION_BASED -> {
        // Opposition-based solutions creation requires DoubleProblem for bounds information
        Check.that(problem instanceof DoubleProblem, 
            "OPPOSITION_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new OppositionBasedSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      default -> throw new JMetalException("Unknown initialization strategy: '" + value() + 
          "'. Valid strategies are: " + String.join(", ", validValues()));
    };
  }

  /**
   * Creates a SolutionsCreation strategy with a custom reference set size for scatter search.
   * This method provides additional configuration options for the scatter search strategy.
   *
   * @param problem The double problem for which to create initial solutions
   * @param populationSize The number of solutions to create
   * @param referenceSetSize The reference set size for scatter search (ignored for other strategies)
   * @return A configured SolutionsCreation strategy for double solutions
   * @throws JMetalException if the current value does not match any known strategy
   * @throws IllegalArgumentException if parameters are invalid
   */
  public SolutionsCreation<DoubleSolution> getCreateInitialSolutionsStrategy(
      Problem<DoubleSolution> problem, int populationSize, int referenceSetSize) {
    
    Check.notNull(problem);
    Check.that(populationSize > 0, "Population size must be positive, but was: " + populationSize);
    Check.that(referenceSetSize > 0, "Reference set size must be positive, but was: " + referenceSetSize);
    
    return switch (value()) {
      case DEFAULT_STRATEGY -> new RandomSolutionsCreation<>(problem, populationSize);
      case SCATTER_SEARCH -> {
        Check.that(problem instanceof DoubleProblem, 
            "SCATTER_SEARCH strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new ScatterSearchSolutionsCreation((DoubleProblem) problem, populationSize, referenceSetSize);
      }
      case LATIN_HYPERCUBE_SAMPLING -> {
        Check.that(problem instanceof DoubleProblem, 
            "LATIN_HYPERCUBE_SAMPLING strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new LatinHypercubeSamplingSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case CHAOS_BASED -> {
        Check.that(problem instanceof DoubleProblem, 
            "CHAOS_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new ChaosBasedSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case GRID_BASED -> {
        Check.that(problem instanceof DoubleProblem, 
            "GRID_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new GridSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      case OPPOSITION_BASED -> {
        Check.that(problem instanceof DoubleProblem, 
            "OPPOSITION_BASED strategy requires a DoubleProblem, but got: " + problem.getClass().getSimpleName());
        yield new OppositionBasedSolutionsCreation((DoubleProblem) problem, populationSize);
      }
      default -> throw new JMetalException("Unknown initialization strategy: '" + value() + 
          "'. Valid strategies are: " + String.join(", ", validValues()));
    };
  }
}
