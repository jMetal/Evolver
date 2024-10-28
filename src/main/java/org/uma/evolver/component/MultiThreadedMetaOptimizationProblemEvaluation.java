package org.uma.evolver.component;

import java.util.List;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Class that evaluates a list of solutions using threads.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 *
 * @param <S>
 */
public class MultiThreadedMetaOptimizationProblemEvaluation implements Evaluation<DoubleSolution> {
  private int computedEvaluations;
  private final DoubleProblem problem;
  private final int numberOfThreads ;

  public MultiThreadedMetaOptimizationProblemEvaluation(int numberOfThreads, MetaOptimizationProblem metaOptimizationProblem) {
    Check.that(numberOfThreads >= 0, "The number of threads is a negative value: " + numberOfThreads) ;
    Check.notNull(metaOptimizationProblem);

    if (numberOfThreads == 0) {
      numberOfThreads = Runtime.getRuntime().availableProcessors();
    }
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
        "" + numberOfThreads);

    this.numberOfThreads = numberOfThreads ;
    this.problem = metaOptimizationProblem.getNewInstance() ;
    computedEvaluations = 0;
  }

  @Override
  public List<DoubleSolution> evaluate(List<DoubleSolution> solutionList) {
    Check.notNull(solutionList);
    solutionList.parallelStream().forEach(problem::evaluate);
    computedEvaluations = solutionList.size();

    return solutionList;
  }

  @Override
  public int computedEvaluations() {
    return computedEvaluations;
  }

  public int numberOfThreads() {
    return numberOfThreads ;
  }

  @Override
  public DoubleProblem problem() {
    return problem ;
  }
}
