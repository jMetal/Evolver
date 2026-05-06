package org.uma.evolver.meta.problem;

import java.util.Collections;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;

/**
 * Meta-optimization problem using a flat double encoding.
 *
 * <p>Each parameter is mapped to a double variable in [0, 1]. The full parameter space is
 * flattened (including conditional sub-parameters), and the decoding step re-activates only the
 * relevant parameters at evaluation time.
 *
 * @param <S> the type of solutions used by the base algorithm being optimized
 * @see AbstractMetaOptimizationProblem
 */
public class MetaOptimizationProblem<S extends Solution<?>>
    extends AbstractMetaOptimizationProblem<S, DoubleSolution> {

  private final List<Parameter<?>> parameters;

  /**
   * Constructs a meta-optimization problem with flat double encoding.
   *
   * @param baseAlgorithm            the base algorithm whose parameters will be optimized
   * @param problems                 the training problems
   * @param referenceFrontFileNames  reference front files, one per problem
   * @param indicators               quality indicators to optimize
   * @param evaluationBudgetStrategy evaluation budget per problem
   * @param numberOfIndependentRuns  independent runs per meta-evaluation
   */
  public MetaOptimizationProblem(
      BaseLevelAlgorithm<S> baseAlgorithm,
      List<Problem<S>> problems,
      List<String> referenceFrontFileNames,
      List<QualityIndicator> indicators,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      int numberOfIndependentRuns) {
    super(baseAlgorithm, problems, referenceFrontFileNames, indicators,
        evaluationBudgetStrategy, numberOfIndependentRuns);
    this.parameters = ParameterManagement.parameterFlattening(
        baseAlgorithm.parameterSpace().topLevelParameters());
  }

  @Override
  public int numberOfVariables() {
    return parameters.size();
  }

  @Override
  public DoubleSolution createSolution() {
    return new DefaultDoubleSolution(
        Collections.nCopies(parameters.size(), Bounds.create(0.0, 1.0)),
        numberOfObjectives(), 0);
  }

  @Override
  protected String[] toParameterArray(DoubleSolution solution) {
    return ParameterManagement.decodeParametersToString(parameters, solution.variables())
        .toString().split("\\s+");
  }

  /** Returns the flattened list of parameters being optimized. */
  public List<Parameter<?>> parameters() {
    return Collections.unmodifiableList(parameters);
  }

  /** Returns the top-level parameters from the parameter space. */
  public List<Parameter<?>> topLevelParameters() {
    return baseAlgorithm.parameterSpace().topLevelParameters();
  }
}
