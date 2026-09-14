package org.uma.evolver.cli.runner;

import java.util.Map;
import java.util.function.Supplier;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Maps training-problem names used in a {@link TrainingRequest} to jMetal problem instances.
 *
 * <p>Prototype scope: only the problems needed to reproduce the reference example
 * ({@code NSGAIIOptimizingNSGAIIForProblemZDT4}) are registered. Extend this map to support more
 * training problems.
 */
final class ProblemRegistry {

  private static final Map<String, Supplier<Problem<DoubleSolution>>> PROBLEMS =
      Map.of(
          "ZDT1", ZDT1::new,
          "ZDT4", ZDT4::new);

  private ProblemRegistry() {}

  static Problem<DoubleSolution> resolve(String problemName) {
    Supplier<Problem<DoubleSolution>> supplier = PROBLEMS.get(problemName);
    if (supplier == null) {
      throw new JMetalException(
          "Unknown training problem: "
              + problemName
              + ". Supported problems: "
              + PROBLEMS.keySet());
    }
    return supplier.get();
  }
}
