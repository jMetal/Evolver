package org.uma.evolver.algorithm.base.moead;

import java.io.FileNotFoundException;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADCommonParameterSpace;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
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
 * Abstract base class for configurable implementations of the MOEA/D algorithm.
 *
 * <p>This class provides the core structure and workflow for MOEA/D variants, supporting
 * flexible configuration via a {@link MOEADCommonParameterSpace}. It manages the main algorithm
 * components, including initialization, variation, selection, replacement, evaluation,
 * and termination. Subclasses should extend this class and implement the method
 * {@link #setNonConfigurableParameters()} to set any parameters that depend on the problem
 * instance or are derived from the configuration.
 *
 * <p>Typical usage involves:
 * <ul>
 *   <li>Defining the problem, population size, and maximum number of evaluations</li>
 *   <li>Configuring the parameter space with the desired operators and strategies</li>
 *   <li>Building and running the algorithm using the {@link #build()} method</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * MOEADCommonParameterSpace<DoubleSolution> parameterSpace = new MOEADCommonParameterSpace();
 * // Configure parameterSpace as needed
 * AbstractMOEAD<DoubleSolution> moead = new MOEADDouble(problem, 100, 25000, "weightsVectorDirectory", parameterSpace);
 * moead.parse(args);
 * EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();
 * algorithm.run();
 * }</pre>
 *
 * <p>Subclasses must implement {@link #setNonConfigurableParameters()} to set any parameters
 * that are fixed or derived from the problem instance. If there are no such parameters,
 * the implementation can be left empty.
 *
 * @param <S> the solution type handled by the algorithm
 */
public abstract class AbstractMOEAD<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  private MOEADCommonParameterSpace<S> parameterSpace;

  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;
  protected String weightVectorFilesDirectory;
  protected SequenceGenerator<Integer> subProblemIdGenerator;
  protected Neighborhood<S> neighborhood;
  protected int maximumNumberOfReplacedSolutions;
  protected AggregationFunction aggregationFunction;
  protected boolean normalizedObjectives;

  public AbstractMOEAD(int populationSize, MOEADCommonParameterSpace<S> parameterSpace) {
    this.populationSize = populationSize;
    this.parameterSpace = parameterSpace;
  }

  public AbstractMOEAD(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory,
      MOEADCommonParameterSpace<S> parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.weightVectorFilesDirectory = weightVectorFilesDirectory;
  }

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

  @Override
  public EvolutionaryAlgorithm<S> build() {
    setNonConfigurableParameters();

    Archive<S> archive = null;
    if (usingExternalArchive()) {
      archive = createExternalArchive();
    }
    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation);
    Evaluation<S> evaluation = createEvaluation(archive);
    Replacement<S> replacement = createReplacement(selection);
    Termination termination = createTermination();

    return new EvolutionaryAlgorithmBuilder().build(
        "MOEAD",
        initialSolutionsCreation,
        evaluation,
        termination,
        selection,
        variation,
        replacement,
        archive);
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

  private PopulationAndNeighborhoodSelection<S> createSelection(Variation<S> variation) {
    return (PopulationAndNeighborhoodSelection<S>)
        ((SelectionParameter<S>) parameterSpace.get(parameterSpace.SELECTION))
            .getSelection(variation.getMatingPoolSize(), null);
  }

  /**
   * Checks whether the algorithm is configured to use an external archive.
   *
   * @return {@code true} if an external archive is used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace
        .get(parameterSpace.ALGORITHM_RESULT)
        .value()
        .equals(parameterSpace.EXTERNAL_ARCHIVE);
  }

  private Variation<S> createVariation() {
    parameterSpace
        .get(parameterSpace.VARIATION)
        .addNonConfigurableSubParameter(
            parameterSpace.SUB_PROBLEM_ID_GENERATOR, subProblemIdGenerator);

    Variation<S> variation =
        ((VariationParameter<S>) parameterSpace.get(parameterSpace.VARIATION)).getVariation();
    return variation;
  }

  protected Neighborhood<S> getNeighborhood() {
    if (problem.numberOfObjectives() == 2) {
      neighborhood =
          new WeightVectorNeighborhood<>(
              populationSize, (int) parameterSpace.get(parameterSpace.NEIGHBORHOOD_SIZE).value());
    } else {
      try {
        neighborhood =
            new WeightVectorNeighborhood<>(
                populationSize,
                problem.numberOfObjectives(),
                (int) parameterSpace.get(parameterSpace.NEIGHBORHOOD_SIZE).value(),
                weightVectorFilesDirectory);
      } catch (FileNotFoundException exception) {
        exception.printStackTrace();
      }
    }

    return neighborhood;
  }

  private Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  /**
   * Creates and configures the external archive if required by the parameter space. The archive
   * size is set according to the current population size.
   *
   * @return the configured external archive, or {@code null} if not used
   */
  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get(parameterSpace.EXTERNAL_ARCHIVE);

    externalArchiveParameter.setSize(populationSize);
    Archive<S> archive = externalArchiveParameter.getExternalArchive();

    return archive;
  }

  private SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter)
            parameterSpace.get(parameterSpace.CREATE_INITIAL_SOLUTIONS))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  /**
   * Creates the evaluation component for the algorithm. If an external archive is used, a
   * sequential evaluation with archive is created; otherwise, a standard sequential evaluation is
   * used.
   *
   * @param archive the external archive, or {@code null} if not used
   * @return the evaluation component
   */
  protected Evaluation<S> createEvaluation(Archive<S> archive) {
    Evaluation<S> evaluation;
    if (usingExternalArchive()) {
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }
    return evaluation;
  }
}
