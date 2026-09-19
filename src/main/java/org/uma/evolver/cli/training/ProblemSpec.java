package org.uma.evolver.cli.training;

import java.util.Arrays;
import java.util.List;

/**
 * Identifies a training problem: a class (a short, curated name registered in {@link
 * ProblemRegistry}, or a fully-qualified class name resolved by reflection) plus the constructor
 * arguments it needs, if any.
 *
 * <p>Most jMetal problems ({@code ZDT1}, {@code DTLZ3}, {@code RE31}, ...) have a no-arg
 * constructor, so the common case is just a name with empty {@code args}. Parametrized problems
 * (DTLZ/WFG/ZCAT with an explicit number of variables/objectives) or ones that need external data
 * (a TSP instance file) pass whatever their constructor expects as {@code args}; {@link
 * ProblemRegistry#resolve(ProblemSpec)} matches them against a constructor by arity. A user's own
 * {@code Problem} class works the same way — a fully-qualified class name resolved by reflection
 * — as long as it is on the runtime classpath; no Evolver code change is needed to use it.
 *
 * @param className a name registered in {@link ProblemRegistry}'s curated catalogue (e.g. {@code
 *     "ZDT1"}), or a fully-qualified class name (e.g. {@code
 *     "org.uma.jmetal.problem.multiobjective.wfg.WFG1"})
 * @param args constructor arguments, in order; empty for a no-arg constructor
 */
public record ProblemSpec(String className, List<Object> args) {

  public ProblemSpec {
    args = List.copyOf(args);
  }

  public ProblemSpec(String className) {
    this(className, List.of());
  }

  /** Convenience factory for the common no-arg case: {@code ProblemSpec.of("ZDT1", "DTLZ3")}. */
  public static List<ProblemSpec> of(String... classNames) {
    return Arrays.stream(classNames).map(ProblemSpec::new).toList();
  }

  /** Short label for display (e.g. training-set naming) — the class's simple name. */
  String displayName() {
    int lastDot = className.lastIndexOf('.');
    return lastDot < 0 ? className : className.substring(lastDot + 1);
  }
}
