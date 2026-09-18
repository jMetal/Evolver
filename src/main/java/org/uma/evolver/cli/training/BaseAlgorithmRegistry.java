package org.uma.evolver.cli.training;

import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the base-level algorithm to be tuned, from a name and its parameter space.
 *
 * <p>Base-level algorithms are not built uniformly: {@code DoubleMOEAD} needs an extra
 * constructor argument ({@code weightVectorFilesDirectory}) that {@code DoubleNSGAII} does not.
 * {@link TrainingRequest#baseLevelExtraConfig()} carries that kind of algorithm-specific extra
 * configuration as a small string map instead of growing new dedicated request fields per
 * algorithm.
 *
 * <p>Registered algorithms live as data in {@link #ALGORITHMS}, not only as {@code switch} cases,
 * so that {@link DescribeMain} can list them (name, encoding, required extra-config keys) without
 * a second, hand-maintained copy of the same information — see
 * {@code docs/proposals/cli-describe-manifest.md}.
 *
 * <p>Prototype scope: only the two algorithms needed to reproduce the reference examples
 * ({@code NSGAIIOptimizingNSGAIIForProblemZDT4}, {@code NSGAIIOptimizingNSGAIIForBenchmarkRE3D}
 * and {@code NSGAIIOptimizingMOEADForProblemZDT4}) are registered.
 */
final class BaseAlgorithmRegistry {

  /**
   * @param name the base-level algorithm name, resolved via {@link #resolve}
   * @param encoding the jMetal solution encoding it is built for (only {@code "Double"} so far)
   * @param requiredExtraConfigKeys keys {@link #resolve} requires present in {@code extraConfig}
   */
  record BaseAlgorithmDescriptor(
      String name, String encoding, List<String> requiredExtraConfigKeys) {}

  private static final List<BaseAlgorithmDescriptor> ALGORITHMS =
      List.of(
          new BaseAlgorithmDescriptor("NSGA-II", "Double", List.of()),
          new BaseAlgorithmDescriptor(
              "MOEAD", "Double", List.of("weightVectorFilesDirectory")));

  private BaseAlgorithmRegistry() {}

  /** Registered algorithms, for {@link DescribeMain}. */
  static List<BaseAlgorithmDescriptor> registeredAlgorithms() {
    return ALGORITHMS;
  }

  static BaseLevelAlgorithm<DoubleSolution> resolve(
      String algorithmName,
      int populationSize,
      ParameterSpace parameterSpace,
      Map<String, String> extraConfig) {
    return switch (algorithmName) {
      case "NSGA-II" -> new DoubleNSGAII(populationSize, parameterSpace);
      case "MOEAD" -> new DoubleMOEAD(
          populationSize, requireExtra(extraConfig, "weightVectorFilesDirectory"), parameterSpace);
      default -> throw new JMetalException(
          "Unknown base-level algorithm: " + algorithmName + ". Supported: NSGA-II, MOEAD");
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
