package org.uma.evolver.algorithm.base;

import java.util.List;

import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A builder class for creating instances of {@link EvolutionaryAlgorithm} with optional archive support.
 * This builder provides a flexible way to configure and instantiate evolutionary algorithms,
 * with or without an external archive for storing non-dominated solutions.
 *
 * <p>Example usage:
 * <pre>{@code
 * EvolutionaryAlgorithm<DoubleSolution> algorithm = new EvolutionaryAlgorithmBuilder<DoubleSolution>()
 *     .build(
 *         "NSGA-II",
 *         initialSolutionsCreation,
 *         evaluation,
 *         termination,
 *         selection,
 *         variation,
 *         replacement,
 *         archive  // can be null if no archive is needed
 *     );
 * }</pre>
 *
 * @param <S> the type of solutions handled by the algorithm
 */

public class EvolutionaryAlgorithmBuilder<S extends Solution<?>> {
  /**
   * Builds an instance of EvolutionaryAlgorithm with the specified components.
   *
   * @param name the name of the algorithm (used for identification and logging)
   * @param initialSolutionsCreation component responsible for creating the initial population
   * @param evaluation component that handles solution evaluation
   * @param termination condition that determines when the algorithm should stop
   * @param selection operator for selecting parent solutions
   * @param variation operator for creating new solutions through recombination and mutation
   * @param replacement strategy for updating the population with new solutions
   * @param archive optional archive for storing non-dominated solutions (can be null)
   * @return a configured instance of EvolutionaryAlgorithm
   * @throws IllegalArgumentException if any required parameter is null (except archive)
   */
  public EvolutionaryAlgorithm<S> build(
      String name,
      SolutionsCreation<S> initialSolutionsCreation,
      Evaluation<S> evaluation,
      Termination termination,
      Selection<S> selection,
      Variation<S> variation,
      Replacement<S> replacement,
      Archive<S> archive) {
    if (archive != null) {
      Check.that(evaluation instanceof SequentialEvaluationWithArchive, "The evaluator must be of class SequentialEvaluatorWithArchive");
      Check.notNull(((SequentialEvaluationWithArchive<S>) evaluation).archive());
      return new EvolutionaryAlgorithmWithArchive<>(
          name,
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement,
          archive);
    } else {
      return new EvolutionaryAlgorithm<>(
          name,
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement);
    }
  }

  /**
   * An extension of EvolutionaryAlgorithm that maintains an external archive of non-dominated solutions.
   * This class automatically updates the archive with new non-dominated solutions during the
   * evolutionary process and returns the archive contents as the final result.
   *
   * @param <S> the type of solutions stored in the archive
   */
  class EvolutionaryAlgorithmWithArchive<S extends Solution<?>> extends EvolutionaryAlgorithm<S> {

    private final Archive<S> archive;

    /**
     * Creates a new instance of EvolutionaryAlgorithmWithArchive.
     *
     * @param name the name of the algorithm
     * @param initialPopulationCreation component for creating the initial population
     * @param evaluation component for evaluating solutions
     * @param termination condition for stopping the algorithm
     * @param selection operator for parent selection
     * @param variation operator for creating offspring solutions
     * @param replacement strategy for population update
     * @param archive the archive to store non-dominated solutions (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public EvolutionaryAlgorithmWithArchive(
        String name,
        SolutionsCreation<S> initialPopulationCreation,
        Evaluation<S> evaluation,
        Termination termination,
        Selection<S> selection,
        Variation<S> variation,
        Replacement<S> replacement,
        Archive<S> archive) {
      super(
          name,
          initialPopulationCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement);
      this.archive = archive;
    }

    /**
     * Returns the solutions stored in the archive.
     *
     * @return a list of non-dominated solutions found during the search
     */
    @Override
    public List<S> result() {
      return archive.solutions();
    }
  }
}
