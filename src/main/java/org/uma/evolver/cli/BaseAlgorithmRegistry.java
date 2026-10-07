package org.uma.evolver.cli;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.algorithm.moead.BinaryMOEAD;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.moead.PermutationMOEAD;
import org.uma.evolver.algorithm.nsgaii.BinaryNSGAII;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.nsgaii.PermutationNSGAII;
import org.uma.evolver.algorithm.nsgaiii.DoubleNSGAIII;
import org.uma.evolver.algorithm.paes.BinaryPAES;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.algorithm.paes.PermutationPAES;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.algorithm.rdemoea.PermutationRDEMOEA;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.algorithm.smsemoa.BinarySMSEMOA;
import org.uma.evolver.algorithm.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.algorithm.smsemoa.PermutationSMSEMOA;
import org.uma.evolver.algorithm.ssmoea.DoubleSSMOEA;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the base-level algorithm to be tuned, from a name, an encoding and its parameter space.
 *
 * <p>Base-level algorithms are not built uniformly: MOEA/D and RVEA need an extra constructor
 * argument ({@code weightVectorFilesDirectory}) that NSGA-II does not.
 * The {@code extraConfig} map of a training or solve request carries that kind of algorithm-specific extra
 * configuration as a small string map instead of growing new dedicated request fields per
 * algorithm.
 *
 * <p>Registered algorithms live in one table, {@link #ENTRIES}: each entry has its name, its
 * encoding, its required extra-config keys and how it is built, so that {@code DescribeMain} lists
 * the same algorithms that {@link #resolve} builds, with no second copy that could diverge.
 *
 * <p>{@link #resolve} returns {@code BaseLevelAlgorithm<?>}: the concrete solution type it is
 * built for depends on {@code encoding} and is only known at runtime, the same trust model
 * {@link ProblemRegistry} already uses for reflective problem resolution — a training set whose
 * problems do not actually match the declared encoding fails with a {@code ClassCastException} at
 * run time, not at compile time.
 */
public final class BaseAlgorithmRegistry {

  /**
   * @param name the base-level algorithm name, resolved via {@link #resolve}
   * @param encoding the jMetal solution encoding it is built for ({@code "Double"}, {@code
   *     "Binary"} or {@code "Permutation"})
   * @param requiredExtraConfigKeys keys {@link #resolve} requires present in {@code extraConfig}
   */
  public record BaseAlgorithmDescriptor(
      String name, String encoding, List<String> requiredExtraConfigKeys) {}

  /** How an algorithm is built from the population, its parameter space and its extra config. */
  @FunctionalInterface
  private interface Builder {
    BaseLevelAlgorithm<?> build(
        int populationSize, ParameterSpace parameterSpace, Map<String, String> extraConfig);
  }

  private record Entry(BaseAlgorithmDescriptor descriptor, Builder builder) {}

  private static Entry entry(
      String name, String encoding, List<String> requiredExtraConfigKeys, Builder builder) {
    return new Entry(new BaseAlgorithmDescriptor(name, encoding, requiredExtraConfigKeys), builder);
  }

  private static final List<String> WEIGHT_VECTORS = List.of("weightVectorFilesDirectory");

  /**
   * Every registered algorithm and encoding. {@code populationSize} is the population, except for
   * PAES, which has none: it is the number of solutions to find (the size of its archive).
   */
  private static final List<Entry> ENTRIES =
      List.of(
          entry("NSGA-II", "Double", List.of(), (n, s, e) -> new DoubleNSGAII(n, s)),
          entry("NSGA-II", "Binary", List.of(), (n, s, e) -> new BinaryNSGAII(n, s)),
          entry("NSGA-II", "Permutation", List.of(), (n, s, e) -> new PermutationNSGAII(n, s)),
          entry("NSGA-III", "Double", List.of(), (n, s, e) -> new DoubleNSGAIII(n, s)),
          entry(
              "MOEAD",
              "Double",
              WEIGHT_VECTORS,
              (n, s, e) -> new DoubleMOEAD(n, requireExtra(e, "weightVectorFilesDirectory"), s)),
          entry(
              "MOEAD",
              "Binary",
              WEIGHT_VECTORS,
              (n, s, e) -> new BinaryMOEAD(n, requireExtra(e, "weightVectorFilesDirectory"), s)),
          entry(
              "MOEAD",
              "Permutation",
              WEIGHT_VECTORS,
              (n, s, e) ->
                  new PermutationMOEAD(n, requireExtra(e, "weightVectorFilesDirectory"), s)),
          entry("SMS-EMOA", "Double", List.of(), (n, s, e) -> new DoubleSMSEMOA(n, s)),
          entry("SMS-EMOA", "Binary", List.of(), (n, s, e) -> new BinarySMSEMOA(n, s)),
          entry("SMS-EMOA", "Permutation", List.of(), (n, s, e) -> new PermutationSMSEMOA(n, s)),
          entry("RDEMOEA", "Double", List.of(), (n, s, e) -> new DoubleRDEMOEA(n, s)),
          entry("RDEMOEA", "Permutation", List.of(), (n, s, e) -> new PermutationRDEMOEA(n, s)),
          entry(
              "RVEA",
              "Double",
              WEIGHT_VECTORS,
              (n, s, e) -> new DoubleRVEA(n, requireExtra(e, "weightVectorFilesDirectory"), s)),
          entry("AGE-MOEA", "Double", List.of(), (n, s, e) -> new DoubleAGEMOEA(n, s)),
          entry("SSMOEA", "Double", List.of(), (n, s, e) -> new DoubleSSMOEA(n, s)),
          entry("PAES", "Double", List.of(), (n, s, e) -> new DoublePAES(n, s)),
          entry("PAES", "Binary", List.of(), (n, s, e) -> new BinaryPAES(n, s)),
          entry("PAES", "Permutation", List.of(), (n, s, e) -> new PermutationPAES(n, s)));

  private static final List<BaseAlgorithmDescriptor> ALGORITHMS =
      ENTRIES.stream().map(Entry::descriptor).toList();

  private static final List<String> ENCODINGS = List.of("Double", "Binary", "Permutation");
  private static final String SUPPORTED_ENCODINGS = String.join(", ", ENCODINGS);

  private BaseAlgorithmRegistry() {}

  /** Registered algorithms, for {@code DescribeMain}. */
  public static List<BaseAlgorithmDescriptor> registeredAlgorithms() {
    return ALGORITHMS;
  }

  /**
   * Builds the {@link ParameterSpace} for {@code algorithmName}/{@code encoding}, using the {@link
   * ParameterFactory} that matches the encoding — this must stay in lockstep with {@link
   * #resolve}, so both are driven by the same {@code encoding} value.
   */
  public static YAMLParameterSpace resolveParameterSpace(
      String encoding, String yamlParameterSpaceFile) {
    return new YAMLParameterSpace(yamlParameterSpaceFile, parameterFactory(encoding));
  }

  public static BaseLevelAlgorithm<?> resolve(
      String algorithmName,
      String encoding,
      int populationSize,
      ParameterSpace parameterSpace,
      Map<String, String> extraConfig) {
    if (!ENCODINGS.contains(encoding)) {
      throw new JMetalException(
          "Unknown base-level encoding: " + encoding + ". Supported: " + SUPPORTED_ENCODINGS);
    }
    return ENTRIES.stream()
        .filter(entry -> entry.descriptor().name().equals(algorithmName))
        .filter(entry -> entry.descriptor().encoding().equals(encoding))
        .findFirst()
        .orElseThrow(() -> unknownAlgorithm(algorithmName, encoding))
        .builder()
        .build(populationSize, parameterSpace, extraConfig);
  }

  private static JMetalException unknownAlgorithm(String algorithmName, String encoding) {
    String supported =
        ALGORITHMS.stream()
            .filter(algorithm -> algorithm.encoding().equals(encoding))
            .map(BaseAlgorithmDescriptor::name)
            .collect(Collectors.joining(", "));
    return new JMetalException(
        "Unknown base-level algorithm: "
            + algorithmName
            + " for encoding "
            + encoding
            + ". Supported: "
            + supported);
  }

  private static ParameterFactory<?> parameterFactory(String encoding) {
    return switch (encoding) {
      case "Double" -> new DoubleParameterFactory();
      case "Binary" -> new BinaryParameterFactory();
      case "Permutation" -> new PermutationParameterFactory();
      default ->
          throw new JMetalException(
              "Unknown base-level encoding: " + encoding + ". Supported: " + SUPPORTED_ENCODINGS);
    };
  }

  private static String requireExtra(Map<String, String> extraConfig, String key) {
    String value = extraConfig == null ? null : extraConfig.get(key);
    if (value == null) {
      throw new JMetalException("Missing required baseLevelExtraConfig entry: " + key);
    }
    return value;
  }
}
