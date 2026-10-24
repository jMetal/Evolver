package org.uma.evolver.cli;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.binaryproblem.BinaryProblem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.Binh2;
import org.uma.jmetal.problem.multiobjective.CarSideImpact;
import org.uma.jmetal.problem.multiobjective.ConstrEx;
import org.uma.jmetal.problem.multiobjective.ElectricMotor;
import org.uma.jmetal.problem.multiobjective.GAA;
import org.uma.jmetal.problem.multiobjective.Golinski;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.problem.multiobjective.Osyczka2;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.problem.multiobjective.Tanaka;
import org.uma.jmetal.problem.multiobjective.Viennet4;
import org.uma.jmetal.problem.multiobjective.Water;
import org.uma.jmetal.problem.multiobjective.cdtlz.C1_DTLZ1;
import org.uma.jmetal.problem.multiobjective.cdtlz.C1_DTLZ3;
import org.uma.jmetal.problem.multiobjective.cdtlz.C2_DTLZ2;
import org.uma.jmetal.problem.multiobjective.cdtlz.C3_DTLZ1;
import org.uma.jmetal.problem.multiobjective.cdtlz.C3_DTLZ4;
import org.uma.jmetal.problem.multiobjective.cdtlz.ConvexC2_DTLZ2;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM01;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM02;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM03;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM04;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM05;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM06;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM07;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM08;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM09;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM10;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM11;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM12;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM13;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM14;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM15;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM16;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM17;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM18;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM19;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM20;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM21;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM22;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM23;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM24;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM25;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM26;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM27;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM28;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM29;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM30;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM31;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM32;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM33;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM34;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM35;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM36;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM37;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM38;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM39;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM40;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM41;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM42;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM43;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM44;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM45;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM46;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM47;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM48;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM49;
import org.uma.jmetal.problem.multiobjective.cec2021.RCM50;
import org.uma.jmetal.problem.multiobjective.cf.CF10;
import org.uma.jmetal.problem.multiobjective.cf.CF11;
import org.uma.jmetal.problem.multiobjective.cf.CF12;
import org.uma.jmetal.problem.multiobjective.cf.CF13;
import org.uma.jmetal.problem.multiobjective.cf.CF14;
import org.uma.jmetal.problem.multiobjective.cf.CF15;
import org.uma.jmetal.problem.multiobjective.cf.CF16;
import org.uma.jmetal.problem.multiobjective.cf.CF1;
import org.uma.jmetal.problem.multiobjective.cf.CF2;
import org.uma.jmetal.problem.multiobjective.cf.CF3;
import org.uma.jmetal.problem.multiobjective.cf.CF4;
import org.uma.jmetal.problem.multiobjective.cf.CF5;
import org.uma.jmetal.problem.multiobjective.cf.CF6;
import org.uma.jmetal.problem.multiobjective.cf.CF7;
import org.uma.jmetal.problem.multiobjective.cf.CF8;
import org.uma.jmetal.problem.multiobjective.cf.CF9;
import org.uma.jmetal.problem.multiobjective.cre.CRE21;
import org.uma.jmetal.problem.multiobjective.cre.CRE22;
import org.uma.jmetal.problem.multiobjective.cre.CRE23;
import org.uma.jmetal.problem.multiobjective.cre.CRE24;
import org.uma.jmetal.problem.multiobjective.cre.CRE25;
import org.uma.jmetal.problem.multiobjective.cre.CRE31;
import org.uma.jmetal.problem.multiobjective.cre.CRE32;
import org.uma.jmetal.problem.multiobjective.cre.CRE51;
import org.uma.jmetal.problem.multiobjective.dascmop.AbstractDASCMOP;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP1;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP2;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP3;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP4;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP5;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP6;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP7;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP8;
import org.uma.jmetal.problem.multiobjective.dascmop.DASCMOP9;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP10;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP11;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP12;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP13;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP14;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP1;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP2;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP3;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP4;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP5;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP6;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP7;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP8;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP9;
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
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroBC100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroBD100TSP;
import org.uma.jmetal.problem.multiobjective.mw.MW10;
import org.uma.jmetal.problem.multiobjective.mw.MW11;
import org.uma.jmetal.problem.multiobjective.mw.MW12;
import org.uma.jmetal.problem.multiobjective.mw.MW13;
import org.uma.jmetal.problem.multiobjective.mw.MW14;
import org.uma.jmetal.problem.multiobjective.mw.MW1;
import org.uma.jmetal.problem.multiobjective.mw.MW2;
import org.uma.jmetal.problem.multiobjective.mw.MW3;
import org.uma.jmetal.problem.multiobjective.mw.MW4;
import org.uma.jmetal.problem.multiobjective.mw.MW5;
import org.uma.jmetal.problem.multiobjective.mw.MW6;
import org.uma.jmetal.problem.multiobjective.mw.MW7;
import org.uma.jmetal.problem.multiobjective.mw.MW8;
import org.uma.jmetal.problem.multiobjective.mw.MW9;
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
import org.uma.jmetal.problem.multiobjective.rwa.RWA10;
import org.uma.jmetal.problem.multiobjective.rwa.RWA1;
import org.uma.jmetal.problem.multiobjective.rwa.RWA2;
import org.uma.jmetal.problem.multiobjective.rwa.RWA3;
import org.uma.jmetal.problem.multiobjective.rwa.RWA4;
import org.uma.jmetal.problem.multiobjective.rwa.RWA5;
import org.uma.jmetal.problem.multiobjective.rwa.RWA6;
import org.uma.jmetal.problem.multiobjective.rwa.RWA7;
import org.uma.jmetal.problem.multiobjective.rwa.RWA8;
import org.uma.jmetal.problem.multiobjective.rwa.RWA9;
import org.uma.jmetal.problem.multiobjective.uf.UF10;
import org.uma.jmetal.problem.multiobjective.uf.UF1;
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
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT1;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT20;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT2;
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
 * UF1-10 (all continuous); the problems with constraints of jMetal (see its page "Constrained
 * benchmarks and reference fronts"): LIR-CMOP1-14, CF1-16, C-DTLZ, DAS-CMOP1-9, MW1-14, the
 * real-world suites CRE21-51 and RCM01-50 (CEC 2021), eight classical problems (family {@code
 * Classical}) and three engineering problems (family {@code Engineering}), all with the reference
 * fronts of jMetal 7.9 in {@code resources/referenceFronts} except Viennet4 and RCM17; and the
 * problems of the other encodings that need no data of their own:
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
          // LIR-CMOP1-14 (Fan et al., Soft Computing 2019; constrained, 2 or 3 objectives)
          Map.entry("LIRCMOP1", LIRCMOP1.class),
          Map.entry("LIRCMOP2", LIRCMOP2.class),
          Map.entry("LIRCMOP3", LIRCMOP3.class),
          Map.entry("LIRCMOP4", LIRCMOP4.class),
          Map.entry("LIRCMOP5", LIRCMOP5.class),
          Map.entry("LIRCMOP6", LIRCMOP6.class),
          Map.entry("LIRCMOP7", LIRCMOP7.class),
          Map.entry("LIRCMOP8", LIRCMOP8.class),
          Map.entry("LIRCMOP9", LIRCMOP9.class),
          Map.entry("LIRCMOP10", LIRCMOP10.class),
          Map.entry("LIRCMOP11", LIRCMOP11.class),
          Map.entry("LIRCMOP12", LIRCMOP12.class),
          Map.entry("LIRCMOP13", LIRCMOP13.class),
          Map.entry("LIRCMOP14", LIRCMOP14.class),
          // Classical constrained problems (family Classical; Viennet4 has no reference front)
          Map.entry("Binh2", Binh2.class),
          Map.entry("ConstrEx", ConstrEx.class),
          Map.entry("Golinski", Golinski.class),
          Map.entry("Osyczka2", Osyczka2.class),
          Map.entry("Srinivas", Srinivas.class),
          Map.entry("Tanaka", Tanaka.class),
          Map.entry("Viennet4", Viennet4.class),
          Map.entry("Water", Water.class),
          // Constrained engineering problems (family Engineering): CarSideImpact (3 objectives),
          // GAA (the general aviation aircraft, 10 objectives) and ElectricMotor (20 objectives)
          Map.entry("CarSideImpact", CarSideImpact.class),
          Map.entry("GAA", GAA.class),
          Map.entry("ElectricMotor", ElectricMotor.class),
          // CF1-16 (Xiang et al., IEEE TEVC 2021; constrained, 3 objectives by default)
          Map.entry("CF1", CF1.class),
          Map.entry("CF2", CF2.class),
          Map.entry("CF3", CF3.class),
          Map.entry("CF4", CF4.class),
          Map.entry("CF5", CF5.class),
          Map.entry("CF6", CF6.class),
          Map.entry("CF7", CF7.class),
          Map.entry("CF8", CF8.class),
          Map.entry("CF9", CF9.class),
          Map.entry("CF10", CF10.class),
          Map.entry("CF11", CF11.class),
          Map.entry("CF12", CF12.class),
          Map.entry("CF13", CF13.class),
          Map.entry("CF14", CF14.class),
          Map.entry("CF15", CF15.class),
          Map.entry("CF16", CF16.class),
          // C-DTLZ (Jain and Deb, IEEE TEVC 2014; constrained, 3 objectives by default)
          Map.entry("C1_DTLZ1", C1_DTLZ1.class),
          Map.entry("C1_DTLZ3", C1_DTLZ3.class),
          Map.entry("C2_DTLZ2", C2_DTLZ2.class),
          Map.entry("ConvexC2_DTLZ2", ConvexC2_DTLZ2.class),
          Map.entry("C3_DTLZ1", C3_DTLZ1.class),
          Map.entry("C3_DTLZ4", C3_DTLZ4.class),
          // DAS-CMOP1-9 (Fan et al., Evolutionary Computation 2020; constrained, 2 or 3
          // objectives; the difficulty triplet (eta, zeta, gamma) is an argument)
          Map.entry("DASCMOP1", DASCMOP1.class),
          Map.entry("DASCMOP2", DASCMOP2.class),
          Map.entry("DASCMOP3", DASCMOP3.class),
          Map.entry("DASCMOP4", DASCMOP4.class),
          Map.entry("DASCMOP5", DASCMOP5.class),
          Map.entry("DASCMOP6", DASCMOP6.class),
          Map.entry("DASCMOP7", DASCMOP7.class),
          Map.entry("DASCMOP8", DASCMOP8.class),
          Map.entry("DASCMOP9", DASCMOP9.class),
          // MW1-14 (Ma and Wang, IEEE TEVC 2019; constrained, 2 objectives, or 3 for the
          // scalable MW4, MW8 and MW14)
          Map.entry("MW1", MW1.class),
          Map.entry("MW2", MW2.class),
          Map.entry("MW3", MW3.class),
          Map.entry("MW4", MW4.class),
          Map.entry("MW5", MW5.class),
          Map.entry("MW6", MW6.class),
          Map.entry("MW7", MW7.class),
          Map.entry("MW8", MW8.class),
          Map.entry("MW9", MW9.class),
          Map.entry("MW10", MW10.class),
          Map.entry("MW11", MW11.class),
          Map.entry("MW12", MW12.class),
          Map.entry("MW13", MW13.class),
          Map.entry("MW14", MW14.class),
          // CRE (constrained real-world engineering problems; Tanabe and Ishibuchi, Applied Soft
          // Computing 2020; estimated fronts)
          Map.entry("CRE21", CRE21.class),
          Map.entry("CRE22", CRE22.class),
          Map.entry("CRE23", CRE23.class),
          Map.entry("CRE24", CRE24.class),
          Map.entry("CRE25", CRE25.class),
          Map.entry("CRE31", CRE31.class),
          Map.entry("CRE32", CRE32.class),
          Map.entry("CRE51", CRE51.class),
          // RCM01-50 (constrained real-world problems of the CEC 2021 competition; Kumar et al.,
          // Swarm and Evolutionary Computation 2021; estimated fronts, except RCM17, whose first
          // objective is unbounded; RCM09 has no constraints)
          Map.entry("RCM01", RCM01.class),
          Map.entry("RCM02", RCM02.class),
          Map.entry("RCM03", RCM03.class),
          Map.entry("RCM04", RCM04.class),
          Map.entry("RCM05", RCM05.class),
          Map.entry("RCM06", RCM06.class),
          Map.entry("RCM07", RCM07.class),
          Map.entry("RCM08", RCM08.class),
          Map.entry("RCM09", RCM09.class),
          Map.entry("RCM10", RCM10.class),
          Map.entry("RCM11", RCM11.class),
          Map.entry("RCM12", RCM12.class),
          Map.entry("RCM13", RCM13.class),
          Map.entry("RCM14", RCM14.class),
          Map.entry("RCM15", RCM15.class),
          Map.entry("RCM16", RCM16.class),
          Map.entry("RCM17", RCM17.class),
          Map.entry("RCM18", RCM18.class),
          Map.entry("RCM19", RCM19.class),
          Map.entry("RCM20", RCM20.class),
          Map.entry("RCM21", RCM21.class),
          Map.entry("RCM22", RCM22.class),
          Map.entry("RCM23", RCM23.class),
          Map.entry("RCM24", RCM24.class),
          Map.entry("RCM25", RCM25.class),
          Map.entry("RCM26", RCM26.class),
          Map.entry("RCM27", RCM27.class),
          Map.entry("RCM28", RCM28.class),
          Map.entry("RCM29", RCM29.class),
          Map.entry("RCM30", RCM30.class),
          Map.entry("RCM31", RCM31.class),
          Map.entry("RCM32", RCM32.class),
          Map.entry("RCM33", RCM33.class),
          Map.entry("RCM34", RCM34.class),
          Map.entry("RCM35", RCM35.class),
          Map.entry("RCM36", RCM36.class),
          Map.entry("RCM37", RCM37.class),
          Map.entry("RCM38", RCM38.class),
          Map.entry("RCM39", RCM39.class),
          Map.entry("RCM40", RCM40.class),
          Map.entry("RCM41", RCM41.class),
          Map.entry("RCM42", RCM42.class),
          Map.entry("RCM43", RCM43.class),
          Map.entry("RCM44", RCM44.class),
          Map.entry("RCM45", RCM45.class),
          Map.entry("RCM46", RCM46.class),
          Map.entry("RCM47", RCM47.class),
          Map.entry("RCM48", RCM48.class),
          Map.entry("RCM49", RCM49.class),
          Map.entry("RCM50", RCM50.class),
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
          // matrices from resources/tspInstances/, relative to the working directory
          Map.entry("EuclidAB300", EuclidAB300.class),
          Map.entry("KroAB100TSP", KroAB100TSP.class),
          Map.entry("KroABC100TSP", KroABC100TSP.class),
          Map.entry("KroABCD100TSP", KroABCD100TSP.class),
          Map.entry("KroABD100TSP", KroABD100TSP.class),
          Map.entry("KroAC100TSP", KroAC100TSP.class),
          Map.entry("KroACD100TSP", KroACD100TSP.class),
          Map.entry("KroACE100TSP", KroACE100TSP.class),
          Map.entry("KroAD100TSP", KroAD100TSP.class),
          Map.entry("KroAE100TSP", KroAE100TSP.class),
          Map.entry("KroBC100TSP", KroBC100TSP.class),
          Map.entry("KroBD100TSP", KroBD100TSP.class));

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
                  ArgumentSpec.integer(
                      "numberOfPositionParameters",
                      problem -> DefaultWFGSettings.numberOfPositionParameters),
                  ArgumentSpec.integer(
                      "numberOfDistanceParameters",
                      problem -> DefaultWFGSettings.numberOfDistanceParameters),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry("UF", List.of(ArgumentSpec.integer("numberOfVariables", VARIABLES))),
          Map.entry("MW", List.of(ArgumentSpec.integer("numberOfVariables", VARIABLES))),
          // MW4, MW8 and MW14 also take their number of objectives
          Map.entry(
              "MW4",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry(
              "MW8",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry(
              "MW14",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          // DAS-CMOP1-9 also take their difficulty triplet
          Map.entry(
              "DASCMOP",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.number("eta", problem -> ((AbstractDASCMOP) problem).eta()),
                  ArgumentSpec.number("zeta", problem -> ((AbstractDASCMOP) problem).zeta()),
                  ArgumentSpec.number(
                      "gamma", problem -> ((AbstractDASCMOP) problem).gamma()))),
          Map.entry(
              "CF",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          Map.entry(
              "C-DTLZ",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES))),
          // C3-DTLZ1 and C3-DTLZ4 also take their number of constraints, one per objective
          Map.entry(
              "C3_DTLZ1",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES),
                  ArgumentSpec.integer("numberOfConstraints", Problem::numberOfConstraints))),
          Map.entry(
              "C3_DTLZ4",
              List.of(
                  ArgumentSpec.integer("numberOfVariables", VARIABLES),
                  ArgumentSpec.integer("numberOfObjectives", OBJECTIVES),
                  ArgumentSpec.integer("numberOfConstraints", Problem::numberOfConstraints))),
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
        problem == null ? null : problem.numberOfConstraints(),
        arguments);
  }

  private static Problem<?> tryInstantiate(Class<?> problemClass, String name) {
    try {
      return (Problem<?>) instantiate(problemClass, List.of(), name);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** The classical problems with constraints, which share the family Classical. */
  private static final Set<String> CLASSICAL_CONSTRAINED =
      Set.of("Binh2", "ConstrEx", "Golinski", "Osyczka2", "Srinivas", "Tanaka", "Viennet4", "Water");

  /** The engineering problems with constraints, which share the family Engineering. */
  private static final Set<String> ENGINEERING_CONSTRAINED =
      Set.of("CarSideImpact", "GAA", "ElectricMotor");

  /**
   * The family of a problem: its name without the trailing number ({@code ZDT1}: {@code ZDT}),
   * except the TSP instances, the C-DTLZ problems and the classical and engineering problems with
   * constraints, grouped as in jMetal's page of constrained benchmarks.
   */
  private static String familyOf(String name, Class<?> problemClass) {
    if (problemClass.getPackageName().endsWith("multiobjectivetsp.instance")) {
      return "TSP";
    }
    if (problemClass.getPackageName().endsWith(".cdtlz")) {
      return "C-DTLZ";
    }
    if (CLASSICAL_CONSTRAINED.contains(name)) {
      return "Classical";
    }
    if (ENGINEERING_CONSTRAINED.contains(name)) {
      return "Engineering";
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
