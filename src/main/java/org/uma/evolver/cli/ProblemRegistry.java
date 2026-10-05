package org.uma.evolver.cli;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
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
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.EuclidAB300;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroABC100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroABCD100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroABD100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAC100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroACD100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroACE100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAD100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAE100TSP;
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
import org.uma.jmetal.problem.multiobjective.wfg.DefaultWFGSettings;
import org.uma.jmetal.problem.multiobjective.wfg.WFG1;
import org.uma.jmetal.problem.multiobjective.wfg.WFG2;
import org.uma.jmetal.problem.multiobjective.wfg.WFG3;
import org.uma.jmetal.problem.multiobjective.wfg.WFG4;
import org.uma.jmetal.problem.multiobjective.wfg.WFG5;
import org.uma.jmetal.problem.multiobjective.wfg.WFG6;
import org.uma.jmetal.problem.multiobjective.wfg.WFG7;
import org.uma.jmetal.problem.multiobjective.wfg.WFG8;
import org.uma.jmetal.problem.multiobjective.wfg.WFG9;
import org.uma.jmetal.problem.multiobjective.zcat.DefaultZCATSettings;
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
import org.uma.jmetal.problem.multiobjective.zdt.ZDT5;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Resolves a {@link ProblemSpec} (used in a training or solve request) to a jMetal problem
 * instance.
 *
 * <p>{@link #CURATED} is a discoverable catalogue of short names (surfaced by {@code
 * DescribeMain} so an external tool can list them) covering the full standard benchmark families:
 * ZDT1-6, DTLZ1-7, WFG1-9, RE21-25/31-37/41/42/61/91, RWA1-10, LZ09F1-9, LSMOP1-9, ZCAT1-20 and
 * UF1-10 (all continuous), and the problems of the other encodings that need no data of their own:
 * ZDT5 and OneZeroMax (binary) and the multi-objective TSP instances of jMetal (permutation) — it
 * is not the only way to name a problem. A {@link ProblemSpec} whose {@code className} is not in {@link
 * #CURATED} is resolved by reflection instead, so <em>any</em> {@code Problem} on the runtime
 * classpath — every other jMetal problem, or a user's own — works by giving its fully-qualified
 * class name, with no change to this class ever required for that. {@link #resolve} matches
 * {@link ProblemSpec#args()} against a public constructor by arity for both cases alike (curated
 * or reflective), so a parametrized problem (e.g. DTLZ with an explicit number of objectives, or a
 * TSP instance file) is just a matter of supplying the right constructor arguments — no
 * per-parameterization subclass needed.
 */
public final class ProblemRegistry {

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
          Map.entry("UF10", UF10.class),
          // Binary: ZDT5 is the deceptive problem of the ZDT family, OneZeroMax has all its
          // solutions Pareto optimal
          Map.entry("ZDT5", ZDT5.class),
          Map.entry("OneZeroMax", OneZeroMax.class),
          // Permutation: the multi-objective TSP instances of jMetal. They read their distance
          // matrices from resources/tspInstances/, relative to the working directory. KroBC100TSP
          // and KroBD100TSP are left out: jMetal 7.7 points them to kroAC100.tsp and kroAD100.tsp,
          // which do not exist (they should be kroC100.tsp and kroD100.tsp)
          Map.entry("EuclidAB300", EuclidAB300.class),
          Map.entry("KroAB100TSP", KroAB100TSP.class),
          Map.entry("KroABC100TSP", KroABC100TSP.class),
          Map.entry("KroABCD100TSP", KroABCD100TSP.class),
          Map.entry("KroABD100TSP", KroABD100TSP.class),
          Map.entry("KroAC100TSP", KroAC100TSP.class),
          Map.entry("KroACD100TSP", KroACD100TSP.class),
          Map.entry("KroACE100TSP", KroACE100TSP.class),
          Map.entry("KroAD100TSP", KroAD100TSP.class),
          Map.entry("KroAE100TSP", KroAE100TSP.class));

  /** An argument of a problem, with how to find its default in the problem built with none. */
  private record ArgumentSpec(String name, String type, Function<Problem<?>, Object> defaultValue) {

    static ArgumentSpec integer(String name, Function<Problem<?>, Object> defaultValue) {
      return new ArgumentSpec(name, "integer", defaultValue);
    }

    static ArgumentSpec number(String name, Function<Problem<?>, Object> defaultValue) {
      return new ArgumentSpec(name, "number", defaultValue);
    }

    static ArgumentSpec bool(String name, Function<Problem<?>, Object> defaultValue) {
      return new ArgumentSpec(name, "boolean", defaultValue);
    }
  }

  private static final Function<Problem<?>, Object> VARIABLES = Problem::numberOfVariables;
  private static final Function<Problem<?>, Object> OBJECTIVES = Problem::numberOfObjectives;
  private static final Function<Problem<?>, Object> UNKNOWN = problem -> null;

  /**
   * The arguments of the constructor with arguments of each family, in order, or of a problem that
   * differs from its family (UF5, UF6 and UF9): the names of the arguments are not available by
   * reflection (jMetal is not compiled with {@code -parameters}), and what each one means has to be
   * written down. A request gives all the arguments of a problem or none, since {@link
   * #instantiate} matches the constructor by arity. A test builds every problem with the defaults
   * of this table.
   */
  private static final Map<String, List<ArgumentSpec>> ARGUMENTS =
      Map.ofEntries(
          Map.entry("ZDT", List.of(ArgumentSpec.integer("numberOfVariables", VARIABLES))),
          Map.entry(
              "DTLZ",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry(
              "WFG",
              List.of(
                  ArgumentSpec.integer("k", problem -> DefaultWFGSettings.numberOfPositionParameters),
                  ArgumentSpec.integer("l", problem -> DefaultWFGSettings.numberOfDistanceParameters),
                  ArgumentSpec.integer("m", OBJECTIVES))),
          Map.entry("UF", List.of(ArgumentSpec.integer("numberOfVariables", VARIABLES))),
          // UF5 and UF6 also take the number of points of the front and a tolerance, UF9 a tolerance
          Map.entry(
              "UF5",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("N", problem -> 10),
                  ArgumentSpec.number("epsilon", problem -> 0.1))),
          Map.entry(
              "UF6",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("N", problem -> 2),
                  ArgumentSpec.number("epsilon", problem -> 0.1))),
          Map.entry(
              "UF9",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.number("epsilon", problem -> 0.1))),
          // The three types that select the shape of the Pareto set and front, which differ per
          // problem and are not exposed by jMetal
          Map.entry(
              "LZ09F",
              List.of(
                  ArgumentSpec.integer("ptype", UNKNOWN),
                  ArgumentSpec.integer("dtype", UNKNOWN),
                  ArgumentSpec.integer("ltype", UNKNOWN))),
          Map.entry(
              "LSMOP",
              List.of(
                  ArgumentSpec.integer("nk", problem -> 5),
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry(
              "ZCAT",
              List.of(
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES),
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.bool(
                      "complicatedParetoSet", problem -> DefaultZCATSettings.complicatedParetoSet),
                  ArgumentSpec.integer("level", problem -> DefaultZCATSettings.level),
                  ArgumentSpec.bool("bias", problem -> DefaultZCATSettings.bias),
                  ArgumentSpec.bool("imbalance", problem -> DefaultZCATSettings.imbalance))),
          Map.entry(
              "OneZeroMax",
              List.of(
                  ArgumentSpec.integer(
                      "numberOfBits", problem -> ((BinaryProblem) problem).totalNumberOfBits()))));

  private ProblemRegistry() {}

  /** Names registered in {@link #CURATED}, for {@code DescribeMain}. */
  public static Set<String> registeredNames() {
    return CURATED.keySet();
  }

  /**
   * Describes every registered problem, sorted by name.
   *
   * <p>Each problem is built with no arguments to read its dimensions: it takes milliseconds for
   * all of them. A problem that cannot be built (a TSP instance whose files are not in the working
   * directory) is described without dimensions.
   *
   * @return the descriptors, for {@code DescribeMain}
   */
  public static List<ProblemDescriptor> registeredProblems() {
    return CURATED.keySet().stream().sorted().map(ProblemRegistry::describe).toList();
  }

  private static ProblemDescriptor describe(String name) {
    Class<?> problemClass = CURATED.get(name);
    String family = familyOf(name, problemClass);
    Problem<?> problem = tryInstantiate(problemClass, name);
    List<ProblemDescriptor.Argument> arguments =
        ARGUMENTS.getOrDefault(name, ARGUMENTS.getOrDefault(family, List.of())).stream()
            .map(
                spec ->
                    new ProblemDescriptor.Argument(
                        spec.name(),
                        spec.type(),
                        problem == null ? null : spec.defaultValue().apply(problem)))
            .toList();
    return new ProblemDescriptor(
        name,
        family,
        encodingOf(problemClass),
        problem == null ? null : problem.numberOfObjectives(),
        problem == null ? null : problem.numberOfVariables(),
        arguments);
  }

  private static Problem<?> tryInstantiate(Class<?> problemClass, String name) {
    try {
      return (Problem<?>) instantiate(problemClass, List.of(), name);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** The family of a problem: its name without the trailing number ({@code ZDT1}: {@code ZDT}). */
  private static String familyOf(String name, Class<?> problemClass) {
    if (problemClass.getPackageName().endsWith("multiobjectivetsp.instance")) {
      return "TSP";
    }
    return name.replaceAll("\\d+$", "");
  }

  /**
   * The encoding of the solutions of a problem class.
   *
   * @return {@code "Double"}, {@code "Binary"} or {@code "Permutation"}, or {@code null} for a
   *     problem that is none of them
   */
  public static String encodingOf(Class<?> problemClass) {
    if (DoubleProblem.class.isAssignableFrom(problemClass)) {
      return "Double";
    }
    if (BinaryProblem.class.isAssignableFrom(problemClass)) {
      return "Binary";
    }
    if (PermutationProblem.class.isAssignableFrom(problemClass)) {
      return "Permutation";
    }
    return null;
  }

  /**
   * Checks that a problem has the encoding of the algorithm that will solve it.
   *
   * <p>Without it, the mismatch ends in a {@code ClassCastException} deep in the run, whose message
   * names jMetal classes instead of what to change.
   *
   * @param problem the problem
   * @param algorithmEncoding the encoding of the base-level algorithm ({@code "Double"}, {@code
   *     "Binary"} or {@code "Permutation"})
   * @throws JMetalException if the problem's encoding is a different one. A problem that is none of
   *     the three is not checked
   */
  public static void checkEncoding(Problem<?> problem, String algorithmEncoding) {
    String encoding = encodingOf(problem.getClass());
    if (encoding == null || encoding.equals(algorithmEncoding)) {
      return;
    }
    String example =
        CURATED.keySet().stream()
            .sorted()
            .filter(name -> algorithmEncoding.equals(encodingOf(CURATED.get(name))))
            .findFirst()
            .map(name -> " (for instance " + name + ")")
            .orElse("");
    throw new JMetalException(
        "Problem "
            + problem.name()
            + " is "
            + encoding
            + "-encoded, but the algorithm was configured with the "
            + algorithmEncoding
            + " encoding: use a problem of that encoding"
            + example
            + ", or the algorithm for "
            + encoding
            + " problems");
  }

  public static Problem<?> resolve(ProblemSpec spec) {
    Class<?> problemClass = resolveClass(spec.className());
    return (Problem<?>) instantiate(problemClass, spec.args(), spec.className());
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
