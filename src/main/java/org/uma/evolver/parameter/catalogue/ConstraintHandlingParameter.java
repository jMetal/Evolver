package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.constrainthandling.ConstrainedDominance;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;
import org.uma.jmetal.util.constrainthandling.EpsilonConstrained;
import org.uma.jmetal.util.constrainthandling.PushPullSearch;
import org.uma.jmetal.util.constrainthandling.SelfAdaptivePenalty;
import org.uma.jmetal.util.constrainthandling.StochasticRanking;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter for the constraint-handling technique of an algorithm that compares
 * solutions by dominance, with the techniques of jMetal ({@code
 * org.uma.jmetal.util.constrainthandling}). The available techniques are:
 *
 * <ul>
 *   <li>cdp: Deb's constrained-domination principle, with the overall constraint violation
 *   <li>violatedConstraints: the same, with the number of violated constraints
 *   <li>epsilonConstrained: the epsilon-constrained method, with the sub-parameters
 *       epsilonInitialQuantile, epsilonExponent and epsilonDecayFraction
 *   <li>stochasticRanking: stochastic ranking, with the sub-parameter
 *       stochasticRankingProbability
 *   <li>pushPull: push and pull search, with the sub-parameters pushPullChangeThreshold,
 *       pushPullWindowFraction, pushPullDecayFraction, pushPullExponent,
 *       pushPullFeasibilityThreshold and pushPullReductionRate
 *   <li>selfAdaptivePenalty: the self-adaptive penalty function, with no sub-parameters
 * </ul>
 *
 * <p>The techniques that change during the run (epsilonConstrained, pushPull) follow the progress
 * of the run, which the algorithm gives them through jMetal's {@code ConstraintHandlingReplacement}
 * and {@code RunProgress}.
 *
 * @param <S> the type of the solutions
 */
public class ConstraintHandlingParameter<S extends Solution<?>> extends CategoricalParameter {
  public static final String DEFAULT_NAME = "constraintHandling";

  /**
   * Creates the parameter with its default name.
   *
   * @param validValues the techniques it can take
   */
  public ConstraintHandlingParameter(List<String> validValues) {
    this(DEFAULT_NAME, validValues);
  }

  /**
   * Creates the parameter.
   *
   * @param name the name of the parameter
   * @param validValues the techniques it can take
   */
  public ConstraintHandlingParameter(String name, List<String> validValues) {
    super(name, validValues);
  }

  /**
   * The constraint-handling technique of an algorithm: the one of the {@code constraintHandling}
   * parameter of its parameter space if it has one, or else Deb's constrained-domination principle,
   * which is plain Pareto dominance on problems without constraints.
   *
   * @param parameterSpace the parameter space of the algorithm, already parsed
   * @param <S> the type of the solutions
   * @return the constraint-handling technique
   */
  @SuppressWarnings("unchecked")
  public static <S extends Solution<?>> ConstraintHandlingTechnique<S> technique(
      ParameterSpace parameterSpace) {
    if (!parameterSpace.parameters().containsKey(DEFAULT_NAME)) {
      return new ConstrainedDominance<>();
    }
    var parameter = (ConstraintHandlingParameter<S>) parameterSpace.get(DEFAULT_NAME);
    return parameter.getConstraintHandlingTechnique();
  }

  /**
   * Creates the technique of the current value.
   *
   * @return the constraint-handling technique
   */
  public ConstraintHandlingTechnique<S> getConstraintHandlingTechnique() {
    return switch (value()) {
      case "cdp" -> new ConstrainedDominance<>();
      case "violatedConstraints" ->
          new ConstrainedDominance<>(
              ConstrainedDominance.ViolationMeasure.NUMBER_OF_VIOLATED_CONSTRAINTS);
      case "epsilonConstrained" ->
          new EpsilonConstrained<>(
              (double) findConditionalParameter("epsilonInitialQuantile").value(),
              (double) findConditionalParameter("epsilonExponent").value(),
              (double) findConditionalParameter("epsilonDecayFraction").value());
      case "stochasticRanking" ->
          new StochasticRanking<>(
              (double) findConditionalParameter("stochasticRankingProbability").value());
      case "pushPull" ->
          new PushPullSearch<>(
              (double) findConditionalParameter("pushPullChangeThreshold").value(),
              (double) findConditionalParameter("pushPullWindowFraction").value(),
              (double) findConditionalParameter("pushPullDecayFraction").value(),
              (double) findConditionalParameter("pushPullExponent").value(),
              (double) findConditionalParameter("pushPullFeasibilityThreshold").value(),
              (double) findConditionalParameter("pushPullReductionRate").value());
      case "selfAdaptivePenalty" -> new SelfAdaptivePenalty<>();
      default -> throw new JMetalException("Constraint-handling technique unknown: " + value());
    };
  }
}
