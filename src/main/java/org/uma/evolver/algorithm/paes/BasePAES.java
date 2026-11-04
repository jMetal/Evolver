package org.uma.evolver.algorithm.paes;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.uma.evolver.parameter.catalogue.ConstraintHandlingParameter;
import org.uma.evolver.parameter.catalogue.PAESArchiveParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.PAESBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.PAESReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PAESSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.MutationOnlyVariation;
import org.uma.jmetal.component.util.RunProgress;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.BestSolutionsArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.constrainthandling.PairwiseConstraintHandlingTechnique;
import org.uma.jmetal.util.errorchecking.JMetalException;

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
 * <p>Problems with constraints: if the parameter space has the {@code constraintHandling} parameter
 * ({@code PAESDoubleConstrained.yaml}), the constraint-handling technique of jMetal that it selects
 * (see {@link ConstraintHandlingParameter}) compares the mutant with the current solution, and it
 * is updated with the progress of the run and with the archive and both candidates once every as
 * many evaluations as the archive holds, a generation of PAES ({@link
 * ConstraintHandlingReplacement}, {@link RunProgress}); the PAES archive compares solutions with
 * the constrained-domination principle. PAES only compares pairs of solutions, so the technique
 * must be a {@link PairwiseConstraintHandlingTechnique} ({@code cdp}, {@code violatedConstraints},
 * {@code epsilonConstrained} or {@code pushPull}). Without the parameter (e.g. {@code
 * PAESDouble.yaml}) none of this happens, and the constraints are ignored.
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

    PairwiseConstraintHandlingTechnique<S> technique = constraintHandlingTechnique();
    BoundedArchive<S> paesArchive = createPAESArchive(technique != null);
    MutationOperator<S> mutation =
        ((MutationParameter<S>) parameterSpace.get("mutation")).getMutation();
    double archiveSelectionProbability =
        (double) parameterSpace.get("archiveSelectionProbability").value();
    Termination termination = new TerminationByEvaluations(maximumNumberOfEvaluations);

    if (usingExternalArchive()) {
      Archive<S> externalArchive = createExternalArchive();
      RunProgress progress = new RunProgress();
      Replacement<S> replacement =
          technique == null
              ? new PAESReplacement<>(paesArchive, new DefaultDominanceComparator<>())
              // As jMetal's PAESBuilder does with a technique (see the paesArchive branch)
              : new ConstraintHandlingReplacement<>(
                  technique,
                  new PAESReplacement<>(paesArchive, technique.comparator()),
                  progress,
                  paesArchive.maximumSize(),
                  (population, offspring) ->
                      archiveAndCandidates(paesArchive, population, offspring));
      EvolutionaryAlgorithm<S> algorithm =
          new EvolutionaryAlgorithmBuilder<S>()
              .build(
                  "PAES",
                  new RandomSolutionsCreation<>(problem, 1),
                  new SequentialEvaluationWithArchive<>(problem, externalArchive),
                  termination,
                  new PAESSelection<>(archiveSelectionProbability, paesArchive),
                  new MutationOnlyVariation<>(1, mutation),
                  replacement,
                  externalArchive);
      if (technique != null) {
        progress.bind(algorithm::termination, algorithm::attributes);
      }
      return algorithm;
    } else {
      var builder =
          new PAESBuilder<>(problem, numberOfSolutionsToFind, mutation, paesArchive)
              .setTermination(termination)
              .setArchiveSelectionProbability(archiveSelectionProbability);
      if (technique != null) {
        builder.setConstraintHandlingTechnique(technique);
      }
      return builder.build();
    }
  }

  /**
   * The constraint-handling technique if the parameter space has the {@code constraintHandling}
   * parameter, or {@code null} without it.
   *
   * @return the technique, which compares pairs of solutions
   * @throws JMetalException if the technique does not compare pairs of solutions
   */
  protected PairwiseConstraintHandlingTechnique<S> constraintHandlingTechnique() {
    if (!parameterSpace.parameters().containsKey(ConstraintHandlingParameter.DEFAULT_NAME)) {
      return null;
    }
    ConstraintHandlingTechnique<S> technique =
        ConstraintHandlingParameter.technique(parameterSpace);
    if (!(technique instanceof PairwiseConstraintHandlingTechnique<S> pairwise)) {
      throw new JMetalException(
          "PAES compares pairs of solutions, so it needs a technique that compares pairs (cdp,"
              + " violatedConstraints, epsilonConstrained or pushPull), not "
              + parameterSpace.get(ConstraintHandlingParameter.DEFAULT_NAME).value());
    }
    return pairwise;
  }

  /** The archive, the current solution and the mutant, each solution once. */
  private static <S extends Solution<?>> List<S> archiveAndCandidates(
      BoundedArchive<S> archive, List<S> population, List<S> offspringPopulation) {
    Set<S> solutions = Collections.newSetFromMap(new IdentityHashMap<>());
    solutions.addAll(archive.solutions());
    solutions.addAll(population);
    solutions.addAll(offspringPopulation);
    return new ArrayList<>(solutions);
  }

  protected abstract void setNonConfigurableParameters();

  protected BoundedArchive<S> createPAESArchive(boolean constrained) {
    PAESArchiveParameter<S> paesArchiveParam =
        (PAESArchiveParameter<S>) parameterSpace.get("paesArchiveType");
    paesArchiveParam.setSize(numberOfSolutionsToFind);
    return constrained
        ? paesArchiveParam.getConstrainedBoundedArchive()
        : paesArchiveParam.getBoundedArchive();
  }

  protected Archive<S> createExternalArchive() {
    return new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(), numberOfSolutionsToFind);
  }

  private boolean usingExternalArchive() {
    return "externalArchive".equals(parameterSpace.get("algorithmResult").value());
  }
}
