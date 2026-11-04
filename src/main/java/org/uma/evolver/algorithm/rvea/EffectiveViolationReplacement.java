package org.uma.evolver.algorithm.rvea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;

/**
 * A replacement that makes a selection by feasibility and violation, such as that of C-RVEA, follow
 * a constraint-handling technique: it hands the replacement copies of the solutions whose
 * constraints carry the violation of the technique (all zero for a solution that the technique
 * treats as feasible, and minus the violation in the first constraint otherwise), and returns the
 * original solutions that the replacement keeps. The replacement must not keep solutions between
 * generations, as RVEA's does not (iRVEA's archive would keep the copies).
 *
 * @param <S> the type of the solutions
 */
final class EffectiveViolationReplacement<S extends Solution<?>> implements Replacement<S> {
  private final Replacement<S> replacement;
  private final ToDoubleFunction<S> violation;

  /**
   * Creates the replacement.
   *
   * @param replacement the replacement that selects by feasibility and violation
   * @param violation the violation of a solution as the technique judges it
   */
  EffectiveViolationReplacement(Replacement<S> replacement, ToDoubleFunction<S> violation) {
    this.replacement = replacement;
    this.violation = violation;
  }

  @Override
  public List<S> replace(List<S> population, List<S> offspringPopulation) {
    Map<S, S> originals = new IdentityHashMap<>();
    List<S> parents = withEffectiveViolation(population, originals);
    List<S> children = withEffectiveViolation(offspringPopulation, originals);
    return replacement.replace(parents, children).stream().map(originals::get).toList();
  }

  @SuppressWarnings("unchecked")
  private List<S> withEffectiveViolation(List<S> solutions, Map<S, S> originals) {
    List<S> result = new ArrayList<>(solutions.size());
    for (S solution : solutions) {
      S copy = (S) solution.copy();
      double value = violation.applyAsDouble(solution);
      // A violation precomputed by the problem would hide that of the constraints
      copy.attributes().remove(ConstraintHandling.PRECOMPUTED.OVERALL_CONSTRAINT_VIOLATION);
      copy.attributes().remove(ConstraintHandling.PRECOMPUTED.NUMBER_OF_VIOLATED_CONSTRAINTS);
      Arrays.fill(copy.constraints(), 0.0);
      if (value > 0 && copy.constraints().length > 0) {
        copy.constraints()[0] = -value;
      }
      originals.put(copy, solution);
      result.add(copy);
    }
    return result;
  }
}
