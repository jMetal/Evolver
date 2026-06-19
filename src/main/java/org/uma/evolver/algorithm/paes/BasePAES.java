package org.uma.evolver.algorithm.paes;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.PAESArchiveParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.BestSolutionsArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

/**
 * Abstract base class for configurable PAES (Pareto Archived Evolution Strategy).
 *
 * <p>PAES is a 1+1 evolution strategy that uses a bounded archive as a density estimator. The
 * population size is fixed at 1 (non-configurable). Variation is mutation-only (no crossover).
 * Replacement follows the three-way PAES rule: accept offspring if it dominates the current
 * solution, reject if dominated, or use the archive's density comparator as a tiebreaker when
 * neither solution dominates the other.
 *
 * <p>The size of the PAES archive ({@code numberOfSolutionsToFind}) is fixed and provided through
 * the constructor, analogous to the population size of other algorithms (typical value: 100).
 *
 * <p>Two result modes are supported via {@code algorithmResult}:
 * <ul>
 *   <li>{@code paesArchive} — returns the bounded PAES density archive directly.</li>
 *   <li>{@code externalArchive} — maintains a separate unbounded archive updated at every
 *       evaluation; the bounded PAES archive is still used internally for tiebreaking.</li>
 * </ul>
 *
 * @param <S> the solution type
 */
@SuppressWarnings("unchecked")
public abstract class BasePAES<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  protected final ParameterSpace parameterSpace;
  protected Problem<S> problem;
  protected int numberOfSolutionsToFind;
  protected int maximumNumberOfEvaluations;

  protected BasePAES(int numberOfSolutionsToFind, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.numberOfSolutionsToFind = numberOfSolutionsToFind;
  }

  protected BasePAES(
      Problem<S> problem,
      int numberOfSolutionsToFind,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this(numberOfSolutionsToFind, parameterSpace);
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

    BoundedArchive<S> paesArchive = createPAESArchive();

    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(paesArchive);
    Termination termination = createTermination();
    Replacement<S> replacement = new PAESReplacement<>(paesArchive, new DefaultDominanceComparator<>());

    if (usingExternalArchive()) {
      Archive<S> externalArchive = createExternalArchive();
      Evaluation<S> evaluation = new SequentialEvaluationWithArchive<>(problem, externalArchive);
      return new EvolutionaryAlgorithmBuilder<S>().build(
          "PAES", initialSolutionsCreation, evaluation, termination, selection, variation,
          replacement, externalArchive);
    } else {
      Evaluation<S> evaluation = new SequentialEvaluation<>(problem);
      return new PAESEvolutionaryAlgorithm<>(
          "PAES", initialSolutionsCreation, evaluation, termination, selection, variation,
          replacement, paesArchive);
    }
  }

  protected abstract void setNonConfigurableParameters();

  protected BoundedArchive<S> createPAESArchive() {
    PAESArchiveParameter<S> paesArchiveParam =
        (PAESArchiveParameter<S>) parameterSpace.get("paesArchiveType");
    paesArchiveParam.setSize(numberOfSolutionsToFind);
    return paesArchiveParam.getBoundedArchive();
  }

  protected SolutionsCreation<S> createInitialSolutions() {
    // A (1+1) ES starts from a single random solution. Population-diversity strategies
    // (Latin hypercube, scatter search) are meaningless with a single solution.
    return new RandomSolutionsCreation<>(problem, 1);
  }

  protected Variation<S> createVariation() {
    MutationParameter<S> mutationParameter =
        (MutationParameter<S>) parameterSpace.get("mutation");
    return new MutationOnlyVariation<>(1, mutationParameter.getMutation());
  }

  protected Selection<S> createSelection(BoundedArchive<S> paesArchive) {
    double archiveSelectionProbability =
        (double) parameterSpace.get("archiveSelectionProbability").value();
    return new PAESSelection<>(archiveSelectionProbability, paesArchive);
  }

  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  protected Archive<S> createExternalArchive() {
    return new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(), numberOfSolutionsToFind);
  }

  private boolean usingExternalArchive() {
    return "externalArchive".equals(parameterSpace.get("algorithmResult").value());
  }

  /**
   * Extends {@link EvolutionaryAlgorithm} to return the PAES archive contents as the result
   * instead of the population (which has size 1 and is not the intended output).
   */
  private static class PAESEvolutionaryAlgorithm<S extends Solution<?>>
      extends EvolutionaryAlgorithm<S> {
    private final BoundedArchive<S> paesArchive;

    public PAESEvolutionaryAlgorithm(
        String name,
        SolutionsCreation<S> createInitialPopulation,
        Evaluation<S> evaluation,
        Termination termination,
        Selection<S> selection,
        Variation<S> variation,
        Replacement<S> replacement,
        BoundedArchive<S> paesArchive) {
      super(name, createInitialPopulation, evaluation, termination, selection, variation,
          replacement);
      this.paesArchive = paesArchive;
    }

    @Override
    public List<S> result() {
      return paesArchive.solutions();
    }
  }
}
