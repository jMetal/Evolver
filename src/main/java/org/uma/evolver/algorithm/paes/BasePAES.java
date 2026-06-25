package org.uma.evolver.algorithm.paes;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.PAESArchiveParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.PAESBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.PAESReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PAESSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.MutationOnlyVariation;
import org.uma.jmetal.operator.mutation.MutationOperator;
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
 *   <li>{@code paesArchive} — delegates to {@link PAESBuilder}; returns the bounded PAES
 *       density archive directly.</li>
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
    MutationOperator<S> mutation =
        ((MutationParameter<S>) parameterSpace.get("mutation")).getMutation();
    double archiveSelectionProbability =
        (double) parameterSpace.get("archiveSelectionProbability").value();
    Termination termination = new TerminationByEvaluations(maximumNumberOfEvaluations);

    if (usingExternalArchive()) {
      Archive<S> externalArchive = createExternalArchive();
      return new EvolutionaryAlgorithmBuilder<S>().build(
          "PAES",
          new RandomSolutionsCreation<>(problem, 1),
          new SequentialEvaluationWithArchive<>(problem, externalArchive),
          termination,
          new PAESSelection<>(archiveSelectionProbability, paesArchive),
          new MutationOnlyVariation<>(1, mutation),
          new PAESReplacement<>(paesArchive, new DefaultDominanceComparator<>()),
          externalArchive);
    } else {
      return new PAESBuilder<>(problem, numberOfSolutionsToFind, mutation, paesArchive)
          .setTermination(termination)
          .setArchiveSelectionProbability(archiveSelectionProbability)
          .build();
    }
  }

  protected abstract void setNonConfigurableParameters();

  protected BoundedArchive<S> createPAESArchive() {
    PAESArchiveParameter<S> paesArchiveParam =
        (PAESArchiveParameter<S>) parameterSpace.get("paesArchiveType");
    paesArchiveParam.setSize(numberOfSolutionsToFind);
    return paesArchiveParam.getBoundedArchive();
  }

  protected Archive<S> createExternalArchive() {
    return new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(), numberOfSolutionsToFind);
  }

  private boolean usingExternalArchive() {
    return "externalArchive".equals(parameterSpace.get("algorithmResult").value());
  }
}
