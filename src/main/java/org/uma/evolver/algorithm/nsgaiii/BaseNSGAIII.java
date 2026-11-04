package org.uma.evolver.algorithm.nsgaiii;

import java.util.Comparator;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ConstraintHandlingParameter;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
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
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.NSGAIIIReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.util.RunProgress;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.densityestimator.impl.ReferencePointNicheDistanceEstimator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.referencepoint.ReferencePointGenerator;

/**
 * Abstract base class for configurable NSGA-III (Non-dominated Sorting Genetic Algorithm III)
 * implementations.
 *
 * <p>This class provides a flexible foundation for building NSGA-III variants for different
 * solution types. It manages the configuration and assembly of the main algorithmic components
 * (selection, variation, replacement, evaluation, termination, etc.) through a parameter space
 * abstraction. NSGA-III selects survivors using non-dominated sorting combined with a
 * reference-point-based niching mechanism that preserves diversity in many-objective problems.
 *
 * <p>By default, reference points are generated with the Das-Dennis systematic approach on a
 * unit simplex: the number of lattice divisions is derived from the population size and the
 * number of problem objectives as the smallest number of divisions whose lattice contains at
 * least as many points as the population size. Alternatively, an explicit list of reference
 * points (e.g. read from a weight-vector file, the same convention used for MOEA/D and RVEA) can
 * be supplied via the constructor, bypassing Das-Dennis generation entirely -- this is useful to
 * give NSGA-III the exact same reference-vector set used by other decomposition-based algorithms
 * in a comparison, rather than the closest Das-Dennis lattice size (which need not match the
 * requested population size exactly).
 *
 * <p>Problems with constraints: if the parameter space has the {@code constraintHandling} parameter
 * ({@code NSGAIIIDoubleConstrained.yaml}), the algorithm uses the constraint-handling technique of
 * jMetal that it selects (see {@link ConstraintHandlingParameter}): the ranking of the technique
 * replaces the non-dominated sorting in the replacement and in the tournament, the technique is
 * updated once per generation with the progress of the run ({@link ConstraintHandlingReplacement},
 * {@link RunProgress}), and the bounded external archives compare solutions with the
 * constrained-domination principle; the reference points and the niche distance do not change.
 * Without the parameter (e.g. {@code NSGAIIIDouble.yaml}) none of this happens, and the
 * constraints are ignored.
 *
 * <p>Subclasses must implement {@link #setNonConfigurableParameters()} to set any parameters
 * derived from the problem instance.
 *
 * @param <S> the solution type handled by this algorithm
 * @see <a href="https://doi.org/10.1109/TEVC.2013.2281535">K. Deb and H. Jain, "An Evolutionary
 *     Many-Objective Optimization Algorithm Using Reference-Point-Based Nondominated Sorting
 *     Approach, Part I", IEEE TEVC, vol. 18, no. 4, 2014</a>
 */
public abstract class BaseNSGAIII<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  protected final ParameterSpace parameterSpace;

  protected Problem<S> problem;
  protected int populationSize;
  protected int offspringPopulationSize;
  protected int maximumNumberOfEvaluations;
  protected Archive<S> externalArchive;

  protected Ranking<S> ranking;

  /**
   * The constraint-handling technique, or {@code null} if the parameter space has no {@code
   * constraintHandling} parameter.
   */
  protected ConstraintHandlingTechnique<S> constraintHandlingTechnique;

  /** The progress of the run, which the constraint-handling technique follows (if there is one). */
  protected RunProgress progress;
  protected List<double[]> referencePoints;
  protected final List<double[]> explicitReferencePoints;
  protected ReferencePointNicheDistanceEstimator<S> densityEstimator;

  protected BaseNSGAIII(int populationSize, ParameterSpace parameterSpace) {
    this(populationSize, parameterSpace, null);
  }

  protected BaseNSGAIII(
      int populationSize, ParameterSpace parameterSpace, List<double[]> explicitReferencePoints) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
    this.offspringPopulationSize = populationSize;
    this.explicitReferencePoints = explicitReferencePoints;
  }

  protected BaseNSGAIII(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this(problem, populationSize, maximumNumberOfEvaluations, parameterSpace, null);
  }

  /**
   * Constructs an instance using an explicit list of reference points instead of generating them
   * with Das-Dennis. {@code explicitReferencePoints} must not be empty; its size does not need to
   * equal {@code populationSize} (matching the flexibility Das-Dennis itself already has).
   */
  protected BaseNSGAIII(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      List<double[]> explicitReferencePoints) {
    this(populationSize, parameterSpace, explicitReferencePoints);
    this.problem = problem;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  @Override
  public EvolutionaryAlgorithm<S> build() {
    setNonConfigurableParameters();
    configureConstraintHandling();
    Archive<S> archive = null;
    if (usingExternalArchive()) {
      archive = createExternalArchive();
      updatePopulationSize(archive);
      Check.notNull(archive);
    }
    configureReferencePointComponents();
    if (usingConstraintHandling()) {
      ranking = constraintHandlingTechnique.ranking();
    }
    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation);
    Evaluation<S> evaluation = createEvaluation(archive);
    Replacement<S> replacement = createReplacement();
    Termination termination = createTermination();

    EvolutionaryAlgorithm<S> algorithm =
        new EvolutionaryAlgorithmBuilder<S>()
            .build(
                "NSGA-III",
                initialSolutionsCreation,
                evaluation,
                termination,
                selection,
                variation,
                replacement,
                archive);
    if (usingConstraintHandling()) {
      progress.bind(algorithm::termination, algorithm::attributes);
    }
    return algorithm;
  }

  protected abstract void setNonConfigurableParameters();

  /**
   * Creates the ranking, the reference points, and the niche distance estimator shared by the
   * selection and replacement components. Must be called after the population size is final
   * (i.e., after the external archive handling). Uses {@link #explicitReferencePoints} if
   * supplied at construction time; otherwise falls back to Das-Dennis generation.
   */
  private void configureReferencePointComponents() {
    int numberOfObjectives = problem.numberOfObjectives();
    if (explicitReferencePoints != null) {
      referencePoints = explicitReferencePoints;
    } else {
      int divisions = numberOfDivisions(numberOfObjectives, populationSize);
      referencePoints = ReferencePointGenerator.generateSingleLayer(numberOfObjectives, divisions);
    }
    ranking = new FastNonDominatedSortRanking<>();
    densityEstimator =
        new ReferencePointNicheDistanceEstimator<>(referencePoints, numberOfObjectives);
  }

  /**
   * Computes the smallest number of Das-Dennis lattice divisions whose number of reference points
   * is greater than or equal to the population size.
   */
  private static int numberOfDivisions(int numberOfObjectives, int populationSize) {
    int divisions = 1;
    while (numberOfReferencePoints(numberOfObjectives, divisions) < populationSize) {
      divisions++;
    }
    return divisions;
  }

  /** Number of Das-Dennis points for a lattice: C(divisions + M - 1, M - 1). */
  private static long numberOfReferencePoints(int numberOfObjectives, int divisions) {
    long result = 1;
    for (int i = 1; i < numberOfObjectives; i++) {
      result = result * (divisions + i) / i;
    }
    return result;
  }

  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");

    externalArchiveParameter.setSize(populationSize);
    return usingConstraintHandling()
        ? externalArchiveParameter.getConstrainedExternalArchive()
        : externalArchiveParameter.getExternalArchive();
  }

  private boolean usingExternalArchive() {
    return parameterSpace.get("algorithmResult").value().equals("externalArchive");
  }

  private void updatePopulationSize(Archive<S> archive) {
    if (archive != null) {
      populationSize = (int) parameterSpace.get("populationSizeWithArchive").value();
    }
  }

  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  protected Evaluation<S> createEvaluation(Archive<S> archive) {
    Evaluation<S> evaluation;
    if (usingExternalArchive()) {
      Check.notNull(archive);
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }
    return evaluation;
  }

  /**
   * Creates the mating selection component. Solutions are compared by non-domination rank first
   * and by distance to their associated reference point second, mirroring the canonical NSGA-III
   * selection pressure.
   */
  protected Selection<S> createSelection(Variation<S> variation) {
    var selectionParameter = (SelectionParameter<S>) parameterSpace.get("selection");
    List<Comparator<S>> comparators =
        List.of(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value));
    return selectionParameter.getSelection(
        variation.matingPoolSize(), new MultiComparator<>(comparators));
  }

  protected Variation<S> createVariation() {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter(
        "offspringPopulationSize", parameterSpace.get("offspringPopulationSize").value());

    return variationParameter.getVariation();
  }

  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  /**
   * Builds the NSGA-III replacement strategy: non-dominated sorting plus reference-point-based
   * niching on the critical front.
   */
  protected Replacement<S> createReplacement() {
    Replacement<S> replacement =
        new NSGAIIIReplacement<>(
            ranking, referencePoints, problem.numberOfObjectives(), populationSize);
    return usingConstraintHandling()
        ? new ConstraintHandlingReplacement<>(constraintHandlingTechnique, replacement, progress)
        : replacement;
  }

  /**
   * Sets the constraint-handling technique if the parameter space has the {@code
   * constraintHandling} parameter. Without the parameter it does nothing.
   */
  protected void configureConstraintHandling() {
    if (!parameterSpace.parameters().containsKey(ConstraintHandlingParameter.DEFAULT_NAME)) {
      constraintHandlingTechnique = null;
      progress = null;
      return;
    }
    constraintHandlingTechnique = ConstraintHandlingParameter.technique(parameterSpace);
    progress = new RunProgress();
  }

  /**
   * Checks whether the algorithm handles the constraints of the problem, that is, whether its
   * parameter space has the {@code constraintHandling} parameter.
   *
   * @return {@code true} if a constraint-handling technique is used
   */
  protected boolean usingConstraintHandling() {
    return constraintHandlingTechnique != null;
  }
}
