package org.uma.evolver.meta.problem;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Meta-optimization problem using derivation tree encoding.
 *
 * <p>The tree structure mirrors the grammar of the parameter space, so only active parameters
 * are present in each solution. This eliminates the inactive-variable problem inherent in flat
 * double encodings.
 *
 * @param <S> the type of solutions used by the base algorithm being optimized
 * @see AbstractMetaOptimizationProblem
 */
public class TreeMetaOptimizationProblem<S extends Solution<?>>
    extends AbstractMetaOptimizationProblem<S, DerivationTreeSolution> {

  private final TreeSolutionGenerator solutionGenerator;

  /**
   * Constructs a tree-based meta-optimization problem.
   *
   * @param baseAlgorithm            the base algorithm whose parameters will be optimized
   * @param problems                 the training problems
   * @param referenceFrontFileNames  reference front files, one per problem
   * @param indicators               quality indicators to optimize
   * @param evaluationBudgetStrategy evaluation budget per problem
   * @param numberOfIndependentRuns  independent runs per meta-evaluation
   * @param solutionGenerator        generator for random tree solutions
   */
  public TreeMetaOptimizationProblem(
      BaseLevelAlgorithm<S> baseAlgorithm,
      List<Problem<S>> problems,
      List<String> referenceFrontFileNames,
      List<QualityIndicator> indicators,
      EvaluationBudgetStrategy evaluationBudgetStrategy,
      int numberOfIndependentRuns,
      TreeSolutionGenerator solutionGenerator) {
    super(baseAlgorithm, problems, referenceFrontFileNames, indicators,
        evaluationBudgetStrategy, numberOfIndependentRuns);
    Check.notNull(solutionGenerator);
    this.solutionGenerator = solutionGenerator;
  }

  @Override
  public int numberOfVariables() {
    return baseAlgorithm.parameterSpace().topLevelParameters().size();
  }

  @Override
  public DerivationTreeSolution createSolution() {
    return solutionGenerator.generate(numberOfObjectives());
  }

  @Override
  protected String[] toParameterArray(DerivationTreeSolution solution) {
    return solution.toParameterArray();
  }

  @Override
  public String name() {
    return "TreeMetaOptimizationProblem";
  }
}
