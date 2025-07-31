package org.uma.evolver.algorithm.base.moead;

import java.io.FileNotFoundException;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADCommonParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.MOEADReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PopulationAndNeighborhoodSelection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.aggregationfunction.AggregationFunction;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.neighborhood.impl.WeightVectorNeighborhood;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

/**
 * Abstract base class for configurable implementations of the MOEA/D (Multi-Objective Evolutionary
 * Algorithm based on Decomposition) algorithm.
 *
 * <p>This class provides a flexible and extensible implementation of the MOEA/D framework,
 * supporting various decomposition-based multi-objective optimization approaches. It manages the
 * configuration and assembly of the main algorithm components through a parameter space.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Decomposition-based optimization using weight vectors</li>
 *   <li>Configurable aggregation functions</li>
 *   <li>Neighborhood-based mating and replacement</li>
 *   <li>Support for external archives</li>
 *   <li>Flexible termination conditions</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Create a problem instance
 * Problem<DoubleSolution> problem = new MyMultiObjectiveProblem();
 * 
 * // Configure the algorithm
 * int populationSize = 100;
 * int maxEvaluations = 25000;
 * String weightVectorsDir = "src/main/resources/weightVectors";
 * ParameterSpace parameterSpace = new MOEADCommonParameterSpace<>();
 * 
 * // Create and run the algorithm
 * BaseMOEAD<DoubleSolution> moead = new MOEADDouble(problem, populationSize, maxEvaluations, 
 *                                                  weightVectorsDir, parameterSpace);
 * EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();
 * algorithm.run();
 * 
 * // Get results
 * List<DoubleSolution> population = algorithm.result();
 * }</pre>
 *
 * <p>Subclasses must implement the {@link #setNonConfigurableParameters()} method to set any
 * parameters that are fixed or derived from the problem instance. If no such parameters exist,
 * the implementation can be left empty.
 *
 * @param <S> the type of solutions handled by this algorithm, must extend {@link Solution}
 * @see <a href="https://ieeexplore.ieee.org/document/4358754">Q. Zhang and H. Li, "MOEA/D: A Multiobjective 
 * Evolutionary Algorithm Based on Decomposition," in IEEE Transactions on Evolutionary Computation, 
 * vol. 11, no. 6, pp. 712-731, Dec. 2007, doi: 10.1109/TEVC.2007.892759.</a>
 * @see BaseLevelAlgorithm
 * @see EvolutionaryAlgorithm
 * @since version
 */
public abstract class BaseMOEAD<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  /** The parameter space containing all configurable components and parameters. */
  private final ParameterSpace parameterSpace;

  /** The optimization problem to be solved. */
  protected Problem<S> problem;
  
  /** The size of the population. */
  protected int populationSize;
  
  /** The maximum number of evaluations allowed for the algorithm. */
  protected int maximumNumberOfEvaluations;
  
  /** Directory containing the weight vector files. */
  protected String weightVectorFilesDirectory;
  
  /** Generator for subproblem indices. */
  protected SequenceGenerator<Integer> subProblemIdGenerator;
  
  /** Neighborhood structure for solution interactions. */
  protected Neighborhood<S> neighborhood;
  
  /** Maximum number of solutions to replace in the neighborhood. */
  protected int maximumNumberOfReplacedSolutions;
  
  /** Aggregation function for scalarizing objectives. */
  protected AggregationFunction aggregationFunction;
  
  /** Flag indicating whether objectives should be normalized. */
  protected boolean normalizedObjectives;

  /**
   * Constructs a new BaseMOEAD instance with the specified population size, weight vector directory,
   * and parameter space.
   * 
   * <p>Note: This creates a partially configured instance. The {@link #createInstance(Problem, int)}
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param populationSize the size of the population. Must be positive.
   * @param weightVectorFilesDirectory the directory containing weight vector files. Must not be null.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if populationSize is not positive, or if any parameter is null
   * @throws IllegalStateException if the weight vector files cannot be found or parsed
   */
  public BaseMOEAD(int populationSize, String weightVectorFilesDirectory, ParameterSpace parameterSpace) {
    if (populationSize <= 0) {
      throw new IllegalArgumentException("Population size must be positive: " + populationSize);
    }
    if (weightVectorFilesDirectory == null) {
      throw new IllegalArgumentException("Weight vector files directory cannot be null");
    }
    if (parameterSpace == null) {
      throw new IllegalArgumentException("Parameter space cannot be null");
    }
    
    this.populationSize = populationSize;
    this.weightVectorFilesDirectory = weightVectorFilesDirectory;
    this.parameterSpace = parameterSpace;
  }

  /**
   * Constructs a fully configured BaseMOEAD instance ready for execution.
   *
   * @param problem the optimization problem to be solved. Must not be null.
   * @param populationSize the size of the population. Must be positive.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the algorithm.
   *                                 Must be positive and greater than populationSize.
   * @param weightVectorFilesDirectory the directory containing weight vector files. Must not be null.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws IllegalStateException if the weight vector files cannot be found or parsed
   */
  public BaseMOEAD(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory,
      ParameterSpace parameterSpace) {
    this(populationSize, weightVectorFilesDirectory, parameterSpace);
    
    if (problem == null) {
      throw new IllegalArgumentException("Problem cannot be null");
    }
    if (maximumNumberOfEvaluations <= 0) {
      throw new IllegalArgumentException("Maximum number of evaluations must be positive: " + maximumNumberOfEvaluations);
    }
    if (maximumNumberOfEvaluations <= populationSize) {
      throw new IllegalArgumentException("Maximum number of evaluations must be greater than population size");
    }
    
    this.problem = problem;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
  }

  /**
   * Returns the parameter space used by this algorithm.
   *
   * @return the parameter space containing all configurable components and parameters
   */
  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  /**
   * Configures any parameters that are fixed or derived from the problem instance.
   *
   * <p>Subclasses must implement this method to set non-configurable parameters before building the
   * algorithm. If there are not non-configurable parameters, the implementation of this method will
   * be empty.
   */
  protected abstract void setNonConfigurableParameters();

  /**
   * Builds and configures the MOEA/D algorithm based on the current parameter space.
   * 
   * <p>This method initializes all necessary components for the MOEA/D algorithm, including:
   * <ul>
   *   <li>Termination condition based on maximum evaluations</li>
   *   <li>Solution evaluation strategy</li>
   *   <li>Initial population generation</li>
   *   <li>Variation operators (crossover and mutation)</li>
   *   <li>Selection mechanism for parent solutions</li>
   *   <li>Replacement strategy for updating the population</li>
   *   <li>Optional external archive for storing non-dominated solutions</li>
   * </ul>
   *
   * @return a fully configured EvolutionaryAlgorithm instance implementing MOEA/D
   * @throws IllegalStateException if required parameters are not properly configured
   */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public EvolutionaryAlgorithm<S> build() {
    setNonConfigurableParameters();

    Archive<S> externalArchive = null;
    if (usingExternalArchive()) {
      externalArchive = createExternalArchive();
    }

    Termination termination = createTermination();
    Evaluation<S> evaluation = createEvaluation(externalArchive);
    SolutionsCreation<S> initialSolutions = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation);
    Replacement<S> replacement = createReplacement(selection);

    // Create builder with raw types to avoid type inference issues
    EvolutionaryAlgorithmBuilder builder = new EvolutionaryAlgorithmBuilder("MOEA/D", problem, populationSize, initialSolutions);
    
    return builder
        .setTermination(termination)
        .setEvaluation(evaluation)
        .setVariation(variation)
        .setSelection(selection)
        .setReplacement(replacement)
        .setArchive(externalArchive)
        .build();
  }

  /**
   * Creates the replacement operator for the algorithm using ranking and density estimators.
   *
   * @return the replacement operator
   */
  protected Replacement<S> createReplacement(Selection<S> selection) {
    return new MOEADReplacement<S>(
        (PopulationAndNeighborhoodSelection<S>) selection,
        (WeightVectorNeighborhood<S>) neighborhood,
        aggregationFunction,
        subProblemIdGenerator,
        maximumNumberOfReplacedSolutions,
        normalizedObjectives);
  }

  /**
   * Creates the selection component for parent selection.
   * 
   * <p>This method configures the selection strategy based on the variation operator's
   * requirements for the number of parents and mating pool size.
   *
   * @param variation the variation operator that will use the selected parents
   * @return a selection component configured for the given variation operator
   * @throws IllegalStateException if the parameter space does not contain the required parameter
   */
  private PopulationAndNeighborhoodSelection<S> createSelection(Variation<S> variation) {
    return (PopulationAndNeighborhoodSelection<S>)
        ((SelectionParameter<S>) parameterSpace.get("selection"))
            .getSelection(variation.getMatingPoolSize(), null);
  }

  /**
   * Checks whether the algorithm is configured to use an external archive.
   *
   * @return {@code true} if an external archive is used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace
        .get("algorithmResult")
        .value()
        .equals("externalArchive");
  }

  /**
   * Creates the variation operator for the algorithm.
   * 
   * <p>Subclasses must implement this method to provide the specific variation operator
   * (typically a combination of crossover and mutation) appropriate for the solution type.
   *
   * @return a variation operator for creating new solutions
   */
  /**
   * Creates the variation operator for the algorithm.
   * 
   * <p>Subclasses must implement this method to provide the specific variation operator
   * (typically a combination of crossover and mutation) appropriate for the solution type.
   * The implementation should configure the variation operator using parameters from the
   * parameter space and add any required non-configurable parameters.
   *
   * @return a variation operator for creating new solutions
   */
  protected abstract Variation<S> createVariation();

  /**
   * Creates and configures the variation operator for the algorithm.
   * 
   * <p>This method retrieves the variation operator from the parameter space and adds
   * the subproblem ID generator as a non-configurable subparameter.
   *
   * @return a configured variation operator
   * @throws IllegalStateException if the variation operator cannot be created or configured
   */
  private Variation<S> createConfiguredVariation() {
    parameterSpace
        .get("variation")
        .addNonConfigurableSubParameter(
            "subProblemIdGenerator", subProblemIdGenerator);

    Variation<S> variation =
        ((VariationParameter<S>) parameterSpace.get("variation")).getVariation();
    return variation;
  }

  /**
   * Creates the neighborhood structure for solution interactions.
   * 
   * <p>This method creates a weight vector neighborhood based on the population size and number
   * of objectives in the problem. If the problem has two objectives, a simple weight vector
   * neighborhood is created; otherwise, a weight vector neighborhood with a specified size is
   * created.
   *
   * @return a neighborhood structure for solution interactions
   */
  protected Neighborhood<S> getNeighborhood() {
    if (problem.numberOfObjectives() == 2) {
      neighborhood =
          new WeightVectorNeighborhood<>(
              populationSize, (int) parameterSpace.get("neighborhoodSize").value());
    } else {
      try {
        neighborhood =
            new WeightVectorNeighborhood<>(
                populationSize,
                problem.numberOfObjectives(),
                (int) parameterSpace.get("neighborhoodSize").value(),
                weightVectorFilesDirectory);
      } catch (FileNotFoundException exception) {
        exception.printStackTrace();
      }
    }

    return neighborhood;
  }

  /**
   * Creates the termination condition for the algorithm.
   * 
   * <p>This implementation creates a termination condition based on the maximum number of
   * evaluations specified during construction. The algorithm will stop once this number of
   * solution evaluations is reached.
   *
   * @return a termination condition based on evaluation count
   * @see #maximumNumberOfEvaluations
   */
  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  /**
   * Creates and configures the external archive for storing non-dominated solutions.
   * 
   * <p>This method retrieves the external archive configuration from the parameter space
   * and sets its size to match the population size.
   *
   * @return a configured external archive instance
   * @throws IllegalStateException if the external archive cannot be created or configured
   */
  @SuppressWarnings("unchecked")
  private Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("externalArchiveType");
    externalArchiveParameter.setSize(populationSize);
    return externalArchiveParameter.getExternalArchive();
  }

  /**
   * Creates the component responsible for generating the initial population of solutions.
   * 
   * <p>This method retrieves the solution creation strategy from the parameter space
   * and configures it with the problem and population size.
   *
   * @return a component for creating the initial population
   * @throws IllegalStateException if the parameter space does not contain the required parameter
   */
  @SuppressWarnings("unchecked")
  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  /**
   * Creates the evaluation component for the algorithm.
   * 
   * <p>This method creates an evaluation component that can optionally update an external archive
   * with non-dominated solutions during the evaluation process.
   *
   * @param archive the external archive to update with evaluated solutions, or null if no archive is used
   * @return an evaluation component configured with the problem and optional archive
   * @see SequentialEvaluation
   * @see SequentialEvaluationWithArchive
   */
  /**
   * Creates the evaluation component for the algorithm.
   * 
   * <p>This method creates an evaluation component that can optionally update an external archive
   * with non-dominated solutions during the evaluation process. If no archive is provided,
   * a simple sequential evaluation is used.
   *
   * @param archive the external archive to update with evaluated solutions, or null if no archive is used
   * @return an evaluation component configured with the problem and optional archive
   * @see SequentialEvaluation
   * @see SequentialEvaluationWithArchive
   */
  protected Evaluation<S> createEvaluation(Archive<S> archive) {
    if (archive == null) {
      return new SequentialEvaluation<>(problem);
    } else {
      return new SequentialEvaluationWithArchive<>(problem, archive);
    }
  }
}
