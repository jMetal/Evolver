package org.uma.evolver.algorithm.rvea;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Abstract base class for configurable RVEA (Reference Vector Guided Evolutionary Algorithm)
 * implementations.
 *
 * <p>Follows the same template-method pattern as {@link org.uma.evolver.algorithm.nsgaii.BaseNSGAII}.
 * The configurable components are initialisation strategy and variation (crossover + mutation),
 * which are drawn from a {@link ParameterSpace}. Selection and replacement are fixed by the RVEA
 * algorithm (random selection, APD-based replacement) and are not exposed to the parameter space.
 *
 * <p>Constraints inherited from RVEA:
 * <ul>
 *   <li>Population size must equal the number of reference vectors.</li>
 *   <li>Termination is always by number of evaluations.</li>
 *   <li>External archives are not supported.</li>
 * </ul>
 *
 * @param <S> the type of solutions handled by this algorithm
 * @see org.uma.evolver.algorithm.nsgaii.BaseNSGAII
 */
public abstract class BaseRVEA<S extends Solution<?>> implements BaseLevelAlgorithm<S> {

  protected final ParameterSpace parameterSpace;
  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;
  protected final double alpha;
  protected final double fr;
  protected final List<double[]> referenceVectors;

  protected BaseRVEA(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      double alpha,
      double fr,
      List<double[]> referenceVectors) {
    Check.notNull(problem, "problem");
    Check.notNull(parameterSpace, "parameterSpace");
    Check.notNull(referenceVectors, "referenceVectors");
    Check.valueIsPositive(populationSize, "populationSize");
    Check.valueIsPositive(maximumNumberOfEvaluations, "maximumNumberOfEvaluations");
    Check.that(populationSize == referenceVectors.size(),
        "populationSize must equal the number of reference vectors ("
            + referenceVectors.size() + ")");

    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
    this.alpha = alpha;
    this.fr = fr;
    this.referenceVectors = referenceVectors;
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  @Override
  public Algorithm<List<S>> build() {
    setNonConfigurableParameters();
    Archive<S> archive = usingExternalArchive() ? createExternalArchive() : null;
    SolutionsCreation<S> init = createInitialSolutions();
    Variation<S> variation = createVariation();
    return buildRVEA(init, variation, archive);
  }

  private boolean usingExternalArchive() {
    return parameterSpace.get("algorithmResult").value().equals("externalArchive");
  }

  @SuppressWarnings("unchecked")
  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> archiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");
    archiveParameter.setSize(populationSize);
    return archiveParameter.getExternalArchive();
  }

  /**
   * Sets parameters that are fixed or derived from the problem (e.g. number of variables for
   * mutation operators). Called at the start of {@link #build()}.
   */
  protected abstract void setNonConfigurableParameters();

  /**
   * Assembles and returns the concrete {@link Algorithm} using the configured initialisation
   * strategy, variation operator, and optional external archive. Subclasses use
   * {@code RVEABuilder} here, passing the typed placeholder operators required by its constructor.
   */
  protected abstract Algorithm<List<S>> buildRVEA(
      SolutionsCreation<S> init, Variation<S> variation, Archive<S> archive);

  @SuppressWarnings("unchecked")
  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  @SuppressWarnings("unchecked")
  protected Variation<S> createVariation() {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter(
        "offspringPopulationSize", parameterSpace.get("offspringPopulationSize").value());
    return variationParameter.getVariation();
  }
}
