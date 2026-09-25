package org.uma.evolver.cli;

import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.nsgaii.PermutationNSGAII;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the base-level algorithm to be tuned, from a name, an encoding and its parameter space.
 *
 * <p>Base-level algorithms are not built uniformly: {@code DoubleMOEAD} needs an extra
 * constructor argument ({@code weightVectorFilesDirectory}) that {@code DoubleNSGAII} does not.
 * The {@code extraConfig} map of a training or solve request carries that kind of algorithm-specific extra
 * configuration as a small string map instead of growing new dedicated request fields per
 * algorithm.
 *
 * <p>Registered algorithms live as data in {@link #ALGORITHMS}, not only as {@code switch} cases,
 * so that {@code DescribeMain} can list them (name, encoding, required extra-config keys) without
 * a second, hand-maintained copy of the same information — see
 * {@code docs/proposals/cli-describe-manifest.md}.
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
   * @param encoding the jMetal solution encoding it is built for ({@code "Double"} or
   *     {@code "Permutation"})
   * @param requiredExtraConfigKeys keys {@link #resolve} requires present in {@code extraConfig}
   */
  public record BaseAlgorithmDescriptor(
      String name, String encoding, List<String> requiredExtraConfigKeys) {}

  private static final List<BaseAlgorithmDescriptor> ALGORITHMS =
      List.of(
          new BaseAlgorithmDescriptor("NSGA-II", "Double", List.of()),
          new BaseAlgorithmDescriptor("NSGA-II", "Permutation", List.of()),
          new BaseAlgorithmDescriptor(
              "MOEAD", "Double", List.of("weightVectorFilesDirectory")));

  private BaseAlgorithmRegistry() {}

  /** Registered algorithms, for {@code DescribeMain}. */
  public static List<BaseAlgorithmDescriptor> registeredAlgorithms() {
    return ALGORITHMS;
  }

  /**
   * Builds the {@link ParameterSpace} for {@code algorithmName}/{@code encoding}, using the
   * {@link ParameterFactory} that matches the encoding — this must stay in lockstep with
   * {@link #resolve}, so both are driven by the same {@code encoding} value.
   */
  public static YAMLParameterSpace resolveParameterSpace(String encoding, String yamlParameterSpaceFile) {
    return new YAMLParameterSpace(yamlParameterSpaceFile, parameterFactory(encoding));
  }

  public static BaseLevelAlgorithm<?> resolve(
      String algorithmName,
      String encoding,
      int populationSize,
      ParameterSpace parameterSpace,
      Map<String, String> extraConfig) {
    return switch (encoding) {
      case "Double" -> resolveDouble(algorithmName, populationSize, parameterSpace, extraConfig);
      case "Permutation" -> resolvePermutation(algorithmName, populationSize, parameterSpace);
      default -> throw new JMetalException(
          "Unknown base-level encoding: " + encoding + ". Supported: Double, Permutation");
    };
  }

  private static ParameterFactory<?> parameterFactory(String encoding) {
    return switch (encoding) {
      case "Double" -> new DoubleParameterFactory();
      case "Permutation" -> new PermutationParameterFactory();
      default -> throw new JMetalException(
          "Unknown base-level encoding: " + encoding + ". Supported: Double, Permutation");
    };
  }

  private static BaseLevelAlgorithm<?> resolveDouble(
      String algorithmName,
      int populationSize,
      ParameterSpace parameterSpace,
      Map<String, String> extraConfig) {
    return switch (algorithmName) {
      case "NSGA-II" -> new DoubleNSGAII(populationSize, parameterSpace);
      case "MOEAD" -> new DoubleMOEAD(
          populationSize, requireExtra(extraConfig, "weightVectorFilesDirectory"), parameterSpace);
      default -> throw new JMetalException(
          "Unknown base-level algorithm: "
              + algorithmName
              + " for encoding Double. Supported: NSGA-II, MOEAD");
    };
  }

  private static BaseLevelAlgorithm<?> resolvePermutation(
      String algorithmName, int populationSize, ParameterSpace parameterSpace) {
    return switch (algorithmName) {
      case "NSGA-II" -> new PermutationNSGAII(populationSize, parameterSpace);
      default -> throw new JMetalException(
          "Unknown base-level algorithm: "
              + algorithmName
              + " for encoding Permutation. Supported: NSGA-II");
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
