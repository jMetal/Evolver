package org.uma.evolver.meta.algorithm;

import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.rdemoea.BaseRDEMOEA;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * A configurable SPEA2 (an RDEMOEA configured with strength ranking, k-nearest-neighbour density and sequential replacement) for derivation tree solutions ({@link DerivationTreeSolution}), used as
 * a meta-optimizer with the tree encoding. The problem must be a {@link
 * TreeMetaOptimizationProblem}: its {@link TreeMetaOptimizationProblem#solutionGenerator()} is
 * handed to the {@code mutation} parameter, which needs it to regenerate subtrees.
 *
 * <p>The parameter space is expected to be built with a {@code TreeParameterFactory} (see {@code
 * SPEA2MetaTree.yaml}).
 */
public class TreeSPEA2 extends BaseRDEMOEA<DerivationTreeSolution> {

  public TreeSPEA2(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  public TreeSPEA2(
      Problem<DerivationTreeSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public synchronized BaseLevelAlgorithm<DerivationTreeSolution> createInstance(
      Problem<DerivationTreeSolution> problem, int maximumNumberOfEvaluations) {
    return new TreeSPEA2(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  @Override
  protected void setNonConfigurableParameters() {
    Check.that(
        problem instanceof TreeMetaOptimizationProblem<?>,
        "TreeSPEA2 requires a TreeMetaOptimizationProblem");
    var mutationParameter =
        (MutationParameter<DerivationTreeSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "treeSolutionGenerator",
        ((TreeMetaOptimizationProblem<?>) problem).solutionGenerator());
  }
}
