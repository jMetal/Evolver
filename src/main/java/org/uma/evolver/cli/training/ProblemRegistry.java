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
 * Resolves a {@link ProblemSpec} (used in a {@link BaseLevelConfig}) to a jMetal problem
 * instance.
 *
 * <p>{@link #CURATED} is a small, discoverable catalogue of short names (surfaced by {@code
 * DescribeMain} so an external tool can list them) for the problems exercised by the reference
 * examples — it is not the only way to name a problem. A {@link ProblemSpec} whose {@code
 * className} is not in {@link #CURATED} is resolved by reflection instead, so <em>any</em> {@code
 * Problem} on the runtime classpath — every other jMetal problem, or a user's own — works by
 * giving its fully-qualified class name, with no change to this class ever required for that.
 * {@link #resolve} matches {@link ProblemSpec#args()} against a public constructor by arity for
 * both cases alike (curated or reflective), so a parametrized problem (e.g. DTLZ with an explicit
 * number of objectives, or a TSP instance file) is just a matter of supplying the right
 * constructor arguments — no per-parameterization subclass needed.
 */
final class ProblemRegistry {

  private static final Map<String, Class<?>> CURATED =
      Map.ofEntries(
          Map.entry("ZDT1", ZDT1.class),
          Map.entry("ZDT4", ZDT4.class),
          Map.entry("DTLZ1", DTLZ1.class),
          Map.entry("DTLZ2", DTLZ2.class),
          Map.entry("DTLZ3", DTLZ3.class),
          Map.entry("DTLZ4", DTLZ4.class),
          Map.entry("DTLZ5", DTLZ5.class),
          Map.entry("DTLZ6", DTLZ6.class),
          Map.entry("DTLZ7", DTLZ7.class),
          Map.entry("RE31", RE31.class),
          Map.entry("RE32", RE32.class),
          Map.entry("RE33", RE33.class),
          Map.entry("RE34", RE34.class),
          Map.entry("RE35", RE35.class),
          Map.entry("RE36", RE36.class),
          Map.entry("RE37", RE37.class));

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
