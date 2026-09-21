package org.uma.evolver.cli.training;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP1;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP2;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP3;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP4;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP5;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP6;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP7;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP8;
import org.uma.jmetal.problem.multiobjective.lsmop.LSMOP9;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F1;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F3;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F4;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F5;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F6;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F7;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F8;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F9;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.problem.multiobjective.re.RE22;
import org.uma.jmetal.problem.multiobjective.re.RE23;
import org.uma.jmetal.problem.multiobjective.re.RE24;
import org.uma.jmetal.problem.multiobjective.re.RE25;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.problem.multiobjective.re.RE32;
import org.uma.jmetal.problem.multiobjective.re.RE33;
import org.uma.jmetal.problem.multiobjective.re.RE34;
import org.uma.jmetal.problem.multiobjective.re.RE35;
import org.uma.jmetal.problem.multiobjective.re.RE36;
import org.uma.jmetal.problem.multiobjective.re.RE37;
import org.uma.jmetal.problem.multiobjective.re.RE41;
import org.uma.jmetal.problem.multiobjective.re.RE42;
import org.uma.jmetal.problem.multiobjective.re.RE61;
import org.uma.jmetal.problem.multiobjective.re.RE91;
import org.uma.jmetal.problem.multiobjective.rwa.RWA1;
import org.uma.jmetal.problem.multiobjective.rwa.RWA10;
import org.uma.jmetal.problem.multiobjective.rwa.RWA2;
import org.uma.jmetal.problem.multiobjective.rwa.RWA3;
import org.uma.jmetal.problem.multiobjective.rwa.RWA4;
import org.uma.jmetal.problem.multiobjective.rwa.RWA5;
import org.uma.jmetal.problem.multiobjective.rwa.RWA6;
import org.uma.jmetal.problem.multiobjective.rwa.RWA7;
import org.uma.jmetal.problem.multiobjective.rwa.RWA8;
import org.uma.jmetal.problem.multiobjective.rwa.RWA9;
import org.uma.jmetal.problem.multiobjective.uf.UF1;
import org.uma.jmetal.problem.multiobjective.uf.UF10;
import org.uma.jmetal.problem.multiobjective.uf.UF2;
import org.uma.jmetal.problem.multiobjective.uf.UF3;
import org.uma.jmetal.problem.multiobjective.uf.UF4;
import org.uma.jmetal.problem.multiobjective.uf.UF5;
import org.uma.jmetal.problem.multiobjective.uf.UF6;
import org.uma.jmetal.problem.multiobjective.uf.UF7;
import org.uma.jmetal.problem.multiobjective.uf.UF8;
import org.uma.jmetal.problem.multiobjective.uf.UF9;
import org.uma.jmetal.problem.multiobjective.wfg.WFG1;
import org.uma.jmetal.problem.multiobjective.wfg.WFG2;
import org.uma.jmetal.problem.multiobjective.wfg.WFG3;
import org.uma.jmetal.problem.multiobjective.wfg.WFG4;
import org.uma.jmetal.problem.multiobjective.wfg.WFG5;
import org.uma.jmetal.problem.multiobjective.wfg.WFG6;
import org.uma.jmetal.problem.multiobjective.wfg.WFG7;
import org.uma.jmetal.problem.multiobjective.wfg.WFG8;
import org.uma.jmetal.problem.multiobjective.wfg.WFG9;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT1;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT10;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT11;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT12;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT13;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT14;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT15;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT16;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT17;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT18;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT19;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT2;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT20;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT3;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT4;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT5;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT6;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT7;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT8;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT9;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Resolves a {@link ProblemSpec} (used in a {@link BaseLevelConfig}) to a jMetal problem
 * instance.
 *
 * <p>{@link #CURATED} is a discoverable catalogue of short names (surfaced by {@code
 * DescribeMain} so an external tool can list them) covering the full standard benchmark families:
 * ZDT (all but ZDT5, whose binary encoding does not fit {@code Problem<DoubleSolution>}), DTLZ1-7,
 * WFG1-9, RE21-25/31-37/41/42/61/91, RWA1-10, LZ09F1-9, LSMOP1-9, ZCAT1-20, UF1-10 — it is not the
 * only way to name a problem. A {@link ProblemSpec} whose {@code className} is not in {@link
 * #CURATED} is resolved by reflection instead, so <em>any</em> {@code Problem} on the runtime
 * classpath — every other jMetal problem, or a user's own — works by giving its fully-qualified
 * class name, with no change to this class ever required for that. {@link #resolve} matches
 * {@link ProblemSpec#args()} against a public constructor by arity for both cases alike (curated
 * or reflective), so a parametrized problem (e.g. DTLZ with an explicit number of objectives, or a
 * TSP instance file) is just a matter of supplying the right constructor arguments — no
 * per-parameterization subclass needed.
 */
final class ProblemRegistry {

  private static final Map<String, Class<?>> CURATED =
      Map.ofEntries(
          // ZDT (ZDT5 excluded: binary encoding, not a Problem<DoubleSolution>)
          Map.entry("ZDT1", ZDT1.class),
          Map.entry("ZDT2", ZDT2.class),
          Map.entry("ZDT3", ZDT3.class),
          Map.entry("ZDT4", ZDT4.class),
          Map.entry("ZDT6", ZDT6.class),
          // DTLZ1-7
          Map.entry("DTLZ1", DTLZ1.class),
          Map.entry("DTLZ2", DTLZ2.class),
          Map.entry("DTLZ3", DTLZ3.class),
          Map.entry("DTLZ4", DTLZ4.class),
          Map.entry("DTLZ5", DTLZ5.class),
          Map.entry("DTLZ6", DTLZ6.class),
          Map.entry("DTLZ7", DTLZ7.class),
          // WFG1-9
          Map.entry("WFG1", WFG1.class),
          Map.entry("WFG2", WFG2.class),
          Map.entry("WFG3", WFG3.class),
          Map.entry("WFG4", WFG4.class),
          Map.entry("WFG5", WFG5.class),
          Map.entry("WFG6", WFG6.class),
          Map.entry("WFG7", WFG7.class),
          Map.entry("WFG8", WFG8.class),
          Map.entry("WFG9", WFG9.class),
          // RE (real-world engineering design problems)
          Map.entry("RE21", RE21.class),
          Map.entry("RE22", RE22.class),
          Map.entry("RE23", RE23.class),
          Map.entry("RE24", RE24.class),
          Map.entry("RE25", RE25.class),
          Map.entry("RE31", RE31.class),
          Map.entry("RE32", RE32.class),
          Map.entry("RE33", RE33.class),
          Map.entry("RE34", RE34.class),
          Map.entry("RE35", RE35.class),
          Map.entry("RE36", RE36.class),
          Map.entry("RE37", RE37.class),
          Map.entry("RE41", RE41.class),
          Map.entry("RE42", RE42.class),
          Map.entry("RE61", RE61.class),
          Map.entry("RE91", RE91.class),
          // RWA (real-world application problems)
          Map.entry("RWA1", RWA1.class),
          Map.entry("RWA2", RWA2.class),
          Map.entry("RWA3", RWA3.class),
          Map.entry("RWA4", RWA4.class),
          Map.entry("RWA5", RWA5.class),
          Map.entry("RWA6", RWA6.class),
          Map.entry("RWA7", RWA7.class),
          Map.entry("RWA8", RWA8.class),
          Map.entry("RWA9", RWA9.class),
          Map.entry("RWA10", RWA10.class),
          // LZ09
          Map.entry("LZ09F1", LZ09F1.class),
          Map.entry("LZ09F2", LZ09F2.class),
          Map.entry("LZ09F3", LZ09F3.class),
          Map.entry("LZ09F4", LZ09F4.class),
          Map.entry("LZ09F5", LZ09F5.class),
          Map.entry("LZ09F6", LZ09F6.class),
          Map.entry("LZ09F7", LZ09F7.class),
          Map.entry("LZ09F8", LZ09F8.class),
          Map.entry("LZ09F9", LZ09F9.class),
          // LSMOP1-9
          Map.entry("LSMOP1", LSMOP1.class),
          Map.entry("LSMOP2", LSMOP2.class),
          Map.entry("LSMOP3", LSMOP3.class),
          Map.entry("LSMOP4", LSMOP4.class),
          Map.entry("LSMOP5", LSMOP5.class),
          Map.entry("LSMOP6", LSMOP6.class),
          Map.entry("LSMOP7", LSMOP7.class),
          Map.entry("LSMOP8", LSMOP8.class),
          Map.entry("LSMOP9", LSMOP9.class),
          // ZCAT1-20
          Map.entry("ZCAT1", ZCAT1.class),
          Map.entry("ZCAT2", ZCAT2.class),
          Map.entry("ZCAT3", ZCAT3.class),
          Map.entry("ZCAT4", ZCAT4.class),
          Map.entry("ZCAT5", ZCAT5.class),
          Map.entry("ZCAT6", ZCAT6.class),
          Map.entry("ZCAT7", ZCAT7.class),
          Map.entry("ZCAT8", ZCAT8.class),
          Map.entry("ZCAT9", ZCAT9.class),
          Map.entry("ZCAT10", ZCAT10.class),
          Map.entry("ZCAT11", ZCAT11.class),
          Map.entry("ZCAT12", ZCAT12.class),
          Map.entry("ZCAT13", ZCAT13.class),
          Map.entry("ZCAT14", ZCAT14.class),
          Map.entry("ZCAT15", ZCAT15.class),
          Map.entry("ZCAT16", ZCAT16.class),
          Map.entry("ZCAT17", ZCAT17.class),
          Map.entry("ZCAT18", ZCAT18.class),
          Map.entry("ZCAT19", ZCAT19.class),
          Map.entry("ZCAT20", ZCAT20.class),
          // UF1-10
          Map.entry("UF1", UF1.class),
          Map.entry("UF2", UF2.class),
          Map.entry("UF3", UF3.class),
          Map.entry("UF4", UF4.class),
          Map.entry("UF5", UF5.class),
          Map.entry("UF6", UF6.class),
          Map.entry("UF7", UF7.class),
          Map.entry("UF8", UF8.class),
          Map.entry("UF9", UF9.class),
          Map.entry("UF10", UF10.class));

  private ProblemRegistry() {}

  /** Names registered in {@link #CURATED}, for {@code DescribeMain}. */
  static Set<String> registeredNames() {
    return CURATED.keySet();
  }

  @SuppressWarnings("unchecked")
  static Problem<DoubleSolution> resolve(ProblemSpec spec) {
    Class<?> problemClass = resolveClass(spec.className());
    return (Problem<DoubleSolution>) instantiate(problemClass, spec.args(), spec.className());
  }

  private static Class<?> resolveClass(String className) {
    Class<?> curated = CURATED.get(className);
    if (curated != null) {
      return curated;
    }
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new JMetalException(
          "Unknown training problem: "
              + className
              + ". Not one of the curated names ("
              + CURATED.keySet()
              + ") and not a resolvable fully-qualified class name.");
    }
  }

  private static Object instantiate(Class<?> problemClass, List<Object> args, String label) {
    for (Constructor<?> constructor : problemClass.getConstructors()) {
      if (constructor.getParameterCount() != args.size()) {
        continue;
      }
      try {
        return constructor.newInstance(coerceArgs(constructor.getParameterTypes(), args));
      } catch (IllegalArgumentException e) {
        // Arity matched but types didn't (e.g. an overload for a different arg combination) --
        // keep trying other constructors with the same arity before giving up.
      } catch (ReflectiveOperationException e) {
        throw new JMetalException(
            "Error instantiating training problem " + label + ": " + e.getMessage());
      }
    }
    throw new JMetalException(
        "No public constructor of "
            + label
            + " accepts "
            + args.size()
            + " argument(s): "
            + args);
  }

  private static Object[] coerceArgs(Class<?>[] parameterTypes, List<Object> args) {
    Object[] coerced = new Object[args.size()];
    for (int i = 0; i < args.size(); i++) {
      coerced[i] = coerce(parameterTypes[i], args.get(i));
    }
    return coerced;
  }

  private static Object coerce(Class<?> targetType, Object value) {
    if (targetType.isInstance(value)) {
      return value;
    }
    if (value instanceof Number number) {
      if (targetType == int.class || targetType == Integer.class) {
        return number.intValue();
      }
      if (targetType == long.class || targetType == Long.class) {
        return number.longValue();
      }
      if (targetType == double.class || targetType == Double.class) {
        return number.doubleValue();
      }
    }
    if (value instanceof Boolean && (targetType == boolean.class || targetType == Boolean.class)) {
      return value;
    }
    throw new IllegalArgumentException(
        "Cannot pass " + value + " (" + value.getClass().getSimpleName() + ") as " + targetType);
  }
}
