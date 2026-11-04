package org.uma.evolver.algorithm.agemoea;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
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
import org.uma.jmetal.component.catalogue.ea.replacement.impl.AGEMOEAReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.AGEMOEA2EnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.AGEMOEAEnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.SurvivalScoreComparator;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.util.RunProgress;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;

/**
 * Abstract base class for configurable AGE-MOEA (Adaptive GEometry-based Many-Objective
 * Evolutionary Algorithm) implementations, covering AGE-MOEA and AGE-MOEA-II.
 *
 * <p>This class provides a flexible foundation for building AGE-MOEA variants for different
 * solution types. It manages the configuration and assembly of the main algorithmic components
 * (selection, variation, replacement, evaluation, termination, etc.) through a parameter space
 * abstraction. AGE-MOEA selects survivors using an adaptive geometry that approximates the Pareto
 * front in normalized objective space. The {@code replacement} parameter selects the variant, as
 * in {@link org.uma.evolver.algorithm.rvea.BaseRVEA}: {@code agemoea}, which estimates the
 * curvature {@code p} of the front in closed form from its central point and measures diversity
 * with the Minkowski distance of order {@code p}, or {@code agemoeaII}, which estimates {@code p}
 * with Newton-Raphson from the point closest to the ideal point and approximates geodesic
 * distances on the front. The implementation of both is jMetal's.
 *
 * <p>Problems with constraints: if the parameter space has the {@code constraintHandling} parameter
 * ({@code AGEMOEADoubleConstrained.yaml}), the algorithm uses the constraint-handling technique of
 * jMetal that it selects (see {@link ConstraintHandlingParameter}): the environmental selection
 * sorts the population into fronts with the ranking of the technique, the tournament compares those
 * ranks, the technique is updated once per generation with the progress of the run ({@link
 * ConstraintHandlingReplacement}, {@link RunProgress}), and the bounded external archives compare
 * solutions with the constrained-domination principle. Without the parameter (e.g. {@code
 * AGEMOEADouble.yaml}) none of this happens, and the constraints are ignored.
 *
 * <p>Subclasses must implement {@link #setNonConfigurableParameters()} to set any parameters
 * derived from the problem instance.
 *
 * @param <S> the solution type handled by this algorithm
 * @see <a href="https://doi.org/10.1145/3321707.3321839">Pareto Front Estimation through Geometry
 *     and Adaptive Mating (AGE-MOEA)</a>
 * @see <a href="https://doi.org/10.1145/3512290.3528732">An Improved Pareto Front Modeling
 *     Algorithm for Large-scale Many-Objective Optimization (AGE-MOEA-II)</a>
 */
public abstract class BaseAGEMOEA<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  protected final ParameterSpace parameterSpace;

  protected Problem<S> problem;
  protected int populationSize;
  protected int offspringPopulationSize;
  protected int maximumNumberOfEvaluations;
  protected Archive<S> externalArchive;

  /**
   * The constraint-handling technique, or {@code null} if the parameter space has no {@code
   * constraintHandling} parameter.
   */
  protected ConstraintHandlingTechnique<S> constraintHandlingTechnique;

  /** The progress of the run, which the constraint-handling technique follows (if there is one). */
  protected RunProgress progress;

  /**
   * The ranking of the constraint-handling technique, shared by the environmental selection, which
   * computes the ranks, and the tournament, which compares them ({@code null} without a technique).
   */
  protected Ranking<S> ranking;

  protected BaseAGEMOEA(int populationSize, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
    this.offspringPopulationSize = populationSize;
  }

  protected BaseAGEMOEA(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this(populationSize, parameterSpace);
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
    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation);
    Evaluation<S> evaluation = createEvaluation(archive);
    Replacement<S> replacement = createReplacement();
    Termination termination = createTermination();

    EvolutionaryAlgorithm<S> algorithm =
        new EvolutionaryAlgorithmBuilder<S>()
            .build(
                algorithmName(),
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

  protected Selection<S> createSelection(Variation<S> variation) {
    var selectionParameter = (SelectionParameter<S>) parameterSpace.get("selection");
    // With a constraint-handling technique, the tournament compares the ranks of its ranking
    return selectionParameter.getSelection(
        variation.matingPoolSize(),
        usingConstraintHandling()
            ? new SurvivalScoreComparator<>(ranking)
            : new SurvivalScoreComparator<>());
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

  /** The name of the configured variant: AGE-MOEA or AGE-MOEA-II. */
  protected String algorithmName() {
    return switch (replacementVariant()) {
      case "agemoea" -> "AGE-MOEA";
      case "agemoeaII" -> "AGE-MOEA-II";
      default -> throw new JMetalException("AGE-MOEA replacement unknown: " + replacementVariant());
    };
  }

  private String replacementVariant() {
    return (String) parameterSpace.get("replacement").value();
  }

  /**
   * Builds the replacement of the configured variant: the environmental selection of AGE-MOEA
   * ({@code agemoea}) or that of AGE-MOEA-II ({@code agemoeaII}); with a constraint-handling
   * technique, it sorts the population with the ranking of the technique, updated once per
   * generation.
   *
   * @return the replacement strategy configured with the requested environmental selection
   */
  protected Replacement<S> createReplacement() {
    int numberOfObjectives = problem.numberOfObjectives();
    AGEMOEAEnvironmentalSelection<S> environmentalSelection =
        switch (replacementVariant()) {
          case "agemoea" ->
              usingConstraintHandling()
                  ? new AGEMOEAEnvironmentalSelection<>(numberOfObjectives, ranking)
                  : new AGEMOEAEnvironmentalSelection<>(numberOfObjectives);
          case "agemoeaII" ->
              usingConstraintHandling()
                  ? new AGEMOEA2EnvironmentalSelection<>(numberOfObjectives, ranking)
                  : new AGEMOEA2EnvironmentalSelection<>(numberOfObjectives);
          default ->
              throw new JMetalException("AGE-MOEA replacement unknown: " + replacementVariant());
        };
    Replacement<S> replacement = new AGEMOEAReplacement<>(environmentalSelection);
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
      ranking = null;
      return;
    }
    constraintHandlingTechnique = ConstraintHandlingParameter.technique(parameterSpace);
    progress = new RunProgress();
    ranking = constraintHandlingTechnique.ranking();
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
