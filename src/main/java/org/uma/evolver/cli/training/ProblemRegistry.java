package org.uma.evolver.cli.training;

import java.util.Map;
import java.util.function.Supplier;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.problem.multiobjective.re.RE32;
import org.uma.jmetal.problem.multiobjective.re.RE33;
import org.uma.jmetal.problem.multiobjective.re.RE34;
import org.uma.jmetal.problem.multiobjective.re.RE35;
import org.uma.jmetal.problem.multiobjective.re.RE36;
import org.uma.jmetal.problem.multiobjective.re.RE37;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Maps training-problem names used in a {@link BaseLevelConfig} to jMetal problem instances.
 *
 * <p>Prototype scope: only the problems needed to reproduce the reference examples are
 * registered (the RE31-RE37 problems let {@code Re3dTrainingRunner}/{@code TreeRe3dTrainingRunner}
 * list the {@code RE3DTrainingSet} problems explicitly instead of resolving them by training-set
 * name). Extend this map to support more training problems.
 */
final class ProblemRegistry {

  private static final Map<String, Supplier<Problem<DoubleSolution>>> PROBLEMS =
      Map.ofEntries(
          Map.entry("ZDT1", ZDT1::new),
          Map.entry("ZDT4", ZDT4::new),
          Map.entry("DTLZ1", DTLZ1::new),
          Map.entry("DTLZ2", DTLZ2::new),
          Map.entry("DTLZ3", DTLZ3::new),
          Map.entry("DTLZ4", DTLZ4::new),
          Map.entry("DTLZ5", DTLZ5::new),
          Map.entry("DTLZ6", DTLZ6::new),
          Map.entry("DTLZ7", DTLZ7::new),
          Map.entry("RE31", RE31::new),
          Map.entry("RE32", RE32::new),
          Map.entry("RE33", RE33::new),
          Map.entry("RE34", RE34::new),
          Map.entry("RE35", RE35::new),
          Map.entry("RE36", RE36::new),
          Map.entry("RE37", RE37::new));

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
