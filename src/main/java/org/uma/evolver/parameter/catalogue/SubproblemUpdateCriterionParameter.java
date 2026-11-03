package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.SubproblemUpdateCriterion;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.FeasibilityRulesCriterion;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.ImprovedEpsilonCriterion;
import org.uma.jmetal.component.catalogue.ea.replacement.subproblemupdate.impl.ViolationThresholdCriterion;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter for how MOEA/D decides whether a new solution replaces the solution of a
 * subproblem, which is where MOEA/D handles constraints. The criteria, all of jMetal and all the
 * plain comparison of the aggregation function values on problems without constraints, are:
 *
 * <ul>
 *   <li>feasibilityRules: Deb's feasibility rules, the less violated solution, or the better
 *       aggregation value if both are equally violated
 *   <li>improvedEpsilon: the improved epsilon method of Fan et al. (Soft Computing, 2019), with the
 *       sub-parameters improvedEpsilonTao, improvedEpsilonFeasibilityRatioThreshold and
 *       improvedEpsilonDecayFraction (the fraction of the run after which epsilon is zero), and the
 *       number of generations of the run as the non-configurable sub-parameter
 *       "numberOfGenerations"
 *   <li>violationThreshold: the adaptive violation threshold (the feasibility ratio times the mean
 *       violation of the population), as jMetal's ViolationThresholdCriterion, after Jan and
 *       Khanum (Applied Soft Computing 13(1), 2013)
 * </ul>
 *
 * @param <S> the type of the solutions
 */
public class SubproblemUpdateCriterionParameter<S extends Solution<?>>
    extends CategoricalParameter {
  public static final String DEFAULT_NAME = "subproblemUpdateCriterion";

  /**
   * Creates the parameter with its default name.
   *
   * @param validValues the criteria it can take
   */
  public SubproblemUpdateCriterionParameter(List<String> validValues) {
    this(DEFAULT_NAME, validValues);
  }

  /**
   * Creates the parameter.
   *
   * @param name the name of the parameter
   * @param validValues the criteria it can take
   */
  public SubproblemUpdateCriterionParameter(String name, List<String> validValues) {
    super(name, validValues);
  }

  /**
   * Creates the criterion of the current value.
   *
   * @return the subproblem update criterion
   */
  public SubproblemUpdateCriterion<S> getSubproblemUpdateCriterion() {
    return switch (value()) {
      case "feasibilityRules" -> new FeasibilityRulesCriterion<>();
      case "improvedEpsilon" -> {
        Object numberOfGenerations = nonConfigurableSubParameters().get("numberOfGenerations");
        Check.notNull(numberOfGenerations);
        double decayFraction =
            (double) findConditionalParameter("improvedEpsilonDecayFraction").value();
        int tc = Math.max(1, (int) Math.round(decayFraction * (int) numberOfGenerations));
        yield new ImprovedEpsilonCriterion<>(
            (double) findConditionalParameter("improvedEpsilonTao").value(),
            (double) findConditionalParameter("improvedEpsilonFeasibilityRatioThreshold").value(),
            tc);
      }
      case "violationThreshold" -> new ViolationThresholdCriterion<>();
      default -> throw new JMetalException("Subproblem update criterion unknown: " + value());
    };
  }
}
