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
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.AGEMOEA2EnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.AGEMOEAEnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.agemoea.SurvivalScoreComparator;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

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

    return new EvolutionaryAlgorithmBuilder<S>()
        .build(
            algorithmName(),
            initialSolutionsCreation,
            evaluation,
            termination,
            selection,
            variation,
            replacement,
            archive);
  }

  protected abstract void setNonConfigurableParameters();

  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");

    externalArchiveParameter.setSize(populationSize);
    return externalArchiveParameter.getExternalArchive();
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
    return selectionParameter.getSelection(
        variation.matingPoolSize(), new SurvivalScoreComparator<>());
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
   * ({@code agemoea}) or that of AGE-MOEA-II ({@code agemoeaII}).
   *
   * @return the replacement strategy configured with the requested environmental selection
   */
  protected Replacement<S> createReplacement() {
    int numberOfObjectives = problem.numberOfObjectives();
    AGEMOEAEnvironmentalSelection<S> environmentalSelection =
        switch (replacementVariant()) {
          case "agemoea" -> new AGEMOEAEnvironmentalSelection<>(numberOfObjectives);
          case "agemoeaII" -> new AGEMOEA2EnvironmentalSelection<>(numberOfObjectives);
          default ->
              throw new JMetalException("AGE-MOEA replacement unknown: " + replacementVariant());
        };
    return new AGEMOEAReplacement<>(environmentalSelection);
  }
}
