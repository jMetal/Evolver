package org.uma.evolver.algorithm.agemoea;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A configurable AGE-MOEA for derivation tree solutions ({@link DerivationTreeSolution}), used as
 * a meta-optimizer with the tree encoding. The problem must be a {@link
 * TreeMetaOptimizationProblem}: its {@link TreeMetaOptimizationProblem#solutionGenerator()} is
 * handed to the {@code mutation} parameter, which needs it to regenerate subtrees.
 *
 * <p>The parameter space is expected to be built with a {@code TreeParameterFactory} (see {@code
 * AGEMOEAMetaTree.yaml}).
 */
public class TreeAGEMOEA extends BaseAGEMOEA<DerivationTreeSolution> {

  public TreeAGEMOEA(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  public TreeAGEMOEA(
      Problem<DerivationTreeSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public synchronized BaseLevelAlgorithm<DerivationTreeSolution> createInstance(
      Problem<DerivationTreeSolution> problem, int maximumNumberOfEvaluations) {
    return new TreeAGEMOEA(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  @Override
  protected void setNonConfigurableParameters() {
    Check.that(
        problem instanceof TreeMetaOptimizationProblem<?>,
        "TreeAGEMOEA requires a TreeMetaOptimizationProblem");
    var mutationParameter =
        (MutationParameter<DerivationTreeSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "treeSolutionGenerator",
        ((TreeMetaOptimizationProblem<?>) problem).solutionGenerator());
  }
}
